package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CostUsageTokenSnapshot
import com.steipete.codexbar.domain.model.NamedRateWindow
import com.steipete.codexbar.domain.model.ProviderCostSnapshot
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.RateWindow
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant

/**
 * Live usage fetcher for Cursor via cursor.com web APIs.
 */
class CursorUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.CURSOR

    companion object {
        const val BASE_URL = "https://cursor.com"
        const val USAGE_SUMMARY_URL = "$BASE_URL/api/usage-summary"
        const val AUTH_ME_URL = "$BASE_URL/api/auth/me"
        const val SAND_USAGE_URL = "$BASE_URL/api/dashboard/get-sand-usage-status"
    }

    @Serializable
    private data class UserInfoDto(
        val email: String? = null,
        val sub: String? = null,
        val membershipType: String? = null
    )

    @Serializable
    private data class FastRequestsDto(
        @SerialName("numRequests") val numRequests: Int? = null,
        @SerialName("maxRequestUsage") val maxRequestUsage: Int? = null
    )

    @Serializable
    private data class UsageSummaryDto(
        @SerialName("fastRequests") val fastRequests: FastRequestsDto? = null,
        @SerialName("numFastRequests") val numFastRequests: Int? = null,
        @SerialName("maxFastRequests") val maxFastRequests: Int? = null,
        @SerialName("numRequests") val numRequests: Int? = null,
        @SerialName("startOfMonth") val startOfMonth: String? = null,
        @SerialName("endOfMonth") val endOfMonth: String? = null
    )

    @Serializable
    private data class SandUsageStatusDto(
        val used: Double? = null,
        val limit: Double? = null,
        val percentUsed: Double? = null
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val cookieHeader = (credentials.sessionToken ?: credentials.apiKey)?.trim()
            if (cookieHeader.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("Cursor requires session cookies (WorkosCursorSessionToken). Please configure in Settings.")
                )
            }

            val now = System.currentTimeMillis()

            try {
                // 1. Fetch User Info (Auth Me)
                var userInfo: UserInfoDto? = null
                try {
                    val meRequest = Request.Builder()
                        .url(AUTH_ME_URL)
                        .header("Cookie", cookieHeader)
                        .header("Accept", "application/json")
                        .build()

                    httpClient.newCall(meRequest).execute().use { resp ->
                        if (resp.code == 401 || resp.code == 403) {
                            return@withContext Result.failure(
                                AuthenticationException("Cursor session expired or invalid (HTTP ${resp.code}). Please update cookies.")
                            )
                        }
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrEmpty()) {
                                userInfo = json.decodeFromString<UserInfoDto>(body)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is AuthenticationException) return@withContext Result.failure(e)
                }

                // 2. Fetch Usage Summary
                val summaryRequest = Request.Builder()
                    .url(USAGE_SUMMARY_URL)
                    .header("Cookie", cookieHeader)
                    .header("Accept", "application/json")
                    .build()

                val summaryDto: UsageSummaryDto
                httpClient.newCall(summaryRequest).execute().use { resp ->
                    if (resp.code == 401 || resp.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("Cursor session rejected (HTTP ${resp.code}).")
                        )
                    }
                    if (resp.code == 429) {
                        val retryAfter = parseRetryAfter(resp.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "Cursor rate limited. Retry after: $retryAfter")
                        )
                    }
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("Cursor usage-summary failed: HTTP ${resp.code}")
                        )
                    }
                    val body = resp.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("Empty body from Cursor usage summary"))
                    summaryDto = json.decodeFromString<UsageSummaryDto>(body)
                }

                // 3. Best-effort Sand Usage
                var sandUsage: SandUsageStatusDto? = null
                try {
                    val emptyJsonBody = "{}".toRequestBody("application/json".toMediaType())
                    val sandRequest = Request.Builder()
                        .url(SAND_USAGE_URL)
                        .header("Cookie", cookieHeader)
                        .header("Accept", "application/json")
                        .header("Origin", BASE_URL)
                        .post(emptyJsonBody)
                        .build()

                    httpClient.newCall(sandRequest).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrEmpty()) {
                                sandUsage = json.decodeFromString<SandUsageStatusDto>(body)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Sand usage is best effort
                }

                // Calculate fast requests rate window
                val usedRequests = summaryDto.fastRequests?.numRequests ?: summaryDto.numFastRequests ?: 0
                val maxRequests = summaryDto.fastRequests?.maxRequestUsage ?: summaryDto.maxFastRequests ?: 500
                val usedPercent = if (maxRequests > 0) {
                    ((usedRequests.toDouble() / maxRequests.toDouble()) * 100.0).coerceIn(0.0, 100.0)
                } else {
                    0.0
                }

                val cycleEndEpochMs = parseIsoDate(summaryDto.endOfMonth)

                val primaryWindow = RateWindow(
                    usedPercent = usedPercent,
                    windowMinutes = null,
                    resetsAtEpochMs = cycleEndEpochMs,
                    resetDescription = "$usedRequests / $maxRequests fast requests"
                )

                val extraWindows = mutableListOf<NamedRateWindow>()
                sandUsage?.let { sand ->
                    val sandPercent = sand.percentUsed ?: if (sand.limit != null && sand.limit > 0 && sand.used != null) {
                        ((sand.used / sand.limit) * 100.0).coerceIn(0.0, 100.0)
                    } else 0.0

                    extraWindows.add(
                        NamedRateWindow(
                            id = "sand_usage",
                            title = "Sand / Grok",
                            window = RateWindow(
                                usedPercent = sandPercent,
                                resetsAtEpochMs = cycleEndEpochMs
                            )
                        )
                    )
                }

                val snapshot = UsageSnapshot(
                    provider = UsageProvider.CURSOR,
                    primary = primaryWindow,
                    extraRateWindows = extraWindows,
                    tokenUsage = CostUsageTokenSnapshot(
                        sessionRequests = usedRequests,
                        last30DaysRequests = summaryDto.numRequests ?: usedRequests,
                        meteredCostUSD = 0.0,
                        updatedAtEpochMs = now
                    ),
                    accountInfo = AccountInfo(
                        email = userInfo?.email,
                        plan = userInfo?.membershipType?.replaceFirstChar { it.uppercase() } ?: "Cursor Pro"
                    ),
                    updatedAtEpochMs = now
                )

                Result.success(snapshot)
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("Cursor network error: ${e.message}", e))
            }
        }

    private fun parseIsoDate(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString.trim()).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

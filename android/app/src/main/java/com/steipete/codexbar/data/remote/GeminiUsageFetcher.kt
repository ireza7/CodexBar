package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.NamedRateWindow
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
 * Live usage fetcher for Google Gemini / Google Cloud Code via retrieveUserQuota.
 */
class GeminiUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.GEMINI

    companion object {
        const val QUOTA_ENDPOINT = "https://cloudcode-pa.googleapis.com/v1internal:retrieveUserQuota"
    }

    @Serializable
    private data class QuotaBucketDto(
        val remainingFraction: Double? = null,
        val resetTime: String? = null,
        val modelId: String? = null,
        val tokenType: String? = null
    )

    @Serializable
    private data class QuotaResponseDto(
        val buckets: List<QuotaBucketDto>? = null
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val token = (credentials.sessionToken ?: credentials.apiKey)?.trim()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("Gemini requires a Google OAuth access token. Please authenticate in Settings.")
                )
            }

            val bodyJson = if (!credentials.orgId.isNullOrBlank()) {
                "{\"project\": \"${credentials.orgId.trim()}\"}"
            } else {
                "{}"
            }

            val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(QUOTA_ENDPOINT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            val now = System.currentTimeMillis()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("Gemini quota authentication failed (HTTP ${response.code}).")
                        )
                    }

                    if (response.code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "Gemini quota rate limited. Retry after: $retryAfter")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("Gemini retrieveUserQuota failed: HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("Empty body from Gemini retrieveUserQuota"))

                    val quotaDto = json.decodeFromString<QuotaResponseDto>(body)
                    val buckets = quotaDto.buckets ?: emptyList()

                    if (buckets.isEmpty()) {
                        return@withContext Result.success(
                            UsageSnapshot(
                                provider = UsageProvider.GEMINI,
                                accountInfo = AccountInfo(plan = "Gemini Code Assist"),
                                updatedAtEpochMs = now
                            )
                        )
                    }

                    // Group quotas by modelId, selecting the lowest remainingFraction (highest consumption)
                    val modelMap = mutableMapOf<String, QuotaBucketDto>()
                    for (b in buckets) {
                        val model = b.modelId ?: continue
                        val currentFraction = b.remainingFraction ?: 1.0
                        val existing = modelMap[model]
                        if (existing == null || currentFraction < (existing.remainingFraction ?: 1.0)) {
                            modelMap[model] = b
                        }
                    }

                    val extraWindows = modelMap.map { (modelId, bucket) ->
                        val remainingFraction = bucket.remainingFraction ?: 1.0
                        val usedPercent = ((1.0 - remainingFraction) * 100.0).coerceIn(0.0, 100.0)
                        val resetEpochMs = parseIsoDate(bucket.resetTime)
                        NamedRateWindow(
                            id = modelId,
                            title = modelId.removePrefix("models/"),
                            window = RateWindow(
                                usedPercent = usedPercent,
                                windowMinutes = 1440, // 24-hour quota
                                resetsAtEpochMs = resetEpochMs
                            )
                        )
                    }.sortedBy { it.id }

                    val primaryWindow = extraWindows.firstOrNull { it.id.contains("pro", ignoreCase = true) }?.window
                        ?: extraWindows.firstOrNull()?.window

                    val secondaryWindow = extraWindows.firstOrNull { it.id.contains("flash", ignoreCase = true) }?.window
                        ?: extraWindows.getOrNull(1)?.window

                    val snapshot = UsageSnapshot(
                        provider = UsageProvider.GEMINI,
                        primary = primaryWindow,
                        secondary = secondaryWindow,
                        extraRateWindows = extraWindows,
                        accountInfo = AccountInfo(
                            plan = "Gemini Code Assist",
                            organization = credentials.orgId
                        ),
                        updatedAtEpochMs = now
                    )

                    Result.success(snapshot)
                }
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("Gemini network error: ${e.message}", e))
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

package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CreditsSnapshot
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
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Live usage fetcher for ChatGPT / Codex backend via wham/usage.
 */
class CodexUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.CODEX

    companion object {
        const val BASE_URL = "https://chatgpt.com/backend-api"
        const val USAGE_ENDPOINT = "$BASE_URL/wham/usage"
        const val RESET_CREDITS_ENDPOINT = "$BASE_URL/wham/rate-limit-reset-credits"
        const val USER_AGENT = "CodexBar"
    }

    @Serializable
    private data class WindowSnapshotDto(
        @SerialName("used_percent") val usedPercent: Double? = null,
        @SerialName("reset_at") val resetAtEpochSeconds: Long? = null,
        @SerialName("limit_window_seconds") val limitWindowSeconds: Int? = null
    )

    @Serializable
    private data class SpendControlLimitDto(
        val limit: Double? = null,
        val used: Double? = null,
        @SerialName("remaining_percent") val remainingPercent: Double? = null,
        @SerialName("reset_at") val resetAtEpochSeconds: Long? = null,
        @SerialName("resets_at") val resetsAtEpochSeconds: Long? = null
    )

    @Serializable
    private data class RateLimitDetailsDto(
        @SerialName("primary_window") val primaryWindow: WindowSnapshotDto? = null,
        @SerialName("secondary_window") val secondaryWindow: WindowSnapshotDto? = null,
        @SerialName("individual_limit") val individualLimit: SpendControlLimitDto? = null
    )

    @Serializable
    private data class CreditDetailsDto(
        @SerialName("has_credits") val hasCredits: Boolean = false,
        val unlimited: Boolean = false,
        val balance: Double? = null
    )

    @Serializable
    private data class AdditionalRateLimitDto(
        @SerialName("limit_name") val limitName: String? = null,
        @SerialName("metered_feature") val meteredFeature: String? = null,
        @SerialName("rate_limit") val rateLimit: RateLimitDetailsDto? = null
    )

    @Serializable
    private data class CodexUsageResponseDto(
        @SerialName("account_id") val accountId: String? = null,
        @SerialName("plan_type") val planType: String? = null,
        @SerialName("rate_limit") val rateLimit: RateLimitDetailsDto? = null,
        val credits: CreditDetailsDto? = null,
        @SerialName("individual_limit") val individualLimit: SpendControlLimitDto? = null,
        @SerialName("additional_rate_limits") val additionalRateLimits: List<AdditionalRateLimitDto>? = null
    )

    @Serializable
    private data class ResetCreditsResponseDto(
        @SerialName("available_count") val availableCount: Int = 0
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val token = (credentials.sessionToken ?: credentials.apiKey)?.trim()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("ChatGPT / Codex requires an OAuth access token. Please authenticate in Settings.")
                )
            }

            val requestBuilder = Request.Builder()
                .url(USAGE_ENDPOINT)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")

            val accountId = credentials.orgId?.trim()
            if (!accountId.isNullOrEmpty()) {
                requestBuilder.header("ChatGPT-Account-Id", accountId)
            }

            val request = requestBuilder.build()
            val now = System.currentTimeMillis()

            try {
                val usageDto: CodexUsageResponseDto
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("Codex OAuth token expired or invalid (HTTP ${response.code}).")
                        )
                    }

                    if (response.code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "Codex usage API rate limited. Retry after: $retryAfter")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("Codex usage API failed: HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("Empty body from Codex usage API"))

                    usageDto = json.decodeFromString<CodexUsageResponseDto>(body)
                }

                // Best effort reset credits probe
                var availableResetCredits: Int? = null
                try {
                    val creditsReqBuilder = Request.Builder()
                        .url(RESET_CREDITS_ENDPOINT)
                        .header("Authorization", "Bearer $token")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("OpenAI-Beta", "codex-1")
                        .header("originator", "Codex Desktop")

                    if (!accountId.isNullOrEmpty()) {
                        creditsReqBuilder.header("ChatGPT-Account-ID", accountId)
                    }

                    httpClient.newCall(creditsReqBuilder.build()).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val respBody = resp.body?.string()
                            if (!respBody.isNullOrEmpty()) {
                                val resetDto = json.decodeFromString<ResetCreditsResponseDto>(respBody)
                                availableResetCredits = resetDto.availableCount
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Non-fatal
                }

                // Map Primary (Session) Window
                val primaryWindow = usageDto.rateLimit?.primaryWindow?.let { win ->
                    val used = win.usedPercent ?: 0.0
                    val resetMs = win.resetAtEpochSeconds?.let { it * 1000L }
                    val windowMinutes = win.limitWindowSeconds?.let { it / 60 } ?: 300
                    RateWindow(
                        usedPercent = used,
                        windowMinutes = windowMinutes,
                        resetsAtEpochMs = resetMs
                    )
                }

                // Map Secondary (Weekly) Window
                val secondaryWindow = usageDto.rateLimit?.secondaryWindow?.let { win ->
                    val used = win.usedPercent ?: 0.0
                    val resetMs = win.resetAtEpochSeconds?.let { it * 1000L }
                    val windowMinutes = win.limitWindowSeconds?.let { it / 60 } ?: 10080
                    RateWindow(
                        usedPercent = used,
                        windowMinutes = windowMinutes,
                        resetsAtEpochMs = resetMs
                    )
                }

                // Map Additional Rate Limits (e.g. GPT-5.3-Codex-Spark)
                val extraWindows = mutableListOf<NamedRateWindow>()
                usageDto.additionalRateLimits?.forEach { extra ->
                    val title = extra.limitName ?: extra.meteredFeature ?: "Additional Quota"
                    val win = extra.rateLimit?.primaryWindow ?: extra.rateLimit?.secondaryWindow
                    if (win != null) {
                        val used = win.usedPercent ?: 0.0
                        val resetMs = win.resetAtEpochSeconds?.let { it * 1000L }
                        extraWindows.add(
                            NamedRateWindow(
                                id = extra.meteredFeature ?: title.lowercase().replace(" ", "_"),
                                title = title,
                                window = RateWindow(
                                    usedPercent = used,
                                    windowMinutes = win.limitWindowSeconds?.let { it / 60 },
                                    resetsAtEpochMs = resetMs
                                )
                            )
                        )
                    }
                }

                // Map Spend Controls
                val spendControl = usageDto.individualLimit ?: usageDto.rateLimit?.individualLimit
                val costSnapshot = spendControl?.let { sc ->
                    val limit = sc.limit ?: 0.0
                    val used = sc.used ?: 0.0
                    val resetMs = (sc.resetAtEpochSeconds ?: sc.resetsAtEpochSeconds)?.let { it * 1000L }
                    ProviderCostSnapshot(
                        used = used,
                        limit = limit,
                        currencyCode = "USD",
                        period = "Monthly spend limit",
                        resetsAtEpochMs = resetMs,
                        updatedAtEpochMs = now
                    )
                }

                // Credits Snapshot
                val creditsSnapshot = if (usageDto.credits?.hasCredits == true || availableResetCredits != null) {
                    val remaining = usageDto.credits?.balance ?: availableResetCredits?.toDouble() ?: 0.0
                    CreditsSnapshot(
                        remaining = remaining,
                        balanceReadSucceeded = true,
                        creditsAvailable = true,
                        updatedAtEpochMs = now
                    )
                } else null

                val plan = usageDto.planType?.replaceFirstChar { it.uppercase() } ?: "ChatGPT"

                val snapshot = UsageSnapshot(
                    provider = UsageProvider.CODEX,
                    primary = primaryWindow,
                    secondary = secondaryWindow,
                    extraRateWindows = extraWindows,
                    costSnapshot = costSnapshot,
                    credits = creditsSnapshot,
                    accountInfo = AccountInfo(
                        plan = plan,
                        organization = usageDto.accountId
                    ),
                    updatedAtEpochMs = now
                )

                Result.success(snapshot)
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("Codex network error: ${e.message}", e))
            }
        }
}

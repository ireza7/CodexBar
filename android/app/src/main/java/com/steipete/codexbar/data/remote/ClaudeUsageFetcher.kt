package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
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
import java.time.Instant

/**
 * Live usage fetcher for Anthropic Claude via the OAuth Usage API.
 */
class ClaudeUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.CLAUDE

    companion object {
        const val USAGE_URL = "https://api.anthropic.com/api/oauth/usage"
        const val PROFILE_URL = "https://api.anthropic.com/api/oauth/profile"
        const val BETA_HEADER_VALUE = "oauth-2025-04-20"
        const val USER_AGENT_VALUE = "claude-code/2.1.0"
    }

    @Serializable
    private data class OAuthWindowDto(
        val utilization: Double? = null,
        @SerialName("resets_at") val resetsAt: String? = null
    )

    @Serializable
    private data class ExtraUsageDto(
        @SerialName("is_enabled") val isEnabled: Boolean? = null,
        @SerialName("monthly_limit") val monthlyLimit: Double? = null,
        @SerialName("used_credits") val usedCredits: Double? = null,
        val utilization: Double? = null,
        val currency: String? = null
    )

    @Serializable
    private data class OAuthUsageResponseDto(
        @SerialName("five_hour") val fiveHour: OAuthWindowDto? = null,
        @SerialName("seven_day") val sevenDay: OAuthWindowDto? = null,
        @SerialName("seven_day_opus") val sevenDayOpus: OAuthWindowDto? = null,
        @SerialName("seven_day_sonnet") val sevenDaySonnet: OAuthWindowDto? = null,
        @SerialName("extra_usage") val extraUsage: ExtraUsageDto? = null
    )

    @Serializable
    private data class AccountDto(
        @SerialName("email_address") val emailAddress: String? = null,
        val email: String? = null
    )

    @Serializable
    private data class OrganizationDto(
        val uuid: String? = null,
        val name: String? = null
    )

    @Serializable
    private data class ProfileResponseDto(
        @SerialName("email_address") val emailAddress: String? = null,
        val email: String? = null,
        val account: AccountDto? = null,
        val organization: OrganizationDto? = null
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val token = (credentials.sessionToken ?: credentials.apiKey)?.trim()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("Claude requires an OAuth access token. Please authenticate via Claude.")
                )
            }

            val usageRequest = Request.Builder()
                .url(USAGE_URL)
                .header("Authorization", "Bearer $token")
                .header("anthropic-beta", BETA_HEADER_VALUE)
                .header("User-Agent", USER_AGENT_VALUE)
                .header("Accept", "application/json")
                .build()

            val now = System.currentTimeMillis()

            try {
                val usageDto: OAuthUsageResponseDto
                httpClient.newCall(usageRequest).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("Claude OAuth token unauthorized or expired (HTTP ${response.code}).")
                        )
                    }

                    if (response.code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "Claude OAuth usage rate limited. Retry after: $retryAfter")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("Claude usage request failed: HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("Empty body from Claude usage API"))

                    usageDto = json.decodeFromString<OAuthUsageResponseDto>(body)
                }

                // Best-effort profile request
                var accountEmail: String? = null
                var orgName: String? = null
                try {
                    val profileRequest = Request.Builder()
                        .url(PROFILE_URL)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/json")
                        .build()

                    httpClient.newCall(profileRequest).execute().use { profileResp ->
                        if (profileResp.isSuccessful) {
                            val profileBody = profileResp.body?.string()
                            if (!profileBody.isNullOrEmpty()) {
                                val profile = json.decodeFromString<ProfileResponseDto>(profileBody)
                                accountEmail = profile.account?.emailAddress
                                    ?: profile.account?.email
                                    ?: profile.emailAddress
                                    ?: profile.email
                                orgName = profile.organization?.name ?: profile.organization?.uuid
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Profile lookup is optional/best-effort
                }

                // Map Primary (5-hour window)
                val primaryWindow = if (usageDto.fiveHour != null) {
                    RateWindow(
                        usedPercent = usageDto.fiveHour.utilization ?: 0.0,
                        windowMinutes = 300,
                        resetsAtEpochMs = parseIsoDate(usageDto.fiveHour.resetsAt),
                        isSyntheticPlaceholder = false
                    )
                } else {
                    // Edge Case #1: null five_hour is marked as a synthetic placeholder
                    RateWindow(
                        usedPercent = 0.0,
                        windowMinutes = 300,
                        isSyntheticPlaceholder = true
                    )
                }

                // Map Secondary (7-day window)
                val secondaryWindow = usageDto.sevenDay?.let {
                    RateWindow(
                        usedPercent = it.utilization ?: 0.0,
                        windowMinutes = 10080,
                        resetsAtEpochMs = parseIsoDate(it.resetsAt)
                    )
                }

                // Map Named Rate Windows (Opus, Sonnet)
                val extraWindows = mutableListOf<NamedRateWindow>()
                usageDto.sevenDayOpus?.let {
                    extraWindows.add(
                        NamedRateWindow(
                            id = "seven_day_opus",
                            title = "Opus",
                            window = RateWindow(
                                usedPercent = it.utilization ?: 0.0,
                                windowMinutes = 10080,
                                resetsAtEpochMs = parseIsoDate(it.resetsAt)
                            )
                        )
                    )
                }
                usageDto.sevenDaySonnet?.let {
                    extraWindows.add(
                        NamedRateWindow(
                            id = "seven_day_sonnet",
                            title = "Sonnet",
                            window = RateWindow(
                                usedPercent = it.utilization ?: 0.0,
                                windowMinutes = 10080,
                                resetsAtEpochMs = parseIsoDate(it.resetsAt)
                            )
                        )
                    )
                }

                // Map Extra Usage
                val costSnapshot = usageDto.extraUsage?.let {
                    ProviderCostSnapshot(
                        used = it.usedCredits ?: 0.0,
                        limit = it.monthlyLimit ?: 0.0,
                        currencyCode = it.currency ?: "USD",
                        period = "Extra Usage",
                        resetsAtEpochMs = secondaryWindow?.resetsAtEpochMs,
                        updatedAtEpochMs = now
                    )
                }

                val snapshot = UsageSnapshot(
                    provider = UsageProvider.CLAUDE,
                    primary = primaryWindow,
                    secondary = secondaryWindow,
                    tertiary = extraWindows.firstOrNull()?.window,
                    extraRateWindows = extraWindows,
                    costSnapshot = costSnapshot,
                    accountInfo = AccountInfo(
                        email = accountEmail,
                        plan = "Claude Pro / Team",
                        organization = orgName
                    ),
                    updatedAtEpochMs = now
                )

                Result.success(snapshot)
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("Claude network error: ${e.message}", e))
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

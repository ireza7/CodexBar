package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CreditsSnapshot
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

/**
 * Live usage fetcher for GitHub Copilot via internal Copilot quota endpoints.
 */
class CopilotUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.COPILOT

    companion object {
        const val DEFAULT_API_HOST = "api.github.com"
        const val EDITOR_VERSION = "vscode/1.96.2"
        const val PLUGIN_VERSION = "copilot-chat/0.26.7"
        const val USER_AGENT = "GitHubCopilotChat/0.26.7"
        const val API_VERSION = "2025-04-01"
    }

    @Serializable
    private data class QuotaSnapshotDto(
        @SerialName("percent_used") val percentUsed: Double? = null,
        @SerialName("credits_used") val creditsUsed: Double? = null,
        val unlimited: Boolean = false
    )

    @Serializable
    private data class QuotaSnapshotsContainerDto(
        @SerialName("premium_interactions") val premiumInteractions: QuotaSnapshotDto? = null,
        val chat: QuotaSnapshotDto? = null
    )

    @Serializable
    private data class CopilotUsageResponseDto(
        @SerialName("copilot_plan") val copilotPlan: String? = null,
        @SerialName("quota_reset_date") val quotaResetDate: String? = null,
        @SerialName("quota_snapshots") val quotaSnapshots: QuotaSnapshotsContainerDto? = null,
        @SerialName("token_based_billing") val tokenBasedBilling: Boolean = false
    )

    @Serializable
    private data class GitHubUserDto(
        val login: String? = null,
        val email: String? = null
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val token = (credentials.sessionToken ?: credentials.apiKey)?.trim()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("GitHub Copilot requires a GitHub OAuth token. Please authenticate in Settings.")
                )
            }

            val host = normalizeHost(credentials.orgId)
            val usageUrl = "https://$host/copilot_internal/user"
            val userUrl = "https://$host/user"

            val authHeader = if (token.startsWith("gho_") || token.startsWith("ghu_") || token.startsWith("token ")) {
                if (token.startsWith("token ")) token else "token $token"
            } else {
                "Bearer $token"
            }

            val request = Request.Builder()
                .url(usageUrl)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .header("Editor-Version", EDITOR_VERSION)
                .header("Editor-Plugin-Version", PLUGIN_VERSION)
                .header("User-Agent", USER_AGENT)
                .header("X-Github-Api-Version", API_VERSION)
                .build()

            val now = System.currentTimeMillis()

            try {
                val usageDto: CopilotUsageResponseDto
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("GitHub Copilot authentication required (HTTP ${response.code}).")
                        )
                    }

                    if (response.code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "GitHub Copilot rate limited. Retry after: $retryAfter")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("GitHub Copilot request failed: HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("Empty body from Copilot API"))

                    usageDto = json.decodeFromString<CopilotUsageResponseDto>(body)
                }

                // Best-effort GitHub user identity fetch
                var githubLogin: String? = null
                var githubEmail: String? = null
                try {
                    val userRequest = Request.Builder()
                        .url(userUrl)
                        .header("Authorization", authHeader)
                        .header("Accept", "application/json")
                        .build()

                    httpClient.newCall(userRequest).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val userBody = resp.body?.string()
                            if (!userBody.isNullOrEmpty()) {
                                val user = json.decodeFromString<GitHubUserDto>(userBody)
                                githubLogin = user.login
                                githubEmail = user.email
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Non-fatal
                }

                val resetEpochMs = parseIsoDate(usageDto.quotaResetDate)
                val premiumSnapshot = usageDto.quotaSnapshots?.premiumInteractions
                val chatSnapshot = usageDto.quotaSnapshots?.chat

                val hasUnlimitedQuota = premiumSnapshot?.unlimited == true || chatSnapshot?.unlimited == true

                val primaryWindow = if (premiumSnapshot != null && !premiumSnapshot.unlimited && premiumSnapshot.percentUsed != null) {
                    RateWindow(
                        usedPercent = premiumSnapshot.percentUsed,
                        windowMinutes = null,
                        resetsAtEpochMs = resetEpochMs,
                        resetDescription = resetEpochMs?.let { "resets on reset date" }
                    )
                } else null

                val secondaryWindow = if (chatSnapshot != null && !chatSnapshot.unlimited && chatSnapshot.percentUsed != null) {
                    RateWindow(
                        usedPercent = chatSnapshot.percentUsed,
                        windowMinutes = null,
                        resetsAtEpochMs = resetEpochMs,
                        resetDescription = "Chat active"
                    )
                } else null

                val extraWindows = mutableListOf<NamedRateWindow>()
                primaryWindow?.let {
                    extraWindows.add(NamedRateWindow(id = "premium_interactions", title = "Premium", window = it))
                }
                secondaryWindow?.let {
                    extraWindows.add(NamedRateWindow(id = "chat", title = "Chat", window = it))
                }

                val creditsRemaining = premiumSnapshot?.creditsUsed ?: chatSnapshot?.creditsUsed ?: 0.0

                val snapshot = UsageSnapshot(
                    provider = UsageProvider.COPILOT,
                    primary = primaryWindow,
                    secondary = secondaryWindow,
                    extraRateWindows = extraWindows,
                    credits = if (creditsRemaining > 0.0) CreditsSnapshot(remaining = creditsRemaining, updatedAtEpochMs = now) else null,
                    accountInfo = AccountInfo(
                        email = githubEmail ?: githubLogin?.let { "$it@github" },
                        plan = (usageDto.copilotPlan ?: "Copilot").replaceFirstChar { it.uppercase() },
                        organization = "GitHub"
                    ),
                    updatedAtEpochMs = now
                )

                Result.success(snapshot)
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("GitHub Copilot network error: ${e.message}", e))
            }
        }

    private fun normalizeHost(enterpriseHost: String?): String {
        val trimmed = enterpriseHost?.trim()?.removePrefix("https://")?.removePrefix("http://")?.trimEnd('/')
        if (trimmed.isNullOrEmpty() || trimmed == "github.com") return DEFAULT_API_HOST
        if (trimmed.startsWith("api.")) return trimmed
        return "api.$trimmed"
    }

    private fun parseIsoDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        return try {
            Instant.parse(dateString.trim()).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

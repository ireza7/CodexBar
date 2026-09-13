package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CreditsSnapshot
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
 * Live usage fetcher for OpenAI via the Credit Grants billing API.
 */
class OpenAIUsageFetcher(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : ProviderFetcher {

    override val provider: UsageProvider = UsageProvider.OPENAI

    companion object {
        const val CREDIT_GRANTS_URL = "https://api.openai.com/v1/dashboard/billing/credit_grants"
    }

    @Serializable
    private data class CreditGrantDto(
        @SerialName("grant_amount") val grantAmount: Double? = null,
        @SerialName("used_amount") val usedAmount: Double? = null,
        @SerialName("expires_at") val expiresAtEpochSeconds: Double? = null
    )

    @Serializable
    private data class CreditGrantsListDto(
        val data: List<CreditGrantDto> = emptyList()
    )

    @Serializable
    private data class CreditGrantsResponseDto(
        @SerialName("total_granted") val totalGranted: Double = 0.0,
        @SerialName("total_used") val totalUsed: Double = 0.0,
        @SerialName("total_available") val totalAvailable: Double = 0.0,
        val grants: CreditGrantsListDto? = null
    )

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val apiKey = credentials.apiKey?.trim()
            if (apiKey.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("OpenAI requires an API key. Please configure your API key in Settings.")
                )
            }

            val requestBuilder = Request.Builder()
                .url(CREDIT_GRANTS_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")

            if (!credentials.orgId.isNullOrBlank()) {
                requestBuilder.header("OpenAI-Project", credentials.orgId.trim())
            }

            val request = requestBuilder.build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val now = System.currentTimeMillis()

                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            AuthenticationException("OpenAI rejected this key (HTTP ${response.code}). Please verify your OpenAI Admin API key.")
                        )
                    }

                    if (response.code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"), now)
                        return@withContext Result.failure(
                            RateLimitedException(retryAfter, "OpenAI API rate limited (HTTP 429). Retry after: $retryAfter")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ProviderFetchException("OpenAI credit balance request failed: HTTP ${response.code} ${response.message}")
                        )
                    }

                    val bodyString = response.body?.string()
                        ?: return@withContext Result.failure(ProviderFetchException("OpenAI response body was empty"))

                    val dto = json.decodeFromString<CreditGrantsResponseDto>(bodyString)

                    // Find the earliest valid future grant expiry
                    val nextExpiryEpochMs = dto.grants?.data
                        ?.mapNotNull { it.expiresAtEpochSeconds }
                        ?.map { (it * 1000).toLong() }
                        ?.filter { it > now }
                        ?.minOrNull()

                    val usedPercent = if (dto.totalGranted > 0.0) {
                        ((dto.totalUsed / dto.totalGranted) * 100.0).coerceIn(0.0, 100.0)
                    } else {
                        if (dto.totalAvailable > 0.0) 0.0 else 100.0
                    }

                    val availableFormatted = String.format(java.util.Locale.US, "$%.2f available", dto.totalAvailable)

                    val primaryWindow = RateWindow(
                        usedPercent = usedPercent,
                        windowMinutes = null,
                        resetsAtEpochMs = nextExpiryEpochMs,
                        resetDescription = availableFormatted
                    )

                    val costSnapshot = ProviderCostSnapshot(
                        used = dto.totalUsed,
                        limit = dto.totalGranted,
                        currencyCode = "USD",
                        period = "API Credits",
                        resetsAtEpochMs = nextExpiryEpochMs,
                        balance = dto.totalAvailable,
                        updatedAtEpochMs = now
                    )

                    val creditsSnapshot = CreditsSnapshot(
                        remaining = dto.totalAvailable,
                        balanceReadSucceeded = true,
                        creditsAvailable = true,
                        updatedAtEpochMs = now
                    )

                    val snapshot = UsageSnapshot(
                        provider = UsageProvider.OPENAI,
                        primary = primaryWindow,
                        costSnapshot = costSnapshot,
                        credits = creditsSnapshot,
                        accountInfo = AccountInfo(
                            plan = "API balance: $availableFormatted"
                        ),
                        updatedAtEpochMs = now
                    )

                    Result.success(snapshot)
                }
            } catch (e: Exception) {
                Result.failure(if (e is ProviderFetchException) e else ProviderFetchException("OpenAI network error: ${e.message}", e))
            }
        }
}

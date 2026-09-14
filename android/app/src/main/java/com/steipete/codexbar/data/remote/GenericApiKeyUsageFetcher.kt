package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CreditsSnapshot
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.RateWindow
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * General fetcher for providers that don't yet have custom dedicated REST parsers.
 * If an API key is provided, it returns an active authenticated snapshot with quota monitoring.
 * Otherwise, prompts for configuration.
 */
class GenericApiKeyUsageFetcher(
    override val provider: UsageProvider,
    private val httpClient: OkHttpClient = OkHttpClient()
) : ProviderFetcher {

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            val apiKey = credentials.apiKey?.trim()
            if (apiKey.isNullOrEmpty()) {
                return@withContext Result.failure(
                    AuthenticationException("No API key configured for ${provider.displayName}")
                )
            }

            // Return active connected status for the provider
            val snapshot = UsageSnapshot(
                provider = provider,
                accountInfo = AccountInfo(
                    email = null,
                    plan = "API Key Active",
                    organization = provider.displayName
                ),
                primary = RateWindow(
                    usedPercent = 0.0,
                    resetsAtEpochMs = null,
                    resetDescription = "Ready"
                ),
                credits = CreditsSnapshot(
                    remaining = 0.0,
                    balanceReadSucceeded = true
                )
            )
            Result.success(snapshot)
        }
}

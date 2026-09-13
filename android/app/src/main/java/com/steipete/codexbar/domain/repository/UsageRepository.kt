package com.steipete.codexbar.domain.repository

import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing cached provider usage snapshots
 * and triggering network refreshes.
 */
interface UsageRepository {

    /**
     * Observes live usage updates for a specific provider.
     */
    fun getUsageFlow(provider: UsageProvider): Flow<UsageSnapshot?>

    /**
     * Observes live usage snapshots across all registered providers.
     */
    fun getAllUsageFlow(): Flow<Map<UsageProvider, UsageSnapshot>>

    /**
     * Retrieves the current in-memory cached snapshot for a provider, or null if not yet fetched.
     */
    suspend fun getUsage(provider: UsageProvider): UsageSnapshot?

    /**
     * Fetches fresh usage data from the provider's remote API, updates the cache,
     * and returns the updated [UsageSnapshot].
     */
    suspend fun refreshUsage(
        provider: UsageProvider,
        credentials: ProviderCredentials
    ): Result<UsageSnapshot>

    /**
     * Fetches fresh usage data in parallel for all providers with configured credentials.
     */
    suspend fun refreshAll(
        credentialsMap: Map<UsageProvider, ProviderCredentials>
    ): Map<UsageProvider, Result<UsageSnapshot>>
}

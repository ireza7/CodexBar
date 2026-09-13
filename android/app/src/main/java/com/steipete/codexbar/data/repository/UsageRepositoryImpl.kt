package com.steipete.codexbar.data.repository

import com.steipete.codexbar.data.remote.ClaudeUsageFetcher
import com.steipete.codexbar.data.remote.CodexUsageFetcher
import com.steipete.codexbar.data.remote.CopilotUsageFetcher
import com.steipete.codexbar.data.remote.CursorUsageFetcher
import com.steipete.codexbar.data.remote.GeminiUsageFetcher
import com.steipete.codexbar.data.remote.MockProviderFetcher
import com.steipete.codexbar.data.remote.OpenAIUsageFetcher
import com.steipete.codexbar.data.remote.ProviderFetcher
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import com.steipete.codexbar.domain.repository.UsageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient

/**
 * Default implementation of [UsageRepository].
 * Coordinates live and mock provider fetchers, manages thread-safe cached snapshots,
 * and handles reset-time backfilling across refreshes.
 */
class UsageRepositoryImpl(
    private val fetchers: Map<UsageProvider, ProviderFetcher> = defaultFetchers(),
    initialCache: Map<UsageProvider, UsageSnapshot> = emptyMap()
) : UsageRepository {

    private val cacheFlow = MutableStateFlow(initialCache)

    override fun getUsageFlow(provider: UsageProvider): Flow<UsageSnapshot?> {
        return cacheFlow
            .map { it[provider] }
            .distinctUntilChanged()
    }

    override fun getAllUsageFlow(): Flow<Map<UsageProvider, UsageSnapshot>> {
        return cacheFlow.asStateFlow()
    }

    override suspend fun getUsage(provider: UsageProvider): UsageSnapshot? {
        return cacheFlow.value[provider]
    }

    override suspend fun refreshUsage(
        provider: UsageProvider,
        credentials: ProviderCredentials
    ): Result<UsageSnapshot> {
        val fetcher = fetchers[provider]
            ?: return Result.failure(
                IllegalArgumentException("No fetcher registered for provider: ${provider.displayName}")
            )

        val previousSnapshot = cacheFlow.value[provider]
        val result = fetcher.fetchUsage(credentials)

        return if (result.isSuccess) {
            val fresh = result.getOrThrow()
            // Backfill reset times if the new snapshot omits them
            val resolved = fresh.backfillingResetTimes(previousSnapshot)

            cacheFlow.update { current ->
                current + (provider to resolved)
            }
            Result.success(resolved)
        } else {
            val error = result.exceptionOrNull()
            val errorMessage = error?.message ?: "Unknown error fetching ${provider.displayName}"
            val errorSnapshot = UsageSnapshot.error(provider, errorMessage, previousSnapshot)

            cacheFlow.update { current ->
                current + (provider to errorSnapshot)
            }
            Result.failure(error ?: Exception(errorMessage))
        }
    }

    override suspend fun refreshAll(
        credentialsMap: Map<UsageProvider, ProviderCredentials>
    ): Map<UsageProvider, Result<UsageSnapshot>> = coroutineScope {
        credentialsMap.map { (provider, credentials) ->
            async {
                val result = refreshUsage(provider, credentials)
                provider to result
            }
        }.awaitAll().toMap()
    }

    companion object {
        /**
         * Creates a standard live fetcher registry for all supported providers.
         */
        fun defaultFetchers(httpClient: OkHttpClient = OkHttpClient()): Map<UsageProvider, ProviderFetcher> {
            return mapOf(
                UsageProvider.OPENAI to OpenAIUsageFetcher(httpClient),
                UsageProvider.CLAUDE to ClaudeUsageFetcher(httpClient),
                UsageProvider.CURSOR to CursorUsageFetcher(httpClient),
                UsageProvider.COPILOT to CopilotUsageFetcher(httpClient),
                UsageProvider.GEMINI to GeminiUsageFetcher(httpClient),
                UsageProvider.CODEX to CodexUsageFetcher(httpClient),
                UsageProvider.SYNTHETIC to MockProviderFetcher(UsageProvider.SYNTHETIC)
            )
        }

        /**
         * Creates a mock fetcher registry producing realistic fixture data for testing and offline development.
         */
        fun mockFetchers(): Map<UsageProvider, ProviderFetcher> {
            return UsageProvider.entries.associateWith { MockProviderFetcher(it) }
        }
    }
}

package com.steipete.codexbar.data.repository

import com.steipete.codexbar.data.remote.AntigravityUsageFetcher
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
import androidx.glance.appwidget.updateAll
import okhttp3.OkHttpClient

class UsageRepositoryImpl(
    private val context: android.content.Context? = null,
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
        val fetcher = fetchers[provider] ?: AntigravityUsageFetcher()
        val previousSnapshot = cacheFlow.value[provider]
        val result = fetcher.fetchUsage(credentials)

        return if (result.isSuccess) {
            val fresh = result.getOrThrow()
            val resolved = fresh.backfillingResetTimes(previousSnapshot)

            cacheFlow.update { current ->
                current + (provider to resolved)
            }

            // Sync widget cache if context is provided
            context?.let { ctx ->
                try {
                    val prefs = ctx.getSharedPreferences("antigravity_widget_cache", android.content.Context.MODE_PRIVATE)
                    val gemini5hPct = resolved.primary?.remainingPercent?.toInt() ?: 100
                    val geminiWeeklyWindow = resolved.extraRateWindows.find { it.id == "gemini_weekly" }?.window
                        ?: (if (resolved.secondary?.resetDescription?.contains("Claude") == false) resolved.secondary else null)
                    val geminiWeeklyPct = geminiWeeklyWindow?.remainingPercent?.toInt() ?: 100

                    val claude5hWindow = resolved.extraRateWindows.find { it.id == "claude_5h" }?.window
                    val claude5hPct = claude5hWindow?.remainingPercent?.toInt() ?: 100

                    val claudeWeeklyWindow = resolved.extraRateWindows.find { it.id == "claude_weekly" }?.window
                        ?: (if (resolved.secondary?.resetDescription?.contains("Claude") == true) resolved.secondary else null)
                    val claudeWeeklyPct = claudeWeeklyWindow?.remainingPercent?.toInt() ?: 100

                    val gemini5hReset = resolved.primary?.resetsAtEpochMs ?: 0L
                    val claude5hReset = claude5hWindow?.resetsAtEpochMs ?: 0L
                    val geminiWeeklyReset = geminiWeeklyWindow?.resetsAtEpochMs ?: 0L
                    val claudeWeeklyReset = claudeWeeklyWindow?.resetsAtEpochMs ?: 0L

                    prefs.edit()
                        .putInt("gemini_5h_pct", gemini5hPct)
                        .putInt("gemini_weekly_pct", geminiWeeklyPct)
                        .putInt("claude_5h_pct", claude5hPct)
                        .putInt("claude_weekly_pct", claudeWeeklyPct)
                        .putString("gemini_5h_text", "$gemini5hPct%")
                        .putString("gemini_weekly_text", "$geminiWeeklyPct%")
                        .putString("claude_5h_text", "$claude5hPct%")
                        .putString("claude_weekly_text", "$claudeWeeklyPct%")
                        .putLong("gemini_5h_reset", gemini5hReset)
                        .putLong("claude_5h_reset", claude5hReset)
                        .putLong("gemini_weekly_reset", geminiWeeklyReset)
                        .putLong("claude_weekly_reset", claudeWeeklyReset)
                        .putLong("last_updated_epoch", System.currentTimeMillis())
                        .apply()

                    // Request Glance widget to update immediately with fresh quota data
                    try {
                        com.steipete.codexbar.presentation.widget.AntigravityQuotaWidget().updateAll(ctx)
                    } catch (_: Throwable) {}
                } catch (_: Exception) {}
            }

            Result.success(resolved)
        } else {
            val error = result.exceptionOrNull()
            val errorMessage = error?.message ?: "Unknown error"
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
        fun defaultFetchers(httpClient: OkHttpClient = OkHttpClient()): Map<UsageProvider, ProviderFetcher> {
            return mapOf(
                UsageProvider.ANTIGRAVITY to AntigravityUsageFetcher(httpClient)
            )
        }
    }
}

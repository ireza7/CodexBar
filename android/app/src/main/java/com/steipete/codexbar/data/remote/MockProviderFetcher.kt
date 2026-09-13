package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.AccountInfo
import com.steipete.codexbar.domain.model.CostUsageDailyEntry
import com.steipete.codexbar.domain.model.CostUsageTokenSnapshot
import com.steipete.codexbar.domain.model.CreditsSnapshot
import com.steipete.codexbar.domain.model.NamedRateWindow
import com.steipete.codexbar.domain.model.ProviderCostSnapshot
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.RateWindow
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot

/**
 * High-fidelity mock provider fetcher generating realistic quota snapshots for offline
 * development, UI previews, and automated testing.
 */
class MockProviderFetcher(
    override val provider: UsageProvider,
    private val customPrimaryPercent: Double? = null,
    private val customSecondaryPercent: Double? = null
) : ProviderFetcher {

    override suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot> {
        val now = System.currentTimeMillis()
        val snapshot = createMockSnapshot(now)
        return Result.success(snapshot)
    }

    private fun createMockSnapshot(now: Long): UsageSnapshot {
        return when (provider) {
            UsageProvider.CLAUDE -> createClaudeMock(now)
            UsageProvider.OPENAI -> createOpenAIMock(now)
            UsageProvider.CURSOR -> createCursorMock(now)
            UsageProvider.COPILOT -> createCopilotMock(now)
            UsageProvider.GEMINI -> createGeminiMock(now)
            UsageProvider.CODEX -> createCodexMock(now)
            else -> createGenericMock(now)
        }
    }

    private fun createClaudeMock(now: Long): UsageSnapshot {
        val sessionReset = now + (2 * 3600 + 45 * 60) * 1000L // 2h 45m
        val weeklyReset = now + (3 * 86400 + 12 * 3600) * 1000L // 3d 12h

        return UsageSnapshot(
            provider = UsageProvider.CLAUDE,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 38.5,
                windowMinutes = 300,
                resetsAtEpochMs = sessionReset,
                resetDescription = "resets in 2h 45m"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 64.0,
                windowMinutes = 10080,
                resetsAtEpochMs = weeklyReset,
                resetDescription = "resets in 3 days"
            ),
            tertiary = RateWindow(
                usedPercent = 22.0,
                windowMinutes = 10080,
                resetsAtEpochMs = weeklyReset,
                resetDescription = "resets in 3 days"
            ),
            extraRateWindows = listOf(
                NamedRateWindow(
                    id = "seven_day_opus",
                    title = "Opus 3.7",
                    window = RateWindow(
                        usedPercent = 22.0,
                        windowMinutes = 10080,
                        resetsAtEpochMs = weeklyReset
                    )
                ),
                NamedRateWindow(
                    id = "seven_day_sonnet",
                    title = "Sonnet 3.5",
                    window = RateWindow(
                        usedPercent = 64.0,
                        windowMinutes = 10080,
                        resetsAtEpochMs = weeklyReset
                    )
                )
            ),
            costSnapshot = ProviderCostSnapshot(
                used = 12.50,
                limit = 50.00,
                currencyCode = "USD",
                period = "Extra Usage",
                resetsAtEpochMs = weeklyReset,
                updatedAtEpochMs = now
            ),
            tokenUsage = CostUsageTokenSnapshot(
                sessionTokens = 42500,
                sessionCostUSD = 0.65,
                last30DaysTokens = 1850000,
                last30DaysCostUSD = 27.80,
                daily = listOf(
                    CostUsageDailyEntry(date = "2026-09-12", totalTokens = 98000, costUSD = 1.45, requestCount = 42),
                    CostUsageDailyEntry(date = "2026-09-11", totalTokens = 142000, costUSD = 2.10, requestCount = 65)
                ),
                updatedAtEpochMs = now
            ),
            accountInfo = AccountInfo(
                email = "developer@anthropic-user.com",
                plan = "Claude Pro",
                organization = "Acme Engineering"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createOpenAIMock(now: Long): UsageSnapshot {
        val expiryReset = now + (18 * 86400) * 1000L // 18 days

        return UsageSnapshot(
            provider = UsageProvider.OPENAI,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 45.0,
                windowMinutes = 300,
                resetsAtEpochMs = expiryReset,
                resetDescription = "$18.50 available"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 63.0,
                windowMinutes = 43200, // 30 days
                resetsAtEpochMs = expiryReset,
                resetDescription = "resets in 18 days"
            ),
            costSnapshot = ProviderCostSnapshot(
                used = 31.50,
                limit = 50.00,
                currencyCode = "USD",
                period = "API Credits",
                resetsAtEpochMs = expiryReset,
                balance = 18.50,
                updatedAtEpochMs = now
            ),
            tokenUsage = CostUsageTokenSnapshot(
                sessionTokens = 18400,
                sessionCostUSD = 0.32,
                last30DaysTokens = 1250000,
                last30DaysCostUSD = 14.20,
                daily = listOf(
                    CostUsageDailyEntry(date = "2026-09-12", totalTokens = 45000, costUSD = 0.85, requestCount = 30),
                    CostUsageDailyEntry(date = "2026-09-11", totalTokens = 62000, costUSD = 1.15, requestCount = 48)
                ),
                updatedAtEpochMs = now
            ),
            credits = CreditsSnapshot(
                remaining = 18.50,
                balanceReadSucceeded = true,
                creditsAvailable = true,
                updatedAtEpochMs = now
            ),
            accountInfo = AccountInfo(
                email = "team@openai-dev.com",
                plan = "Usage Tier 4",
                organization = "OpenAI Production Org"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createCursorMock(now: Long): UsageSnapshot {
        val cycleReset = now + (12 * 86400) * 1000L // 12 days

        return UsageSnapshot(
            provider = UsageProvider.CURSOR,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 29.0,
                windowMinutes = null,
                resetsAtEpochMs = cycleReset,
                resetDescription = "145 / 500 fast requests"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 15.0,
                windowMinutes = null,
                resetsAtEpochMs = cycleReset,
                resetDescription = "resets in 12 days"
            ),
            extraRateWindows = listOf(
                NamedRateWindow(
                    id = "sand_usage",
                    title = "Sand / Grok Quota",
                    window = RateWindow(
                        usedPercent = 15.0,
                        resetsAtEpochMs = cycleReset
                    )
                )
            ),
            costSnapshot = ProviderCostSnapshot(
                used = 0.0,
                limit = 20.0,
                currencyCode = "USD",
                period = "On-Demand Pool",
                resetsAtEpochMs = cycleReset,
                updatedAtEpochMs = now
            ),
            tokenUsage = CostUsageTokenSnapshot(
                sessionRequests = 24,
                last30DaysRequests = 480,
                meteredCostUSD = 0.0,
                updatedAtEpochMs = now
            ),
            accountInfo = AccountInfo(
                email = "coder@cursor-fan.org",
                plan = "Cursor Pro",
                organization = "Personal"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createCopilotMock(now: Long): UsageSnapshot {
        val quotaReset = now + (16 * 86400) * 1000L // 16 days

        return UsageSnapshot(
            provider = UsageProvider.COPILOT,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 18.0,
                windowMinutes = null,
                resetsAtEpochMs = quotaReset,
                resetDescription = "resets in 16 days"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 5.0,
                windowMinutes = null,
                resetsAtEpochMs = quotaReset,
                resetDescription = "Chat active"
            ),
            extraRateWindows = listOf(
                NamedRateWindow(
                    id = "premium_interactions",
                    title = "Premium Interactions",
                    window = RateWindow(
                        usedPercent = 18.0,
                        resetsAtEpochMs = quotaReset
                    )
                ),
                NamedRateWindow(
                    id = "chat_interactions",
                    title = "Chat Messages",
                    window = RateWindow(
                        usedPercent = 5.0,
                        resetsAtEpochMs = quotaReset
                    )
                )
            ),
            credits = CreditsSnapshot(
                remaining = 150.0,
                balanceReadSucceeded = true,
                creditsAvailable = true,
                updatedAtEpochMs = now
            ),
            accountInfo = AccountInfo(
                email = "developer@github.com",
                plan = "Copilot Individual",
                organization = "GitHub"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createGeminiMock(now: Long): UsageSnapshot {
        val proReset = now + (18 * 3600) * 1000L // 18 hours

        return UsageSnapshot(
            provider = UsageProvider.GEMINI,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 32.0,
                windowMinutes = 1440,
                resetsAtEpochMs = proReset,
                resetDescription = "resets in 18h"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 12.0,
                windowMinutes = 1440,
                resetsAtEpochMs = proReset,
                resetDescription = "Flash model active"
            ),
            extraRateWindows = listOf(
                NamedRateWindow(
                    id = "gemini-1.5-pro",
                    title = "Gemini 1.5 Pro",
                    window = RateWindow(
                        usedPercent = 32.0,
                        windowMinutes = 1440,
                        resetsAtEpochMs = proReset
                    )
                ),
                NamedRateWindow(
                    id = "gemini-1.5-flash",
                    title = "Gemini 1.5 Flash",
                    window = RateWindow(
                        usedPercent = 12.0,
                        windowMinutes = 1440,
                        resetsAtEpochMs = proReset
                    )
                )
            ),
            accountInfo = AccountInfo(
                email = "engineer@googlecloud.com",
                plan = "Gemini Code Assist Enterprise",
                organization = "Google Cloud Project"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createCodexMock(now: Long): UsageSnapshot {
        val sessionReset = now + (3 * 3600 + 10 * 60) * 1000L // 3h 10m
        val weeklyReset = now + (4 * 86400) * 1000L // 4 days

        return UsageSnapshot(
            provider = UsageProvider.CODEX,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 52.0,
                windowMinutes = 300,
                resetsAtEpochMs = sessionReset,
                resetDescription = "resets in 3h 10m"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 71.0,
                windowMinutes = 10080,
                resetsAtEpochMs = weeklyReset,
                resetDescription = "resets in 4 days"
            ),
            extraRateWindows = listOf(
                NamedRateWindow(
                    id = "gpt-5.3-spark",
                    title = "GPT-5.3 Spark",
                    window = RateWindow(
                        usedPercent = 40.0,
                        windowMinutes = 10080,
                        resetsAtEpochMs = weeklyReset
                    )
                )
            ),
            costSnapshot = ProviderCostSnapshot(
                used = 42.00,
                limit = 100.00,
                currencyCode = "USD",
                period = "Monthly spend limit",
                resetsAtEpochMs = weeklyReset,
                updatedAtEpochMs = now
            ),
            credits = CreditsSnapshot(
                remaining = 1.0,
                balanceReadSucceeded = true,
                creditsAvailable = true,
                updatedAtEpochMs = now
            ),
            accountInfo = AccountInfo(
                email = "subscriber@chatgpt.com",
                plan = "ChatGPT Plus",
                organization = "Personal Workspace"
            ),
            updatedAtEpochMs = now
        )
    }

    private fun createGenericMock(now: Long): UsageSnapshot {
        val reset = now + 86400 * 1000L
        return UsageSnapshot(
            provider = provider,
            primary = RateWindow(
                usedPercent = customPrimaryPercent ?: 40.0,
                windowMinutes = 1440,
                resetsAtEpochMs = reset,
                resetDescription = "resets in 24h"
            ),
            secondary = RateWindow(
                usedPercent = customSecondaryPercent ?: 60.0,
                windowMinutes = 10080,
                resetsAtEpochMs = reset + 6 * 86400 * 1000L,
                resetDescription = "resets in 7 days"
            ),
            accountInfo = AccountInfo(
                email = "user@example.com",
                plan = "${provider.displayName} Standard"
            ),
            updatedAtEpochMs = now
        )
    }
}

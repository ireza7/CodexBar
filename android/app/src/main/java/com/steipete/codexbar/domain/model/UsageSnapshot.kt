package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable

/**
 * Information regarding the authenticated account, subscription tier, and organization.
 */
@Serializable
data class AccountInfo(
    val email: String? = null,
    val plan: String? = null,
    val organization: String? = null
) {
    val hasIdentity: Boolean
        get() = !email.isNullOrBlank() || !plan.isNullOrBlank()
}

/**
 * Aggregate snapshot representing an AI provider's usage state at a point in time.
 * Holds rate limit windows, monetary budgets, token metrics, credit balances, and identity info.
 */
@Serializable
data class UsageSnapshot(
    val provider: UsageProvider,
    val primary: RateWindow? = null,
    val secondary: RateWindow? = null,
    val tertiary: RateWindow? = null,
    val extraRateWindows: List<NamedRateWindow> = emptyList(),
    val costSnapshot: ProviderCostSnapshot? = null,
    val tokenUsage: CostUsageTokenSnapshot? = null,
    val credits: CreditsSnapshot? = null,
    val accountInfo: AccountInfo? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val error: String? = null
) {
    val hasRateLimitWindows: Boolean
        get() = primary != null || secondary != null || tertiary != null || extraRateWindows.isNotEmpty()

    val isSuccess: Boolean
        get() = error == null

    /**
     * Backfills missing reset timestamps from a previously cached snapshot.
     */
    fun backfillingResetTimes(cached: UsageSnapshot?, nowEpochMs: Long = System.currentTimeMillis()): UsageSnapshot {
        if (cached == null || cached.provider != this.provider) return this
        return copy(
            primary = primary?.backfillingResetTime(cached.primary, nowEpochMs) ?: cached.primary,
            secondary = secondary?.backfillingResetTime(cached.secondary, nowEpochMs) ?: cached.secondary,
            tertiary = tertiary?.backfillingResetTime(cached.tertiary, nowEpochMs) ?: cached.tertiary
        )
    }

    companion object {
        fun error(
            provider: UsageProvider,
            errorMessage: String,
            cached: UsageSnapshot? = null
        ): UsageSnapshot {
            return UsageSnapshot(
                provider = provider,
                primary = cached?.primary,
                secondary = cached?.secondary,
                tertiary = cached?.tertiary,
                extraRateWindows = cached?.extraRateWindows ?: emptyList(),
                costSnapshot = cached?.costSnapshot,
                tokenUsage = cached?.tokenUsage,
                credits = cached?.credits,
                accountInfo = cached?.accountInfo,
                updatedAtEpochMs = System.currentTimeMillis(),
                error = errorMessage
            )
        }

        fun empty(provider: UsageProvider): UsageSnapshot {
            return UsageSnapshot(provider = provider)
        }
    }
}

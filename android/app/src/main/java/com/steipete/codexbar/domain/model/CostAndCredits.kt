package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Captures monetary spend and budget limits for a provider (e.g. Claude "Extra Usage"
 * or Codex monthly spend limit).
 */
@Serializable
data class ProviderCostSnapshot(
    val used: Double,
    val limit: Double,
    val currencyCode: String = "USD",
    val period: String? = null,
    val resetsAtEpochMs: Long? = null,
    val nextRegenAmount: Double? = null,
    val personalUsed: Double? = null,
    val balance: Double? = null,
    val balanceUpdatedAtEpochMs: Long? = null,
    val balanceIsWorkspace: Boolean? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val usedPercent: Double
        get() = if (limit > 0.0) (used / limit) * 100.0 else 0.0

    val remainingAmount: Double
        get() = maxOf(0.0, limit - used)
}

/**
 * Daily bucket entry for token and cost historical tracking.
 */
@Serializable
data class CostUsageDailyEntry(
    val date: String,
    val totalTokens: Long? = null,
    val costUSD: Double? = null,
    val requestCount: Int? = null
)

/**
 * Token usage and financial metrics across sessions and the rolling 30-day window.
 */
@Serializable
data class CostUsageTokenSnapshot(
    val sessionTokens: Long? = null,
    val sessionCostUSD: Double? = null,
    val sessionRequests: Int? = null,
    val last30DaysTokens: Long? = null,
    val last30DaysCostUSD: Double? = null,
    val last30DaysRequests: Int? = null,
    val currencyCode: String = "USD",
    val historyDays: Int = 30,
    val meteredCostUSD: Double? = null,
    val daily: List<CostUsageDailyEntry> = emptyList(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Individual credit deduction event.
 */
@Serializable
data class CreditEvent(
    val id: String = UUID.randomUUID().toString(),
    val dateEpochMs: Long,
    val service: String,
    val creditsUsed: Double
)

/**
 * Prepaid credit balance tracking (e.g. OpenAI credit grants, OpenRouter, DeepSeek).
 */
@Serializable
data class CreditsSnapshot(
    val remaining: Double,
    val balanceReadSucceeded: Boolean = true,
    val creditsAvailable: Boolean? = null,
    val balanceIsWorkspace: Boolean = false,
    val events: List<CreditEvent> = emptyList(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

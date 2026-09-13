package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a rolling or periodic quota consumption window (e.g. 5-hour rolling session limit,
 * 7-day weekly quota, monthly token allowance).
 *
 * `usedPercent` is unclamped to allow pace analytics to diagnose over-quota states (> 100%).
 * Display renderers clamp to [0.0..100.0].
 */
@Serializable
data class RateWindow(
    val usedPercent: Double,
    val windowMinutes: Int? = null,
    val resetsAtEpochMs: Long? = null,
    val resetDescription: String? = null,
    val isSyntheticPlaceholder: Boolean = false,
    val nextRegenPercent: Double? = null
) {
    /**
     * Quota capacity remaining in percent (clamped to 0..100).
     */
    val remainingPercent: Double
        get() = maxOf(0.0, 100.0 - usedPercent)

    /**
     * Whether this quota window is exhausted or exceeded.
     */
    val isOverQuota: Boolean
        get() = usedPercent >= 100.0

    /**
     * Backfills reset timestamp from a previously cached snapshot if the current window
     * omitted reset metadata (common when providers drop reset times near period ends).
     */
    fun backfillingResetTime(cached: RateWindow?, nowEpochMs: Long = System.currentTimeMillis()): RateWindow {
        if (this.resetsAtEpochMs != null) {
            return this
        }
        val cachedReset = cached?.resetsAtEpochMs ?: return this
        if (cachedReset <= nowEpochMs) {
            return this
        }
        val effectiveMinutes = if (windowMinutes != null && windowMinutes > 0) {
            windowMinutes
        } else {
            cached.windowMinutes
        }
        return copy(
            resetsAtEpochMs = cachedReset,
            windowMinutes = effectiveMinutes,
            resetDescription = resetDescription ?: cached.resetDescription,
            nextRegenPercent = nextRegenPercent ?: cached.nextRegenPercent,
            isSyntheticPlaceholder = this.isSyntheticPlaceholder
        )
    }
}

/**
 * A named secondary or model-scoped quota window (e.g. Claude Opus, Sonnet, GPT-5.3-Spark).
 */
@Serializable
data class NamedRateWindow(
    val id: String,
    val title: String,
    val window: RateWindow,
    val usageKnown: Boolean = true
)

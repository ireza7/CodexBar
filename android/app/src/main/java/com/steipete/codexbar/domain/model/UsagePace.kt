package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Pace classification stages indicating consumption rate relative to time elapsed in quota window.
 */
@Serializable
enum class UsagePaceStage {
    ON_TRACK,
    SLIGHTLY_AHEAD,
    AHEAD,
    FAR_AHEAD,
    SLIGHTLY_BEHIND,
    BEHIND,
    FAR_BEHIND;

    val isAhead: Boolean
        get() = this == SLIGHTLY_AHEAD || this == AHEAD || this == FAR_AHEAD

    val isBehind: Boolean
        get() = this == SLIGHTLY_BEHIND || this == BEHIND || this == FAR_BEHIND

    val isOnTrack: Boolean
        get() = this == ON_TRACK
}

/**
 * Usage pace diagnostic model measuring quota consumption velocity against expected linear progress.
 */
@Serializable
data class UsagePace(
    val stage: UsagePaceStage,
    val targetPercent: Double,
    val actualPercent: Double,
    val paceDeltaPercent: Double,
    val exhaustionEstimateEpochMs: Long? = null,
    val willLastToReset: Boolean = false,
    val speedMultiplierToReset: Double? = null,
    val runOutProbability: Double? = null
) {
    companion object {
        /**
         * Computes pace analysis for a given [RateWindow].
         *
         * @param window The rate window to analyze.
         * @param nowEpochMs Current epoch timestamp in milliseconds.
         * @param defaultWindowMinutes Default window duration in minutes if not specified in window (defaults to 10080 = 7 days).
         * @return Calculated [UsagePace], or null if reset date is absent or expired.
         */
        fun calculate(
            window: RateWindow,
            nowEpochMs: Long = System.currentTimeMillis(),
            defaultWindowMinutes: Int = 10080
        ): UsagePace? {
            val resetsAtEpochMs = window.resetsAtEpochMs ?: return null
            val minutes = window.windowMinutes ?: defaultWindowMinutes
            if (minutes <= 0) return null

            val durationMs = minutes.toLong() * 60_000L
            val timeUntilResetMs = resetsAtEpochMs - nowEpochMs
            if (timeUntilResetMs <= 0 || timeUntilResetMs > durationMs) return null

            val elapsedMs = (durationMs - timeUntilResetMs).coerceIn(0L, durationMs)
            val actual = window.usedPercent.coerceIn(0.0, 100.0)

            // If window just started but shows usage, pace velocity cannot be reliably established yet
            if (elapsedMs == 0L && actual > 0.0) return null

            val expected = ((elapsedMs.toDouble() / durationMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
            val delta = actual - expected
            val stage = stageForDelta(delta)

            var exhaustionEstimateEpochMs: Long? = null
            var willLastToReset = false

            val remainingCapacity = 100.0 - actual
            val projectedRemainingUsage = if (elapsedMs > 0) {
                (actual * timeUntilResetMs.toDouble()) / elapsedMs.toDouble()
            } else {
                0.0
            }

            val speedMultiplierToReset = if (remainingCapacity > 0 && projectedRemainingUsage > 0) {
                val mult = remainingCapacity / projectedRemainingUsage
                if (mult.isFinite()) mult else null
            } else {
                null
            }

            if (actual >= 100.0) {
                exhaustionEstimateEpochMs = nowEpochMs
                willLastToReset = false
            } else if (elapsedMs > 0 && actual > 0.0) {
                val ratePerMs = actual / elapsedMs.toDouble()
                if (ratePerMs > 0.0) {
                    val candidateMs = (remainingCapacity / ratePerMs).toLong()
                    if (candidateMs >= timeUntilResetMs) {
                        willLastToReset = true
                        exhaustionEstimateEpochMs = null
                    } else {
                        willLastToReset = false
                        exhaustionEstimateEpochMs = nowEpochMs + candidateMs
                    }
                }
            } else if (elapsedMs > 0 && actual == 0.0) {
                willLastToReset = true
            }

            return UsagePace(
                stage = stage,
                targetPercent = expected,
                actualPercent = actual,
                paceDeltaPercent = delta,
                exhaustionEstimateEpochMs = exhaustionEstimateEpochMs,
                willLastToReset = willLastToReset,
                speedMultiplierToReset = speedMultiplierToReset,
                runOutProbability = null
            )
        }

        private fun stageForDelta(delta: Double): UsagePaceStage {
            val absDelta = abs(delta)
            return when {
                absDelta <= 2.0 -> UsagePaceStage.ON_TRACK
                absDelta <= 6.0 -> if (delta > 0) UsagePaceStage.SLIGHTLY_AHEAD else UsagePaceStage.SLIGHTLY_BEHIND
                absDelta <= 12.0 -> if (delta > 0) UsagePaceStage.AHEAD else UsagePaceStage.BEHIND
                else -> if (delta > 0) UsagePaceStage.FAR_AHEAD else UsagePaceStage.FAR_BEHIND
            }
        }
    }
}

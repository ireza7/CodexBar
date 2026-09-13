package com.steipete.codexbar.domain

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.domain.model.RateWindow
import com.steipete.codexbar.domain.model.UsagePace
import com.steipete.codexbar.domain.model.UsagePaceStage
import org.junit.Test

class UsagePaceTest {

    @Test
    fun calculate_returnsNullWhenResetTimeMissing() {
        val window = RateWindow(usedPercent = 40.0, resetsAtEpochMs = null)
        val pace = UsagePace.calculate(window)
        assertThat(pace).isNull()
    }

    @Test
    fun calculate_returnsNullWhenTimeUntilResetIsPast() {
        val now = 10000000L
        val window = RateWindow(
            usedPercent = 40.0,
            windowMinutes = 300,
            resetsAtEpochMs = now - 1000L
        )
        val pace = UsagePace.calculate(window, nowEpochMs = now)
        assertThat(pace).isNull()
    }

    @Test
    fun calculate_onTrackPace() {
        val now = 10000000L
        val durationMinutes = 300 // 5 hours = 18,000,000 ms
        val durationMs = durationMinutes * 60 * 1000L
        // 50% of window remaining -> 50% elapsed
        val resetsAt = now + (durationMs / 2)

        val window = RateWindow(
            usedPercent = 50.0,
            windowMinutes = durationMinutes,
            resetsAtEpochMs = resetsAt
        )

        val pace = UsagePace.calculate(window, nowEpochMs = now)
        assertThat(pace).isNotNull()
        assertThat(pace!!.stage).isEqualTo(UsagePaceStage.ON_TRACK)
        assertThat(pace.targetPercent).isWithin(0.1).of(50.0)
        assertThat(pace.actualPercent).isEqualTo(50.0)
        assertThat(pace.paceDeltaPercent).isWithin(0.1).of(0.0)
        assertThat(pace.willLastToReset).isTrue()
    }

    @Test
    fun calculate_aheadOfPace_exhaustionExpectedBeforeReset() {
        val now = 10000000L
        val durationMinutes = 300
        val durationMs = durationMinutes * 60 * 1000L
        // 25% elapsed (75% remaining)
        val timeUntilReset = (durationMs * 0.75).toLong()
        val resetsAt = now + timeUntilReset

        // 60% used at 25% elapsed -> burning much faster than linear 25%
        val window = RateWindow(
            usedPercent = 60.0,
            windowMinutes = durationMinutes,
            resetsAtEpochMs = resetsAt
        )

        val pace = UsagePace.calculate(window, nowEpochMs = now)
        assertThat(pace).isNotNull()
        assertThat(pace!!.stage).isEqualTo(UsagePaceStage.FAR_AHEAD)
        assertThat(pace.stage.isAhead).isTrue()
        assertThat(pace.willLastToReset).isFalse()
        assertThat(pace.exhaustionEstimateEpochMs).isNotNull()
        assertThat(pace.exhaustionEstimateEpochMs!!).isLessThan(resetsAt)
    }

    @Test
    fun calculate_behindPace_surplusExpected() {
        val now = 10000000L
        val durationMinutes = 300
        val durationMs = durationMinutes * 60 * 1000L
        // 75% elapsed (25% remaining)
        val timeUntilReset = (durationMs * 0.25).toLong()
        val resetsAt = now + timeUntilReset

        // 20% used at 75% elapsed -> burning much slower than linear 75%
        val window = RateWindow(
            usedPercent = 20.0,
            windowMinutes = durationMinutes,
            resetsAtEpochMs = resetsAt
        )

        val pace = UsagePace.calculate(window, nowEpochMs = now)
        assertThat(pace).isNotNull()
        assertThat(pace!!.stage).isEqualTo(UsagePaceStage.FAR_BEHIND)
        assertThat(pace.stage.isBehind).isTrue()
        assertThat(pace.willLastToReset).isTrue()
        assertThat(pace.exhaustionEstimateEpochMs).isNull()
    }

    @Test
    fun calculate_exhaustedQuota_reportsImmediateExhaustion() {
        val now = 10000000L
        val window = RateWindow(
            usedPercent = 105.0,
            windowMinutes = 300,
            resetsAtEpochMs = now + 500000L
        )

        val pace = UsagePace.calculate(window, nowEpochMs = now)
        assertThat(pace).isNotNull()
        assertThat(pace!!.actualPercent).isEqualTo(100.0)
        assertThat(pace.exhaustionEstimateEpochMs).isEqualTo(now)
        assertThat(pace.willLastToReset).isFalse()
    }
}

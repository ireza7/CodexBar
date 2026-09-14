package com.steipete.codexbar.domain

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.domain.model.RateWindow
import org.junit.Test

class RateWindowTest {

    @Test
    fun remainingPercent_computesAccurately() {
        val window = RateWindow(usedPercent = 42.5)
        assertThat(window.remainingPercent).isEqualTo(57.5)
    }

    @Test
    fun remainingPercent_clampsToZeroWhenOverQuota() {
        val window = RateWindow(usedPercent = 115.0)
        assertThat(window.remainingPercent).isEqualTo(0.0)
        assertThat(window.isOverQuota).isTrue()
    }

    @Test
    fun backfillingResetTime_preservesCurrentWhenResetPresent() {
        val current = RateWindow(
            usedPercent = 50.0,
            resetsAtEpochMs = 2000000L,
            windowMinutes = 300
        )
        val cached = RateWindow(
            usedPercent = 40.0,
            resetsAtEpochMs = 3000000L,
            windowMinutes = 300
        )

        val result = current.backfillingResetTime(cached, nowEpochMs = 1000000L)
        assertThat(result.resetsAtEpochMs).isEqualTo(2000000L)
    }

    @Test
    fun backfillingResetTime_adoptsValidFutureResetFromCache() {
        val current = RateWindow(
            usedPercent = 55.0,
            resetsAtEpochMs = null,
            windowMinutes = null
        )
        val cached = RateWindow(
            usedPercent = 50.0,
            resetsAtEpochMs = 2000000L,
            windowMinutes = 300,
            resetDescription = "resets in 2h"
        )

        val result = current.backfillingResetTime(cached, nowEpochMs = 1000000L)
        assertThat(result.resetsAtEpochMs).isEqualTo(2000000L)
        assertThat(result.windowMinutes).isEqualTo(300)
        assertThat(result.resetDescription).isEqualTo("resets in 2h")
    }

    @Test
    fun backfillingResetTime_ignoresExpiredCachedReset() {
        val current = RateWindow(
            usedPercent = 55.0,
            resetsAtEpochMs = null
        )
        val expiredCached = RateWindow(
            usedPercent = 50.0,
            resetsAtEpochMs = 500000L
        )

        val result = current.backfillingResetTime(expiredCached, nowEpochMs = 1000000L)
        assertThat(result.resetsAtEpochMs).isNull()
    }

    @Test
    fun backfillingResetTime_preservesSyntheticPlaceholderFlag() {
        val current = RateWindow(
            usedPercent = 0.0,
            resetsAtEpochMs = null,
            isSyntheticPlaceholder = true
        )
        val cached = RateWindow(
            usedPercent = 0.0,
            resetsAtEpochMs = 2000000L,
            isSyntheticPlaceholder = false
        )

        val result = current.backfillingResetTime(cached, nowEpochMs = 1000000L)
        assertThat(result.isSyntheticPlaceholder).isTrue()
    }

    @Test
    fun costSnapshot_remainingPercent_computesAccurately() {
        val cost = com.steipete.codexbar.domain.model.ProviderCostSnapshot(
            used = 25.0,
            limit = 100.0
        )
        assertThat(cost.remainingPercent).isEqualTo(75.0)
    }

    @Test
    fun costSnapshot_remainingPercent_clampsToZeroWhenExceeded() {
        val cost = com.steipete.codexbar.domain.model.ProviderCostSnapshot(
            used = 120.0,
            limit = 100.0
        )
        assertThat(cost.remainingPercent).isEqualTo(0.0)
    }
}

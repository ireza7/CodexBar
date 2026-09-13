package com.steipete.codexbar.data

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.data.remote.MockProviderFetcher
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MockProviderFetcherTest {

    @Test
    fun claudeMock_returnsValidSessionAndWeeklyWindows() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.CLAUDE)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.CLAUDE)
        assertThat(snapshot.primary).isNotNull()
        assertThat(snapshot.primary!!.usedPercent).isWithin(0.1).of(38.5)
        assertThat(snapshot.primary!!.windowMinutes).isEqualTo(300)
        assertThat(snapshot.secondary).isNotNull()
        assertThat(snapshot.secondary!!.usedPercent).isWithin(0.1).of(64.0)
        assertThat(snapshot.costSnapshot).isNotNull()
        assertThat(snapshot.costSnapshot!!.limit).isEqualTo(50.00)
        assertThat(snapshot.accountInfo?.plan).isEqualTo("Claude Pro")
    }

    @Test
    fun openAIMock_returnsCreditGrantsAndTokenUsage() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.OPENAI)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.OPENAI)
        assertThat(snapshot.credits).isNotNull()
        assertThat(snapshot.credits!!.remaining).isEqualTo(18.50)
        assertThat(snapshot.costSnapshot?.balance).isEqualTo(18.50)
        assertThat(snapshot.tokenUsage).isNotNull()
        assertThat(snapshot.tokenUsage!!.last30DaysTokens).isEqualTo(1250000L)
    }

    @Test
    fun cursorMock_returnsFastRequestsAndSandUsage() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.CURSOR)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.CURSOR)
        assertThat(snapshot.primary).isNotNull()
        assertThat(snapshot.primary!!.usedPercent).isEqualTo(29.0)
        assertThat(snapshot.extraRateWindows).hasSize(1)
        assertThat(snapshot.extraRateWindows.first().id).isEqualTo("sand_usage")
    }

    @Test
    fun copilotMock_returnsPremiumAndChatQuotas() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.COPILOT)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.COPILOT)
        assertThat(snapshot.primary).isNotNull()
        assertThat(snapshot.primary!!.usedPercent).isEqualTo(18.0)
        assertThat(snapshot.secondary).isNotNull()
        assertThat(snapshot.secondary!!.usedPercent).isEqualTo(5.0)
    }

    @Test
    fun geminiMock_returnsProAndFlashModels() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.GEMINI)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.GEMINI)
        assertThat(snapshot.primary).isNotNull()
        assertThat(snapshot.secondary).isNotNull()
        assertThat(snapshot.accountInfo?.plan).contains("Gemini")
    }

    @Test
    fun codexMock_returnsChatGPTWindowsAndResetCredits() = runTest {
        val fetcher = MockProviderFetcher(UsageProvider.CODEX)
        val result = fetcher.fetchUsage(ProviderCredentials())

        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrThrow()

        assertThat(snapshot.provider).isEqualTo(UsageProvider.CODEX)
        assertThat(snapshot.primary).isNotNull()
        assertThat(snapshot.primary!!.usedPercent).isEqualTo(52.0)
        assertThat(snapshot.secondary).isNotNull()
        assertThat(snapshot.secondary!!.usedPercent).isEqualTo(71.0)
        assertThat(snapshot.credits?.remaining).isEqualTo(1.0)
    }
}

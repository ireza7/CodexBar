package com.steipete.codexbar.presentation

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.data.FakeSharedPreferences
import com.steipete.codexbar.data.local.InMemorySecureStorage
import com.steipete.codexbar.data.local.SettingsRepositoryImpl
import com.steipete.codexbar.data.repository.UsageRepositoryImpl
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import com.steipete.codexbar.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [DashboardViewModel] verifying state updates, provider selection,
 * refresh actions, and strict provider data siloing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var secureStorage: InMemorySecureStorage
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var usageRepository: UsageRepositoryImpl
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() = runTest(testDispatcher) {
        secureStorage = InMemorySecureStorage()
        settingsRepository = SettingsRepositoryImpl(FakeSharedPreferences(), testDispatcher)
        usageRepository = UsageRepositoryImpl(UsageRepositoryImpl.mockFetchers())

        // Seed initial active providers: CLAUDE, OPENAI, CURSOR
        settingsRepository.updateProviderEnabled(UsageProvider.CLAUDE, true)
        settingsRepository.updateProviderEnabled(UsageProvider.OPENAI, true)
        settingsRepository.updateProviderEnabled(UsageProvider.CURSOR, true)

        viewModel = DashboardViewModel(
            usageRepository = usageRepository,
            settingsRepository = settingsRepository,
            secureStorage = secureStorage,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testInitialStateObservesActiveProviders() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.activeProviders).contains(UsageProvider.CLAUDE)
        assertThat(state.activeProviders).contains(UsageProvider.OPENAI)
        assertThat(state.selectedProvider).isNotNull()
        assertThat(state.selectedProvider).isEqualTo(state.activeProviders.first())
    }

    @Test
    fun testProviderSelectionChangesSelectedProvider() = runTest(testDispatcher) {
        viewModel.selectProvider(UsageProvider.OPENAI)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(UsageProvider.OPENAI)

        viewModel.selectProvider(UsageProvider.CURSOR)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(UsageProvider.CURSOR)
    }

    @Test
    fun testSelectingInactiveProviderIsIgnored() = runTest(testDispatcher) {
        val initialSelected = viewModel.uiState.value.selectedProvider
        viewModel.selectProvider(UsageProvider.SYNTHETIC)
        // SYNTHETIC is not in activeProviders, selection should remain unchanged
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(initialSelected)
    }

    @Test
    fun testStrictProviderDataSiloing() = runTest(testDispatcher) {
        // Fetch snapshots for both Claude and OpenAI
        secureStorage.saveApiKey(UsageProvider.CLAUDE, "test-claude-key")
        secureStorage.saveApiKey(UsageProvider.OPENAI, "test-openai-key")

        usageRepository.refreshUsage(UsageProvider.CLAUDE, secureStorage.getCredentials(UsageProvider.CLAUDE))
        usageRepository.refreshUsage(UsageProvider.OPENAI, secureStorage.getCredentials(UsageProvider.OPENAI))

        // 1. Inspect Claude silo
        viewModel.selectProvider(UsageProvider.CLAUDE)
        val claudeState = viewModel.uiState.value
        assertThat(claudeState.selectedProvider).isEqualTo(UsageProvider.CLAUDE)
        val claudeSnapshot = claudeState.selectedSnapshot
        assertThat(claudeSnapshot).isNotNull()
        assertThat(claudeSnapshot!!.provider).isEqualTo(UsageProvider.CLAUDE)
        assertThat(claudeSnapshot.accountInfo?.email).isEqualTo("developer@anthropic-user.com")
        assertThat(claudeSnapshot.accountInfo?.plan).isEqualTo("Claude Pro")

        // 2. Inspect OpenAI silo
        viewModel.selectProvider(UsageProvider.OPENAI)
        val openaiState = viewModel.uiState.value
        assertThat(openaiState.selectedProvider).isEqualTo(UsageProvider.OPENAI)
        val openaiSnapshot = openaiState.selectedSnapshot
        assertThat(openaiSnapshot).isNotNull()
        assertThat(openaiSnapshot!!.provider).isEqualTo(UsageProvider.OPENAI)
        assertThat(openaiSnapshot.accountInfo?.email).isEqualTo("team@openai-dev.com")
        assertThat(openaiSnapshot.accountInfo?.plan).isEqualTo("Usage Tier 4")

        // 3. Confirm zero cross-leakage: Claude info is absent from OpenAI snapshot
        assertThat(openaiSnapshot.accountInfo?.email).isNotEqualTo(claudeSnapshot.accountInfo?.email)
        assertThat(openaiSnapshot.accountInfo?.plan).isNotEqualTo(claudeSnapshot.accountInfo?.plan)
    }

    @Test
    fun testRefreshSelectedProviderUpdatesSnapshot() = runTest(testDispatcher) {
        viewModel.selectProvider(UsageProvider.CLAUDE)
        secureStorage.saveApiKey(UsageProvider.CLAUDE, "key-claude")

        viewModel.refreshSelected()

        val state = viewModel.uiState.value
        assertThat(state.isRefreshing).isFalse()
        val snapshot = state.usageSnapshots[UsageProvider.CLAUDE]
        assertThat(snapshot).isNotNull()
        assertThat(snapshot!!.provider).isEqualTo(UsageProvider.CLAUDE)
        assertThat(snapshot.primary).isNotNull()
    }

    @Test
    fun testRefreshAllProvidersUpdatesActiveSnapshots() = runTest(testDispatcher) {
        secureStorage.saveApiKey(UsageProvider.CLAUDE, "key-claude")
        secureStorage.saveApiKey(UsageProvider.OPENAI, "key-openai")

        viewModel.refreshAll()

        val state = viewModel.uiState.value
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.usageSnapshots).containsKey(UsageProvider.CLAUDE)
        assertThat(state.usageSnapshots).containsKey(UsageProvider.OPENAI)
    }

    @Test
    fun testActiveProvidersChangedInSettingsUpdatesSelection() = runTest(testDispatcher) {
        viewModel.selectProvider(UsageProvider.CLAUDE)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(UsageProvider.CLAUDE)

        // Disable CLAUDE from active providers
        settingsRepository.updateProviderEnabled(UsageProvider.CLAUDE, false)

        val updatedState = viewModel.uiState.value
        assertThat(updatedState.activeProviders).doesNotContain(UsageProvider.CLAUDE)
        // Selection must automatically fall back to the next active provider
        assertThat(updatedState.selectedProvider).isNotEqualTo(UsageProvider.CLAUDE)
        assertThat(updatedState.activeProviders).contains(updatedState.selectedProvider)
    }

    @Test
    fun testClearGlobalErrorResetsError() = runTest(testDispatcher) {
        viewModel.clearGlobalError()
        assertThat(viewModel.uiState.value.globalError).isNull()
    }
}

package com.steipete.codexbar.presentation

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.data.FakeSharedPreferences
import com.steipete.codexbar.data.local.InMemorySecureStorage
import com.steipete.codexbar.data.local.SettingsRepositoryImpl
import com.steipete.codexbar.data.repository.UsageRepositoryImpl
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

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
        usageRepository = UsageRepositoryImpl()

        settingsRepository.updateProviderEnabled(UsageProvider.ANTIGRAVITY, true)

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
        assertThat(state.activeProviders).contains(UsageProvider.ANTIGRAVITY)
        assertThat(state.selectedProvider).isEqualTo(UsageProvider.ANTIGRAVITY)
    }

    @Test
    fun testProviderSelectionChangesSelectedProvider() = runTest(testDispatcher) {
        viewModel.selectProvider(UsageProvider.ANTIGRAVITY)
        assertThat(viewModel.uiState.value.selectedProvider).isEqualTo(UsageProvider.ANTIGRAVITY)
    }
}

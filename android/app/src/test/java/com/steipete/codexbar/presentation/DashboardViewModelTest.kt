package com.steipete.codexbar.presentation

import com.google.truth.Truth.assertThat
import com.steipete.codexbar.data.FakeSharedPreferences
import com.steipete.codexbar.data.local.InMemorySecureStorage
import com.steipete.codexbar.data.local.SettingsRepositoryImpl
import com.steipete.codexbar.data.repository.UsageRepositoryImpl
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
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

    @Test
    fun testMultiAccountManagementInViewModel() = runTest(testDispatcher) {
        val fakeRepo = object : com.steipete.codexbar.domain.repository.AccountRepository {
            val list = kotlinx.coroutines.flow.MutableStateFlow<List<com.steipete.codexbar.domain.model.AntigravityAccount>>(emptyList())
            override fun getAccountsFlow() = list
            override fun getActiveAccountFlow() = list.map { it.find { a -> a.isActive } }
            override suspend fun getAccounts() = list.value
            override suspend fun getActiveAccount() = list.value.find { it.isActive }
            override suspend fun addAccount(label: String, token: String, email: String?): com.steipete.codexbar.domain.model.AntigravityAccount {
                val acc = com.steipete.codexbar.domain.model.AntigravityAccount(label = label, token = token, email = email, isActive = list.value.isEmpty())
                list.value = list.value + acc
                return acc
            }
            override suspend fun switchActiveAccount(id: String): Boolean {
                list.value = list.value.map { it.copy(isActive = it.id == id) }
                return true
            }
            override suspend fun updateAccount(account: com.steipete.codexbar.domain.model.AntigravityAccount) {
                list.value = list.value.map { if (it.id == account.id) account else it }
            }
            override suspend fun deleteAccount(id: String): Boolean {
                list.value = list.value.filterNot { it.id == id }
                return true
            }
            override suspend fun updateActiveAccountQuotas(gemini5hPct: Int, geminiWeeklyPct: Int, claude5hPct: Int, claudeWeeklyPct: Int, geminiReset: Long, claudeReset: Long, plan: String?, email: String?) {}
        }

        val vm = DashboardViewModel(
            usageRepository = usageRepository,
            settingsRepository = settingsRepository,
            secureStorage = secureStorage,
            accountRepository = fakeRepo,
            ioDispatcher = testDispatcher
        )

        vm.addAccount("Work", "token-work", "work@company.com")
        vm.addAccount("Personal", "token-pers", "pers@gmail.com")

        val state = vm.uiState.value
        assertThat(state.accounts).hasSize(2)
        assertThat(state.activeAccount?.label).isEqualTo("Work")

        val secondId = state.accounts[1].id
        vm.switchAccount(secondId)
        assertThat(vm.uiState.value.activeAccount?.label).isEqualTo("Personal")
    }
}

package com.steipete.codexbar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.domain.repository.SettingsRepository
import com.steipete.codexbar.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing UI state and user interactions for the CodexBar Dashboard.
 * Strictly guarantees provider data siloing by isolating credentials and snapshots per provider.
 */
class DashboardViewModel(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    private val secureStorage: SecureStorage,
    private val accountRepository: com.steipete.codexbar.domain.repository.AccountRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeUsageSnapshots()
        observeAccounts()
    }

    private fun observeAccounts() {
        if (accountRepository == null) return
        viewModelScope.launch {
            accountRepository.getAccountsFlow().collect { accountsList ->
                val active = accountsList.find { it.isActive } ?: accountsList.firstOrNull()
                _uiState.update { current ->
                    current.copy(
                        accounts = accountsList,
                        activeAccount = active
                    )
                }
            }
        }
    }

    /**
     * Listens for changes to user settings (e.g. active providers list) and updates UI state accordingly.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettingsFlow().collect { userSettings ->
                _uiState.update { current ->
                    val active = userSettings.activeProviders
                    val currentSelected = current.selectedProvider
                    val updatedSelected = when {
                        currentSelected != null && active.contains(currentSelected) -> currentSelected
                        else -> active.firstOrNull()
                    }
                    current.copy(
                        activeProviders = active,
                        selectedProvider = updatedSelected,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Listens for usage cache emissions from the repository and updates UI state.
     */
    private fun observeUsageSnapshots() {
        viewModelScope.launch {
            usageRepository.getAllUsageFlow().collect { snapshots ->
                _uiState.update { current ->
                    current.copy(usageSnapshots = snapshots)
                }
            }
        }
    }

    /**
     * Switches the currently inspected provider on the dashboard.
     */
    fun selectProvider(provider: UsageProvider) {
        _uiState.update { current ->
            if (current.activeProviders.contains(provider)) {
                current.copy(selectedProvider = provider)
            } else {
                current
            }
        }
    }

    /**
     * Triggers a remote probe for the currently selected provider using its siloed credentials.
     */
    fun refreshSelected() {
        val provider = _uiState.value.selectedProvider ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, globalError = null) }
            try {
                val credentials = withContext(ioDispatcher) {
                    secureStorage.getCredentials(provider)
                }
                usageRepository.refreshUsage(provider, credentials)
            } catch (e: Exception) {
                _uiState.update { it.copy(globalError = e.message ?: "Failed to refresh ${provider.displayName}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Refreshes all currently active providers in parallel using their respective siloed credentials.
     */
    fun refreshAll() {
        val providers = _uiState.value.activeProviders
        if (providers.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, globalError = null) }
            try {
                val credentialsMap = withContext(ioDispatcher) {
                    providers.associateWith { secureStorage.getCredentials(it) }
                }
                usageRepository.refreshAll(credentialsMap)
            } catch (e: Exception) {
                _uiState.update { it.copy(globalError = e.message ?: "Failed to refresh providers") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Switches the active Antigravity account and triggers an immediate usage refresh.
     */
    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            val switched = accountRepository?.switchActiveAccount(accountId) ?: false
            if (switched) {
                refreshSelected()
            }
        }
    }

    /**
     * Adds a new Antigravity account, switches to it, and refreshes usage.
     */
    fun addAccount(label: String, token: String, email: String? = null) {
        viewModelScope.launch {
            val newAcc = accountRepository?.addAccount(label, token, email)
            if (newAcc != null) {
                accountRepository.switchActiveAccount(newAcc.id)
                refreshSelected()
            }
        }
    }

    /**
     * Deletes a saved account.
     */
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository?.deleteAccount(accountId)
            refreshSelected()
        }
    }

    /**
     * Clears any active top-level error message.
     */
    fun clearGlobalError() {
        _uiState.update { it.copy(globalError = null) }
    }

    /**
     * Factory for creating [DashboardViewModel] with injected dependencies.
     */
    class Factory(
        private val usageRepository: UsageRepository,
        private val settingsRepository: SettingsRepository,
        private val secureStorage: SecureStorage,
        private val accountRepository: com.steipete.codexbar.domain.repository.AccountRepository? = null,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                usageRepository = usageRepository,
                settingsRepository = settingsRepository,
                secureStorage = secureStorage,
                accountRepository = accountRepository,
                ioDispatcher = ioDispatcher
            ) as T
        }
    }
}

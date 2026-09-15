package com.steipete.codexbar.data.local

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.steipete.codexbar.domain.model.AntigravityAccount
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.repository.AccountRepository
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.presentation.widget.AntigravityQuotaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AccountRepositoryImpl(
    private val context: Context,
    private val secureStorage: SecureStorage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AccountRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val accountsFlow = MutableStateFlow<List<AntigravityAccount>>(emptyList())

    init {
        loadInitialAccounts()
    }

    private fun loadInitialAccounts() {
        val rawJson = prefs.getString(KEY_ACCOUNTS_JSON, null)
        val loaded = if (!rawJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<AntigravityAccount>>(rawJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        if (loaded.isNotEmpty()) {
            accountsFlow.value = loaded
        } else {
            // Check if there is an existing legacy token to migrate
            scope.launch {
                val legacyToken = secureStorage.getApiKey(UsageProvider.ANTIGRAVITY)
                if (!legacyToken.isNullOrBlank()) {
                    val initialAccount = AntigravityAccount(
                        label = "Account 1",
                        token = legacyToken,
                        isActive = true
                    )
                    val list = listOf(initialAccount)
                    saveAccountsInternal(list)
                    accountsFlow.value = list
                }
            }
        }
    }

    override fun getAccountsFlow(): Flow<List<AntigravityAccount>> = accountsFlow.asStateFlow()

    override fun getActiveAccountFlow(): Flow<AntigravityAccount?> =
        accountsFlow.map { list -> list.find { it.isActive } ?: list.firstOrNull() }

    override suspend fun getAccounts(): List<AntigravityAccount> = accountsFlow.value

    override suspend fun getActiveAccount(): AntigravityAccount? =
        accountsFlow.value.find { it.isActive } ?: accountsFlow.value.firstOrNull()

    override suspend fun addAccount(
        label: String,
        token: String,
        email: String?
    ): AntigravityAccount {
        val current = accountsFlow.value
        val isFirst = current.isEmpty()
        val cleanLabel = label.ifBlank { "Account ${current.size + 1}" }
        val newAccount = AntigravityAccount(
            label = cleanLabel,
            token = token.trim(),
            email = email?.trim(),
            isActive = isFirst
        )
        val updated = current + newAccount
        saveAccountsInternal(updated)
        accountsFlow.value = updated

        if (isFirst) {
            secureStorage.saveApiKey(UsageProvider.ANTIGRAVITY, newAccount.token)
            updateWidgetActiveAccount(newAccount)
        }
        return newAccount
    }

    override suspend fun switchActiveAccount(id: String): Boolean {
        val current = accountsFlow.value
        val target = current.find { it.id == id } ?: return false
        val updated = current.map {
            it.copy(isActive = (it.id == id))
        }
        saveAccountsInternal(updated)
        accountsFlow.value = updated

        secureStorage.saveApiKey(UsageProvider.ANTIGRAVITY, target.token)
        updateWidgetActiveAccount(target)
        return true
    }

    override suspend fun updateAccount(account: AntigravityAccount) {
        val current = accountsFlow.value
        val updated = current.map { if (it.id == account.id) account else it }
        saveAccountsInternal(updated)
        accountsFlow.value = updated
        if (account.isActive) {
            secureStorage.saveApiKey(UsageProvider.ANTIGRAVITY, account.token)
            updateWidgetActiveAccount(account)
        }
    }

    override suspend fun deleteAccount(id: String): Boolean {
        val current = accountsFlow.value
        val accountToDelete = current.find { it.id == id } ?: return false
        val remaining = current.filterNot { it.id == id }

        val finalAccounts = if (accountToDelete.isActive && remaining.isNotEmpty()) {
            val newActive = remaining.first().copy(isActive = true)
            listOf(newActive) + remaining.drop(1)
        } else {
            remaining
        }

        saveAccountsInternal(finalAccounts)
        accountsFlow.value = finalAccounts

        val newActive = finalAccounts.find { it.isActive }
        if (newActive != null) {
            secureStorage.saveApiKey(UsageProvider.ANTIGRAVITY, newActive.token)
            updateWidgetActiveAccount(newActive)
        } else {
            secureStorage.removeApiKey(UsageProvider.ANTIGRAVITY)
        }
        return true
    }

    override suspend fun updateActiveAccountQuotas(
        gemini5hPct: Int,
        geminiWeeklyPct: Int,
        claude5hPct: Int,
        claudeWeeklyPct: Int,
        geminiReset: Long,
        claudeReset: Long,
        plan: String?,
        email: String?
    ) {
        val current = accountsFlow.value
        val active = current.find { it.isActive } ?: current.firstOrNull() ?: return
        val updatedActive = active.copy(
            gemini5hPct = gemini5hPct,
            geminiWeeklyPct = geminiWeeklyPct,
            claude5hPct = claude5hPct,
            claudeWeeklyPct = claudeWeeklyPct,
            gemini5hResetEpoch = geminiReset,
            claude5hResetEpoch = claudeReset,
            plan = plan ?: active.plan,
            email = email ?: active.email,
            lastSyncEpochMs = System.currentTimeMillis()
        )
        val updatedList = current.map { if (it.id == updatedActive.id) updatedActive else it }
        saveAccountsInternal(updatedList)
        accountsFlow.value = updatedList
        updateWidgetActiveAccount(updatedActive)
    }

    private fun saveAccountsInternal(accounts: List<AntigravityAccount>) {
        try {
            val raw = json.encodeToString(accounts)
            prefs.edit().putString(KEY_ACCOUNTS_JSON, raw).apply()
        } catch (_: Exception) {}
    }

    private fun updateWidgetActiveAccount(account: AntigravityAccount) {
        try {
            val widgetPrefs = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)
            widgetPrefs.edit()
                .putString("active_account_label", account.label)
                .putString("active_account_email", account.email ?: "")
                .apply()

            scope.launch {
                try {
                    AntigravityQuotaWidget().updateAll(context)
                } catch (_: Throwable) {}
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val PREFS_NAME = "antigravity_accounts_prefs"
        private const val KEY_ACCOUNTS_JSON = "saved_accounts_json"
    }
}

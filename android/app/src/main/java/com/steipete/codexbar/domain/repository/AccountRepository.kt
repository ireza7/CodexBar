package com.steipete.codexbar.domain.repository

import com.steipete.codexbar.domain.model.AntigravityAccount
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining contract for managing multiple Google / Antigravity accounts.
 */
interface AccountRepository {
    fun getAccountsFlow(): Flow<List<AntigravityAccount>>
    fun getActiveAccountFlow(): Flow<AntigravityAccount?>
    suspend fun getAccounts(): List<AntigravityAccount>
    suspend fun getActiveAccount(): AntigravityAccount?
    suspend fun addAccount(label: String, token: String, email: String? = null): AntigravityAccount
    suspend fun switchActiveAccount(id: String): Boolean
    suspend fun updateAccount(account: AntigravityAccount)
    suspend fun deleteAccount(id: String): Boolean
    suspend fun updateActiveAccountQuotas(
        gemini5hPct: Int,
        geminiWeeklyPct: Int,
        claude5hPct: Int,
        claudeWeeklyPct: Int,
        geminiReset: Long,
        claudeReset: Long,
        plan: String? = null,
        email: String? = null
    )
}

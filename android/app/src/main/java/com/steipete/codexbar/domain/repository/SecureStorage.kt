package com.steipete.codexbar.domain.repository

import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider

/**
 * Interface defining contract for hardware-backed secure storage of sensitive credentials
 * (API keys, session tokens, organization IDs).
 */
interface SecureStorage {
    /**
     * Securely stores an API key for the specified [provider].
     */
    suspend fun saveApiKey(provider: UsageProvider, key: String)

    /**
     * Retrieves the stored API key for the specified [provider], or null if not set.
     */
    suspend fun getApiKey(provider: UsageProvider): String?

    /**
     * Removes the stored API key for the specified [provider].
     */
    suspend fun removeApiKey(provider: UsageProvider)

    /**
     * Securely stores a session token / cookie for the specified [provider].
     */
    suspend fun saveSessionToken(provider: UsageProvider, token: String)

    /**
     * Retrieves the stored session token for the specified [provider], or null if not set.
     */
    suspend fun getSessionToken(provider: UsageProvider): String?

    /**
     * Removes the stored session token for the specified [provider].
     */
    suspend fun removeSessionToken(provider: UsageProvider)

    /**
     * Securely stores an organization ID for the specified [provider].
     */
    suspend fun saveOrgId(provider: UsageProvider, orgId: String)

    /**
     * Retrieves the stored organization ID for the specified [provider], or null if not set.
     */
    suspend fun getOrgId(provider: UsageProvider): String?

    /**
     * Removes the stored organization ID for the specified [provider].
     */
    suspend fun removeOrgId(provider: UsageProvider)

    /**
     * Securely stores all non-null credential fields in [credentials] for the specified [provider].
     */
    suspend fun saveCredentials(provider: UsageProvider, credentials: ProviderCredentials)

    /**
     * Retrieves all stored credentials for the specified [provider] as a [ProviderCredentials] object.
     */
    suspend fun getCredentials(provider: UsageProvider): ProviderCredentials

    /**
     * Removes all stored credentials (API key, session token, org ID) for the specified [provider].
     */
    suspend fun removeCredentials(provider: UsageProvider)

    /**
     * Stores an arbitrary key-value string securely.
     */
    suspend fun putString(key: String, value: String?)

    /**
     * Retrieves an arbitrary securely stored string by [key], or null if not present.
     */
    suspend fun getString(key: String): String?

    /**
     * Removes a stored value by [key].
     */
    suspend fun remove(key: String)

    /**
     * Checks if a value exists for [key].
     */
    suspend fun contains(key: String): Boolean

    /**
     * Clears all credentials and data from secure storage.
     */
    suspend fun clear()
}

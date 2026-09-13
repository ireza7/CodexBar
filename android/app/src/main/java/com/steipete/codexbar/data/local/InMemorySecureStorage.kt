package com.steipete.codexbar.data.local

import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.repository.SecureStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory implementation of [SecureStorage] using [ConcurrentHashMap].
 *
 * Designed for pure JVM unit tests, preview mock environments, and fallback scenarios
 * without requiring Android KeyStore or native libraries.
 */
class InMemorySecureStorage : SecureStorage {

    private val storage = ConcurrentHashMap<String, String>()

    override suspend fun saveApiKey(provider: UsageProvider, key: String) {
        putString(apiKeyKey(provider), key.trim())
    }

    override suspend fun getApiKey(provider: UsageProvider): String? {
        return getString(apiKeyKey(provider))
    }

    override suspend fun removeApiKey(provider: UsageProvider) {
        remove(apiKeyKey(provider))
    }

    override suspend fun saveSessionToken(provider: UsageProvider, token: String) {
        putString(sessionTokenKey(provider), token.trim())
    }

    override suspend fun getSessionToken(provider: UsageProvider): String? {
        return getString(sessionTokenKey(provider))
    }

    override suspend fun removeSessionToken(provider: UsageProvider) {
        remove(sessionTokenKey(provider))
    }

    override suspend fun saveOrgId(provider: UsageProvider, orgId: String) {
        putString(orgIdKey(provider), orgId.trim())
    }

    override suspend fun getOrgId(provider: UsageProvider): String? {
        return getString(orgIdKey(provider))
    }

    override suspend fun removeOrgId(provider: UsageProvider) {
        remove(orgIdKey(provider))
    }

    override suspend fun saveCredentials(provider: UsageProvider, credentials: ProviderCredentials) {
        credentials.apiKey?.let { saveApiKey(provider, it) } ?: removeApiKey(provider)
        credentials.sessionToken?.let { saveSessionToken(provider, it) } ?: removeSessionToken(provider)
        credentials.orgId?.let { saveOrgId(provider, it) } ?: removeOrgId(provider)
    }

    override suspend fun getCredentials(provider: UsageProvider): ProviderCredentials {
        return ProviderCredentials(
            apiKey = getApiKey(provider),
            sessionToken = getSessionToken(provider),
            orgId = getOrgId(provider)
        )
    }

    override suspend fun removeCredentials(provider: UsageProvider) {
        removeApiKey(provider)
        removeSessionToken(provider)
        removeOrgId(provider)
    }

    override suspend fun putString(key: String, value: String?) {
        if (value != null) {
            storage[key] = value
        } else {
            storage.remove(key)
        }
    }

    override suspend fun getString(key: String): String? {
        return storage[key]
    }

    override suspend fun remove(key: String) {
        storage.remove(key)
    }

    override suspend fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }

    override suspend fun clear() {
        storage.clear()
    }

    /**
     * Returns an immutable copy of the current in-memory key-value snapshot for verification.
     */
    fun snapshot(): Map<String, String> = HashMap(storage)

    companion object {
        fun apiKeyKey(provider: UsageProvider): String = "api_key_${provider.name.lowercase()}"
        fun sessionTokenKey(provider: UsageProvider): String = "session_token_${provider.name.lowercase()}"
        fun orgIdKey(provider: UsageProvider): String = "org_id_${provider.name.lowercase()}"
    }
}

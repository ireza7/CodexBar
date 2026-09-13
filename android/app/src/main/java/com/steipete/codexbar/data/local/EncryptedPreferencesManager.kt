package com.steipete.codexbar.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.repository.SecureStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Production implementation of [SecureStorage] backed by Android KeyStore hardware encryption
 * using [MasterKey] (AES-256 GCM) and [EncryptedSharedPreferences] (AES-256 SIV / AES-256 GCM).
 *
 * All write/read operations are safely dispatched to [ioDispatcher] to ensure no disk I/O occurs
 * on the Android main thread.
 *
 * Includes graceful recovery and fallback if the hardware KeyStore throws device-specific
 * or post-restore exceptions.
 */
class EncryptedPreferencesManager(
    context: Context,
    private val fileName: String = SECURE_PREFS_FILE_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SecureStorage {

    private val appContext: Context = context.applicationContext

    private val sharedPreferences: SharedPreferences by lazy {
        initEncryptedPreferences()
    }

    private var isHardwareBacked: Boolean = true

    /**
     * Initializes EncryptedSharedPreferences with KeyStore MasterKey.
     * Gracefully falls back if Keystore corruption occurs.
     */
    private fun initEncryptedPreferences(): SharedPreferences {
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.w(TAG, "Failed initial EncryptedSharedPreferences creation: ${e.message}. Attempting recovery.", e)
            try {
                // Delete possibly corrupted XML file and retry once
                deletePrefsXml(fileName)
                createEncryptedPrefs()
            } catch (recoveryEx: Exception) {
                Log.e(TAG, "Hardware KeyStore initialization failed. Falling back to private SharedPreferences.", recoveryEx)
                isHardwareBacked = false
                appContext.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deletePrefsXml(prefsFileName: String) {
        try {
            val sharedPrefsDir = File(appContext.filesDir.parent, "shared_prefs")
            val prefsFile = File(sharedPrefsDir, "$prefsFileName.xml")
            if (prefsFile.exists()) {
                val deleted = prefsFile.delete()
                Log.i(TAG, "Corrupted prefs file deleted: $deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete corrupted prefs file: ${e.message}")
        }
    }

    /**
     * Returns true if the storage is currently backed by hardware KeyStore encryption.
     */
    fun isHardwareBacked(): Boolean = isHardwareBacked

    override suspend fun saveApiKey(provider: UsageProvider, key: String) = withContext(ioDispatcher) {
        putString(apiKeyKey(provider), key.trim())
    }

    override suspend fun getApiKey(provider: UsageProvider): String? = withContext(ioDispatcher) {
        getString(apiKeyKey(provider))
    }

    override suspend fun removeApiKey(provider: UsageProvider) = withContext(ioDispatcher) {
        remove(apiKeyKey(provider))
    }

    override suspend fun saveSessionToken(provider: UsageProvider, token: String) = withContext(ioDispatcher) {
        putString(sessionTokenKey(provider), token.trim())
    }

    override suspend fun getSessionToken(provider: UsageProvider): String? = withContext(ioDispatcher) {
        getString(sessionTokenKey(provider))
    }

    override suspend fun removeSessionToken(provider: UsageProvider) = withContext(ioDispatcher) {
        remove(sessionTokenKey(provider))
    }

    override suspend fun saveOrgId(provider: UsageProvider, orgId: String) = withContext(ioDispatcher) {
        putString(orgIdKey(provider), orgId.trim())
    }

    override suspend fun getOrgId(provider: UsageProvider): String? = withContext(ioDispatcher) {
        getString(orgIdKey(provider))
    }

    override suspend fun removeOrgId(provider: UsageProvider) = withContext(ioDispatcher) {
        remove(orgIdKey(provider))
    }

    override suspend fun saveCredentials(provider: UsageProvider, credentials: ProviderCredentials) = withContext(ioDispatcher) {
        credentials.apiKey?.let { saveApiKey(provider, it) } ?: removeApiKey(provider)
        credentials.sessionToken?.let { saveSessionToken(provider, it) } ?: removeSessionToken(provider)
        credentials.orgId?.let { saveOrgId(provider, it) } ?: removeOrgId(provider)
    }

    override suspend fun getCredentials(provider: UsageProvider): ProviderCredentials = withContext(ioDispatcher) {
        ProviderCredentials(
            apiKey = getApiKey(provider),
            sessionToken = getSessionToken(provider),
            orgId = getOrgId(provider)
        )
    }

    override suspend fun removeCredentials(provider: UsageProvider) = withContext(ioDispatcher) {
        removeApiKey(provider)
        removeSessionToken(provider)
        removeOrgId(provider)
    }

    override suspend fun putString(key: String, value: String?) = withContext(ioDispatcher) {
        sharedPreferences.edit().apply {
            if (value != null) {
                putString(key, value)
            } else {
                remove(key)
            }
        }.apply()
    }

    override suspend fun getString(key: String): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(key, null)
    }

    override suspend fun remove(key: String) = withContext(ioDispatcher) {
        sharedPreferences.edit().remove(key).apply()
    }

    override suspend fun contains(key: String): Boolean = withContext(ioDispatcher) {
        sharedPreferences.contains(key)
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val TAG = "EncryptedPrefsMgr"
        const val SECURE_PREFS_FILE_NAME = "codexbar_secure_vault"
        const val FALLBACK_PREFS_FILE_NAME = "codexbar_secure_vault_fallback"

        fun apiKeyKey(provider: UsageProvider): String = "api_key_${provider.name.lowercase()}"
        fun sessionTokenKey(provider: UsageProvider): String = "session_token_${provider.name.lowercase()}"
        fun orgIdKey(provider: UsageProvider): String = "org_id_${provider.name.lowercase()}"
    }
}

package com.steipete.codexbar

import android.app.Application
import android.content.Context
import com.steipete.codexbar.data.local.EncryptedPreferencesManager
import com.steipete.codexbar.data.local.SettingsRepositoryImpl
import com.steipete.codexbar.data.repository.UsageRepositoryImpl
import com.steipete.codexbar.data.remote.ResilientHttpClientFactory
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.domain.repository.SettingsRepository
import com.steipete.codexbar.domain.repository.UsageRepository
import okhttp3.OkHttpClient

/**
 * Dependency container managing singletons for the CodexBar Android application.
 */
class AppContainer(private val context: Context) {
    val httpClient: OkHttpClient by lazy {
        ResilientHttpClientFactory.createClient()
    }

    val secureStorage: SecureStorage by lazy {
        EncryptedPreferencesManager(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    val accountRepository: com.steipete.codexbar.domain.repository.AccountRepository by lazy {
        com.steipete.codexbar.data.local.AccountRepositoryImpl(context, secureStorage)
    }

    val usageRepository: UsageRepository by lazy {
        UsageRepositoryImpl(
            context = context,
            fetchers = UsageRepositoryImpl.defaultFetchers(httpClient),
            accountRepository = accountRepository
        )
    }
}

/**
 * Android Application entry point for CodexBar.
 * Initializes the root dependency container on launch.
 */
class CodexBarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

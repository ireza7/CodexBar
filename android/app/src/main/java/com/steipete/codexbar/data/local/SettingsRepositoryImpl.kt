package com.steipete.codexbar.data.local

import android.content.Context
import android.content.SharedPreferences
import com.steipete.codexbar.domain.model.ProviderSettings
import com.steipete.codexbar.domain.model.ThemeMode
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import com.steipete.codexbar.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Implementation of [SettingsRepository] backed by Android [SharedPreferences]
 * with reactive [MutableStateFlow] emissions for real-time UI synchronization.
 */
class SettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        ioDispatcher
    )

    private val _settingsStateFlow: MutableStateFlow<UserSettings> =
        MutableStateFlow(loadSettingsFromPrefs())

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            _settingsStateFlow.value = loadSettingsFromPrefs()
        }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun getSettingsFlow(): Flow<UserSettings> = _settingsStateFlow.asStateFlow()

    override suspend fun getSettings(): UserSettings = _settingsStateFlow.value

    override suspend fun updateProviderEnabled(provider: UsageProvider, enabled: Boolean) = withContext(ioDispatcher) {
        _settingsStateFlow.update { current ->
            val updatedList = if (enabled) {
                if (!current.activeProviders.contains(provider)) {
                    current.activeProviders + provider
                } else {
                    current.activeProviders
                }
            } else {
                current.activeProviders.filter { it != provider }
            }
            saveActiveProviders(updatedList)
            current.copy(activeProviders = updatedList)
        }
    }

    override suspend fun setRefreshInterval(minutes: Int) = withContext(ioDispatcher) {
        val sanitized = minutes.coerceIn(
            UserSettings.MIN_REFRESH_INTERVAL_MINUTES,
            UserSettings.MAX_REFRESH_INTERVAL_MINUTES
        )
        sharedPreferences.edit().putInt(KEY_REFRESH_INTERVAL, sanitized).apply()
        _settingsStateFlow.update { it.copy(refreshIntervalMinutes = sanitized) }
    }

    override suspend fun getActiveProviders(): List<UsageProvider> = withContext(ioDispatcher) {
        _settingsStateFlow.value.activeProviders
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _settingsStateFlow.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setShowPaceIndicator(show: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_PACE_INDICATOR, show).apply()
        _settingsStateFlow.update { it.copy(showPaceIndicator = show) }
    }

    override suspend fun getProviderSettings(provider: UsageProvider): ProviderSettings = withContext(ioDispatcher) {
        val enabled = _settingsStateFlow.value.isProviderActive(provider)
        val customUrl = sharedPreferences.getString(customBaseUrlKey(provider), null)
        ProviderSettings(
            provider = provider,
            enabled = enabled,
            customBaseUrl = customUrl
        )
    }

    override suspend fun updateProviderCustomBaseUrl(provider: UsageProvider, customBaseUrl: String?) = withContext(ioDispatcher) {
        sharedPreferences.edit().apply {
            if (!customBaseUrl.isNullOrBlank()) {
                putString(customBaseUrlKey(provider), customBaseUrl.trim())
            } else {
                remove(customBaseUrlKey(provider))
            }
        }.apply()
    }

    override suspend fun resetToDefaults() = withContext(ioDispatcher) {
        sharedPreferences.edit().clear().apply()
        val defaults = defaultSettings()
        _settingsStateFlow.value = defaults
    }

    private fun loadSettingsFromPrefs(): UserSettings {
        val refreshInterval = sharedPreferences.getInt(
            KEY_REFRESH_INTERVAL,
            UserSettings.DEFAULT_REFRESH_INTERVAL_MINUTES
        ).coerceIn(
            UserSettings.MIN_REFRESH_INTERVAL_MINUTES,
            UserSettings.MAX_REFRESH_INTERVAL_MINUTES
        )

        val showPace = sharedPreferences.getBoolean(KEY_SHOW_PACE_INDICATOR, true)

        val themeModeStr = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        val themeMode = try {
            if (themeModeStr != null) ThemeMode.valueOf(themeModeStr) else ThemeMode.DARK
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK
        }

        val activeProviders = loadActiveProvidersFromPrefs()

        return UserSettings(
            activeProviders = activeProviders,
            refreshIntervalMinutes = refreshInterval,
            showPaceIndicator = showPace,
            themeMode = themeMode
        )
    }

    private fun loadActiveProvidersFromPrefs(): List<UsageProvider> {
        val savedString = sharedPreferences.getString(KEY_ACTIVE_PROVIDERS, null)
        return if (savedString != null) {
            savedString.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .mapNotNull { name ->
                    try {
                        UsageProvider.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
        } else {
            defaultActiveProviders()
        }
    }

    private fun saveActiveProviders(providers: List<UsageProvider>) {
        val serialized = providers.joinToString(",") { it.name }
        sharedPreferences.edit().putString(KEY_ACTIVE_PROVIDERS, serialized).apply()
    }

    private fun defaultActiveProviders(): List<UsageProvider> {
        return listOf(
            UsageProvider.CODEX,
            UsageProvider.OPENAI,
            UsageProvider.CLAUDE,
            UsageProvider.CURSOR,
            UsageProvider.COPILOT,
            UsageProvider.GEMINI
        )
    }

    private fun defaultSettings(): UserSettings {
        return UserSettings(
            activeProviders = defaultActiveProviders(),
            refreshIntervalMinutes = UserSettings.DEFAULT_REFRESH_INTERVAL_MINUTES,
            showPaceIndicator = true,
            themeMode = ThemeMode.DARK
        )
    }

    companion object {
        const val PREFERENCES_NAME = "codexbar_user_settings"

        const val KEY_REFRESH_INTERVAL = "settings_refresh_interval_minutes"
        const val KEY_SHOW_PACE_INDICATOR = "settings_show_pace_indicator"
        const val KEY_THEME_MODE = "settings_theme_mode"
        const val KEY_ACTIVE_PROVIDERS = "settings_active_providers"
        const val KEY_PROVIDER_CUSTOM_BASE_URL_PREFIX = "settings_custom_base_url_"

        fun customBaseUrlKey(provider: UsageProvider): String =
            "$KEY_PROVIDER_CUSTOM_BASE_URL_PREFIX${provider.name.lowercase()}"
    }
}

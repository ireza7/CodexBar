package com.steipete.codexbar.domain.repository

import com.steipete.codexbar.domain.model.ProviderSettings
import com.steipete.codexbar.domain.model.ThemeMode
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences, active provider toggles,
 * refresh intervals, and UI options.
 */
interface SettingsRepository {
    /**
     * Returns a reactive [Flow] emitting current [UserSettings] whenever any setting is updated.
     */
    fun getSettingsFlow(): Flow<UserSettings>

    /**
     * Retrieves the current snapshot of [UserSettings].
     */
    suspend fun getSettings(): UserSettings

    /**
     * Enables or disables a specific [UsageProvider] in the active providers list.
     *
     * @param provider The provider to update.
     * @param enabled True to add to active providers; false to remove.
     */
    suspend fun updateProviderEnabled(provider: UsageProvider, enabled: Boolean)

    /**
     * Updates the periodic polling refresh interval in minutes.
     *
     * @param minutes The interval in minutes (constrained between 1 and 1440).
     */
    suspend fun setRefreshInterval(minutes: Int)

    /**
     * Returns the list of currently enabled and active [UsageProvider]s.
     */
    suspend fun getActiveProviders(): List<UsageProvider>

    /**
     * Sets the active UI theme mode ([ThemeMode.SYSTEM], [ThemeMode.DARK], [ThemeMode.LIGHT]).
     */
    suspend fun updateThemeMode(themeMode: ThemeMode)

    /**
     * Toggles whether the workday pace / deficit indicator is shown on usage cards.
     */
    suspend fun setShowPaceIndicator(show: Boolean)

    /**
     * Retrieves the [ProviderSettings] for a given [provider].
     */
    suspend fun getProviderSettings(provider: UsageProvider): ProviderSettings

    /**
     * Sets or removes a custom API base URL for a given [provider].
     */
    suspend fun updateProviderCustomBaseUrl(provider: UsageProvider, customBaseUrl: String?)

    /**
     * Resets all settings to their default values.
     */
    suspend fun resetToDefaults()
}

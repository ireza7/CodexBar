package com.steipete.codexbar.domain.model

/**
 * Theme mode selection for the CodexBar application UI.
 */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

/**
 * Aggregated user settings for active providers, refresh schedule, and UI display options.
 *
 * @property activeProviders The ordered list of currently enabled usage providers displayed on the dashboard.
 * @property refreshIntervalMinutes The periodic background polling interval in minutes (default: 15 min).
 * @property showPaceIndicator Whether to display the workday quota burn pace / deficit tip on usage cards.
 * @property themeMode The active theme mode (SYSTEM, DARK, LIGHT). Default is DARK matching CodexBar style.
 */
data class UserSettings(
    val activeProviders: List<UsageProvider> = emptyList(),
    val refreshIntervalMinutes: Int = DEFAULT_REFRESH_INTERVAL_MINUTES,
    val showPaceIndicator: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK
) {
    /**
     * Checks if a given [UsageProvider] is currently in the active providers list.
     */
    fun isProviderActive(provider: UsageProvider): Boolean = activeProviders.contains(provider)

    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 15
        const val MIN_REFRESH_INTERVAL_MINUTES = 1
        const val MAX_REFRESH_INTERVAL_MINUTES = 1440 // 24 hours
    }
}

/**
 * Configuration and customization settings for an individual [UsageProvider].
 *
 * @property provider The provider this configuration applies to.
 * @property enabled Whether this provider is enabled for polling and display.
 * @property customBaseUrl An optional custom API proxy or base URL endpoint (e.g., custom OpenAI proxy).
 */
data class ProviderSettings(
    val provider: UsageProvider,
    val enabled: Boolean = true,
    val customBaseUrl: String? = null
)

/**
 * Authentication and credential container for AI provider API queries.
 *
 * Sensitive credential strings are securely retrieved from [SecureStorage] at runtime
 * and passed into provider fetchers.
 *
 * @property apiKey The secret API key / bearer token for the provider (if required).
 * @property sessionToken The session cookie / OAuth access token (e.g., for Claude or Cursor web APIs).
 * @property orgId An optional organization or project identifier (e.g., OpenAI organization ID).
 */
data class ProviderCredentials(
    val apiKey: String? = null,
    val sessionToken: String? = null,
    val orgId: String? = null
) {
    /**
     * Returns true if at least one authentication credential is provided and non-blank.
     */
    val hasCredentials: Boolean
        get() = !apiKey.isNullOrBlank() || !sessionToken.isNullOrBlank()

    /**
     * Alias for [hasCredentials].
     */
    val isConfigured: Boolean
        get() = hasCredentials
}

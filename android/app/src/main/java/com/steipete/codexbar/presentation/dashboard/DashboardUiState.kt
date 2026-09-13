package com.steipete.codexbar.presentation.dashboard

import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot

/**
 * Immutable UI state model for the CodexBar Dashboard.
 * Strictly guarantees provider data siloing by isolating snapshots per provider.
 *
 * @property activeProviders The ordered list of currently enabled providers configured by the user.
 * @property selectedProvider The currently viewed provider in the main UsageCard, or null if none active.
 * @property usageSnapshots Map of latest usage snapshots keyed by [UsageProvider].
 * @property isLoading True during initial data loading.
 * @property isRefreshing True when an active network probe or pull-to-refresh is underway.
 * @property globalError Optional top-level message if an unexpected repository-level error occurs.
 */
data class DashboardUiState(
    val activeProviders: List<UsageProvider> = emptyList(),
    val selectedProvider: UsageProvider? = null,
    val usageSnapshots: Map<UsageProvider, UsageSnapshot> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val globalError: String? = null
) {
    /**
     * Siloed usage snapshot for the currently selected provider.
     * Guaranteed to never leak data from any other provider.
     */
    val selectedSnapshot: UsageSnapshot?
        get() = selectedProvider?.let { usageSnapshots[it] }

    /**
     * True if at least one AI provider is configured and active.
     */
    val hasActiveProviders: Boolean
        get() = activeProviders.isNotEmpty()
}

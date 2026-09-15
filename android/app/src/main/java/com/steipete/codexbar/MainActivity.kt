package com.steipete.codexbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steipete.codexbar.presentation.dashboard.DashboardScreen
import com.steipete.codexbar.presentation.dashboard.DashboardViewModel
import com.steipete.codexbar.presentation.settings.SettingsView
import com.steipete.codexbar.presentation.theme.CodexBarTheme

/**
 * Navigation screen routes within the CodexBar application.
 */
enum class AppScreen {
    DASHBOARD,
    SETTINGS
}

/**
 * Main Activity entry point for CodexBar Android.
 * Enables edge-to-edge layout, applies [CodexBarTheme], and coordinates top-level navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CodexBarApp
        val container = app.container

        setContent {
            CodexBarTheme {
                CodexBarNavHost(container = container)
            }
        }
    }
}

@Composable
fun CodexBarNavHost(
    container: AppContainer,
    dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            usageRepository = container.usageRepository,
            settingsRepository = container.settingsRepository,
            secureStorage = container.secureStorage,
            accountRepository = container.accountRepository
        )
    )
) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.DASHBOARD) }
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        dashboardViewModel.refreshSelected()
    }

    when (currentScreen) {
        AppScreen.DASHBOARD -> {
            DashboardScreen(
                uiState = uiState,
                onSelectProvider = dashboardViewModel::selectProvider,
                onRefresh = dashboardViewModel::refreshSelected,
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                onDismissError = dashboardViewModel::clearGlobalError,
                onSwitchAccount = dashboardViewModel::switchAccount,
                onAddAccount = dashboardViewModel::addAccount
            )
        }

        AppScreen.SETTINGS -> {
            BackHandler {
                currentScreen = AppScreen.DASHBOARD
            }
            SettingsView(
                settingsRepository = container.settingsRepository,
                secureStorage = container.secureStorage,
                accountRepository = container.accountRepository,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
            )
        }
    }
}

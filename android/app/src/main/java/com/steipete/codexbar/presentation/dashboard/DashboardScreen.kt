package com.steipete.codexbar.presentation.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.steipete.codexbar.domain.model.ProviderDescriptor
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import com.steipete.codexbar.presentation.components.ProviderSwitcher
import com.steipete.codexbar.presentation.components.UsageCard
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography

/**
 * Main dashboard screen displaying provider switcher, live usage cards, and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSelectProvider: (UsageProvider) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissError: (() -> Unit)? = null,
    onSwitchAccount: ((String) -> Unit)? = null,
    onAddAccount: ((String, String, String?) -> Unit)? = null
) {
    var showAccountDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Antigravity Quota",
                            style = CodexBarTypography.titleLarge,
                            color = CodexBarColors.TextPrimary
                        )
                    }
                },
                actions = {
                    // Refresh Button with smooth rotation animation when active
                    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )

                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Usage",
                            tint = CodexBarColors.TextPrimary,
                            modifier = if (uiState.isRefreshing) Modifier.rotate(rotation) else Modifier
                        )
                    }

                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CodexBarColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CodexBarColors.Background,
                    titleContentColor = CodexBarColors.TextPrimary
                )
            )
        },
        containerColor = CodexBarColors.Background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Error Banner (if present)
            if (uiState.globalError != null) {
                Surface(
                    color = CodexBarColors.StatusRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CodexBarColors.StatusRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = CodexBarColors.StatusRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.globalError,
                            style = CodexBarTypography.labelMedium,
                            color = CodexBarColors.StatusRed,
                            modifier = Modifier.weight(1f)
                        )
                        if (onDismissError != null) {
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CodexBarColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Multi-Account Switcher Banner
            if (uiState.accounts.isNotEmpty()) {
                Surface(
                    onClick = { showAccountDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = CodexBarColors.SurfaceCard,
                    border = BorderStroke(1.dp, CodexBarColors.CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = CodexBarColors.ProviderGemini,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                val activeLabel = uiState.activeAccount?.label ?: "Default Account"
                                Text(
                                    text = activeLabel,
                                    style = CodexBarTypography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = CodexBarColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (!uiState.activeAccount?.email.isNullOrBlank()) {
                                    Text(
                                        text = uiState.activeAccount!!.email!!,
                                        style = CodexBarTypography.labelSmall,
                                        color = CodexBarColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (uiState.accounts.size > 1) {
                                Text(
                                    text = "${uiState.accounts.size} accounts",
                                    style = CodexBarTypography.labelSmall,
                                    color = CodexBarColors.ProviderGemini
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Switch Account",
                                tint = CodexBarColors.TextSecondary
                            )
                        }
                    }
                }
            }

            if (showAccountDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAccountDialog = false },
                    title = {
                        Text(text = "Switch Account", style = CodexBarTypography.headlineMedium)
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.accounts.forEach { acc ->
                                val isCurrent = acc.id == uiState.activeAccount?.id
                                Surface(
                                    onClick = {
                                        onSwitchAccount?.invoke(acc.id)
                                        showAccountDialog = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) CodexBarColors.ProviderGemini.copy(alpha = 0.15f) else CodexBarColors.SurfaceCardElevated,
                                    border = if (isCurrent) BorderStroke(1.dp, CodexBarColors.ProviderGemini) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = acc.label,
                                                style = CodexBarTypography.bodyMedium,
                                                fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                                color = CodexBarColors.TextPrimary
                                            )
                                            if (!acc.email.isNullOrBlank()) {
                                                Text(
                                                    text = acc.email,
                                                    style = CodexBarTypography.labelSmall,
                                                    color = CodexBarColors.TextSecondary
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Active",
                                                tint = CodexBarColors.ProviderGemini,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showAccountDialog = false
                                onNavigateToSettings()
                            }
                        ) {
                            Text(text = "Manage Accounts", color = CodexBarColors.ProviderGemini)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showAccountDialog = false }) {
                            Text(text = "Close", color = CodexBarColors.TextSecondary)
                        }
                    }
                )
            }

            when {
                uiState.isLoading && uiState.activeProviders.isEmpty() -> {
                    // Initial Loading Screen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CodexBarColors.ProviderCodex,
                            strokeWidth = 3.dp
                        )
                    }
                }

                uiState.activeProviders.isEmpty() -> {
                    // Empty State: No active providers configured
                    EmptyDashboardState(onNavigateToSettings = onNavigateToSettings)
                }

                else -> {
                    // Active Provider Switcher
                    ProviderSwitcher(
                        providers = uiState.activeProviders,
                        selectedProvider = uiState.selectedProvider,
                        onSelectProvider = onSelectProvider,
                        snapshots = uiState.usageSnapshots,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Main Selected Usage Card
                    val selected = uiState.selectedProvider
                    val snapshot = uiState.selectedSnapshot
                    val descriptor = selected?.let { ProviderDescriptor.forProvider(it) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selected != null) {
                            val activeSnapshot = snapshot ?: UsageSnapshot(provider = selected)
                            UsageCard(
                                snapshot = activeSnapshot,
                                descriptor = descriptor,
                                onRetry = onRefresh
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardState(
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CodexBarColors.SurfaceCard,
            border = BorderStroke(1.dp, CodexBarColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CodexBarColors.TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No Providers Enabled",
                    style = CodexBarTypography.headlineMedium,
                    color = CodexBarColors.TextPrimary
                )
                Text(
                    text = "Configure your AI providers (OpenAI, Claude, Cursor, Copilot, Gemini) in Settings to track your usage limits and pace.",
                    style = CodexBarTypography.bodyMedium,
                    color = CodexBarColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CodexBarColors.ProviderCodex,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Open Settings", style = CodexBarTypography.labelMedium)
                }
            }
        }
    }
}

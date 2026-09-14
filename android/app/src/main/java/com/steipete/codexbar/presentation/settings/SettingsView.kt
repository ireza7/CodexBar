package com.steipete.codexbar.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steipete.codexbar.domain.model.ProviderDescriptor
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.domain.repository.SettingsRepository
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings screen for configuring AI provider credentials, toggling active providers,
 * selecting adaptive refresh intervals, and customizing pace visualization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    settingsRepository: SettingsRepository,
    secureStorage: SecureStorage,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userSettings by settingsRepository.getSettingsFlow()
        .collectAsStateWithLifecycle(initialValue = UserSettings())

    val scope = rememberCoroutineScope()

    // Store in-memory draft API keys keyed by UsageProvider
    val apiKeysDraft = remember { mutableStateMapOf<UsageProvider, String>() }
    val savedConfirmationMap = remember { mutableStateMapOf<UsageProvider, Boolean>() }

    // Load initial API keys securely
    LaunchedEffect(Unit) {
        UsageProvider.entries.forEach { provider ->
            val key = secureStorage.getApiKey(provider) ?: ""
            apiKeysDraft[provider] = key
        }
    }

    // Search query state for filtering providers
    var searchQuery by remember { mutableStateOf("") }

    // Dynamic providers list: all 69 providers, sorted by active status first, then displayName
    val allProviders = remember { UsageProvider.entries }
    val filteredProviders = remember(searchQuery, userSettings.activeProviders) {
        val query = searchQuery.trim().lowercase()
        allProviders
            .filter { provider ->
                query.isEmpty() ||
                provider.displayName.lowercase().contains(query) ||
                provider.id.lowercase().contains(query)
            }
            .sortedWith(
                compareByDescending<UsageProvider> { userSettings.isProviderActive(it) }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = CodexBarTypography.titleLarge,
                        color = CodexBarColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CodexBarColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CodexBarColors.Background,
                    titleContentColor = CodexBarColors.TextPrimary,
                    navigationIconContentColor = CodexBarColors.TextPrimary
                )
            )
        },
        containerColor = CodexBarColors.Background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: GENERAL PREFERENCES
            item {
                Text(
                    text = "GENERAL PREFERENCES",
                    style = CodexBarTypography.labelSmall,
                    color = CodexBarColors.TextTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                RefreshIntervalCard(
                    currentMinutes = userSettings.refreshIntervalMinutes,
                    onSelectInterval = { minutes ->
                        scope.launch { settingsRepository.setRefreshInterval(minutes) }
                    }
                )
            }

            item {
                PaceToggleCard(
                    showPace = userSettings.showPaceIndicator,
                    onToggle = { show ->
                        scope.launch { settingsRepository.setShowPaceIndicator(show) }
                    }
                )
            }

            // SECTION 2: AI PROVIDERS (69 Providers Supported)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI PROVIDERS (${filteredProviders.size}/${allProviders.size})",
                        style = CodexBarTypography.labelSmall,
                        color = CodexBarColors.TextTertiary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }
            }

            // Search input field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search provider (e.g. Grok, Claude, Bedrock)...",
                            style = CodexBarTypography.bodyMedium,
                            color = CodexBarColors.TextTertiary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CodexBarColors.TextPrimary,
                        unfocusedTextColor = CodexBarColors.TextPrimary,
                        focusedContainerColor = CodexBarColors.SurfaceCard,
                        unfocusedContainerColor = CodexBarColors.SurfaceCard,
                        focusedBorderColor = CodexBarColors.ProviderCodex,
                        unfocusedBorderColor = CodexBarColors.CardBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(filteredProviders, key = { it.id }) { provider ->
                val isEnabled = userSettings.isProviderActive(provider)
                val descriptor = ProviderDescriptor.forProvider(provider)
                val currentDraftKey = apiKeysDraft[provider] ?: ""
                val isSaved = savedConfirmationMap[provider] == true

                ProviderSettingsCard(
                    provider = provider,
                    descriptor = descriptor,
                    isEnabled = isEnabled,
                    apiKey = currentDraftKey,
                    isSavedConfirmation = isSaved,
                    onToggleEnabled = { enabled ->
                        scope.launch {
                            settingsRepository.updateProviderEnabled(provider, enabled)
                        }
                    },
                    onApiKeyChange = { newKey ->
                        apiKeysDraft[provider] = newKey
                        savedConfirmationMap[provider] = false
                    },
                    onSaveApiKey = { key ->
                        scope.launch {
                            secureStorage.saveApiKey(provider, key)
                            savedConfirmationMap[provider] = true
                            delay(2500)
                            savedConfirmationMap[provider] = false
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RefreshIntervalCard(
    currentMinutes: Int,
    onSelectInterval: (Int) -> Unit
) {
    // Refresh intervals matching AdaptiveRefreshPolicy: 2m, 5m, 15m, 30m
    val intervals = listOf(2, 5, 15, 30)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexBarColors.SurfaceCard,
        border = BorderStroke(1.dp, CodexBarColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adaptive Refresh Interval",
                    style = CodexBarTypography.bodyLarge,
                    color = CodexBarColors.TextPrimary
                )
                Text(
                    text = "${currentMinutes}m",
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.ProviderCodex,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Controls how frequently background probes verify quota consumption.",
                style = CodexBarTypography.labelMedium,
                color = CodexBarColors.TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                intervals.forEach { minutes ->
                    val isSelected = currentMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectInterval(minutes) },
                        label = { Text("${minutes}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CodexBarColors.ProviderCodex.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White,
                            containerColor = CodexBarColors.SurfaceCardElevated,
                            labelColor = CodexBarColors.TextSecondary
                        ),
                        border = if (isSelected) BorderStroke(1.dp, CodexBarColors.ProviderCodex) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun PaceToggleCard(
    showPace: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexBarColors.SurfaceCard,
        border = BorderStroke(1.dp, CodexBarColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Show Workday Pace Indicator",
                    style = CodexBarTypography.bodyLarge,
                    color = CodexBarColors.TextPrimary
                )
                Text(
                    text = "Displays the 3-stripe deficit/reserve notch on quota progress bars.",
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.TextSecondary
                )
            }
            Switch(
                checked = showPace,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CodexBarColors.ProviderCodex,
                    uncheckedTrackColor = CodexBarColors.SurfaceCardElevated
                )
            )
        }
    }
}

@Composable
private fun ProviderSettingsCard(
    provider: UsageProvider,
    descriptor: ProviderDescriptor,
    isEnabled: Boolean,
    apiKey: String,
    isSavedConfirmation: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val brandColor = CodexBarColors.colorForProvider(provider)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexBarColors.SurfaceCard,
        border = BorderStroke(1.dp, CodexBarColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Brand Dot + Name + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(brandColor)
                    )
                    Column {
                        Text(
                            text = provider.displayName,
                            style = CodexBarTypography.headlineMedium,
                            color = CodexBarColors.TextPrimary
                        )
                        ConnectionStatusRow(isConfigured = apiKey.isNotBlank())
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = brandColor,
                        uncheckedTrackColor = CodexBarColors.SurfaceCardElevated
                    )
                )
            }

            // Credential Form (Visible when enabled)
            AnimatedVisibility(visible = isEnabled) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val labelText = if (descriptor.requiresApiKey) "API Key / Bearer Token" else "Session Token / Key"
                    val placeholderText = when (provider) {
                        UsageProvider.OPENAI -> "sk-proj-..."
                        UsageProvider.CLAUDE -> "sessionKey / sk-ant-..."
                        UsageProvider.CURSOR -> "WorkosCursorSessionToken..."
                        else -> "Enter ${provider.displayName} API Key..."
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(labelText) },
                        placeholder = { Text(placeholderText, color = CodexBarColors.TextTertiary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = CodexBarColors.TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandColor,
                            unfocusedBorderColor = CodexBarColors.CardBorder,
                            focusedTextColor = CodexBarColors.TextPrimary,
                            unfocusedTextColor = CodexBarColors.TextPrimary,
                            focusedLabelColor = brandColor,
                            unfocusedLabelColor = CodexBarColors.TextSecondary
                        )
                    )

                    // Save Button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSaveApiKey(apiKey) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSavedConfirmation) CodexBarColors.StatusGreen else CodexBarColors.SurfaceCardElevated,
                                contentColor = CodexBarColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSavedConfirmation) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(" Saved", style = CodexBarTypography.labelSmall)
                            } else {
                                Text("Save Key", style = CodexBarTypography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusRow(isConfigured: Boolean) {
    val (color, text) = if (isConfigured) {
        CodexBarColors.StatusGreen to "Configured"
    } else {
        CodexBarColors.TextTertiary to "Unconfigured"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = CodexBarTypography.labelSmall,
            color = color
        )
    }
}

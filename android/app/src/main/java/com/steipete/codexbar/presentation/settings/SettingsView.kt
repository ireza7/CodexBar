package com.steipete.codexbar.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.domain.repository.SettingsRepository
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val context = LocalContext.current

    var tokenDraft by remember { mutableStateOf("") }
    var isSavedConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tokenDraft = secureStorage.getApiKey(UsageProvider.ANTIGRAVITY) ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Antigravity Quota Settings",
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
            item {
                RefreshIntervalCard(
                    currentMinutes = userSettings.refreshIntervalMinutes,
                    onSelectInterval = { interval ->
                        scope.launch { settingsRepository.setRefreshIntervalMinutes(interval) }
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

            // Antigravity Google Account & Browser Auth Card
            item {
                AntigravityAccountCard(
                    context = context,
                    token = tokenDraft,
                    isSavedConfirmation = isSavedConfirmation,
                    onTokenChange = { tokenDraft = it },
                    onSaveToken = { token ->
                        scope.launch {
                            secureStorage.saveApiKey(UsageProvider.ANTIGRAVITY, token)
                            isSavedConfirmation = true
                            delay(2000)
                            isSavedConfirmation = false
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
private fun AntigravityAccountCard(
    context: Context,
    token: String,
    isSavedConfirmation: Boolean,
    onTokenChange: (String) -> Unit,
    onSaveToken: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CodexBarColors.ProviderGemini)
                    )
                    Text(
                        text = "Antigravity (Google)",
                        style = CodexBarTypography.headlineMedium,
                        color = CodexBarColors.TextPrimary
                    )
                }

                ConnectionStatusRow(isConfigured = token.isNotBlank())
            }

            Text(
                text = "Connect your Google account session to track quota & resets for Gemini, Claude, and GPT models.",
                style = CodexBarTypography.bodyMedium,
                color = CodexBarColors.TextSecondary
            )

            // 1. Browser Login Button
            OutlinedButton(
                onClick = {
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    val authUri = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth?client_id=1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&scope=https://www.googleapis.com/auth/cloud-platform%20https://www.googleapis.com/auth/userinfo.email&prompt=select_account")
                    customTabsIntent.launchUrl(context, authUri)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CodexBarColors.TextPrimary
                ),
                border = BorderStroke(1.dp, CodexBarColors.ProviderGemini),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = CodexBarColors.ProviderGemini,
                    modifier = Modifier.size(18.dp)
                )
                Text("  Login with Google in Browser", style = CodexBarTypography.labelMedium)
            }

            Text(
                text = "Or enter your OAuth Bearer Token / Session JSON / Cookie:",
                style = CodexBarTypography.labelSmall,
                color = CodexBarColors.TextTertiary
            )

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text("Token / Session JSON / SID") },
                placeholder = { Text("ya29... or JSON token or SID", color = CodexBarColors.TextTertiary) },
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
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CodexBarColors.ProviderGemini,
                    unfocusedBorderColor = CodexBarColors.CardBorder,
                    focusedTextColor = CodexBarColors.TextPrimary,
                    unfocusedTextColor = CodexBarColors.TextPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSaveToken(token) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSavedConfirmation) CodexBarColors.StatusGreen else CodexBarColors.ProviderGemini,
                        contentColor = Color.White
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
                        Text("Save & Connect", style = CodexBarTypography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshIntervalCard(
    currentMinutes: Int,
    onSelectInterval: (Int) -> Unit
) {
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
                    text = "Refresh Interval",
                    style = CodexBarTypography.bodyLarge,
                    color = CodexBarColors.TextPrimary
                )
                Text(
                    text = "${currentMinutes}m",
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.ProviderGemini,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Controls how often quotas and reset times update in the background.",
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
                            selectedContainerColor = CodexBarColors.ProviderGemini.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White,
                            containerColor = CodexBarColors.SurfaceCardElevated,
                            labelColor = CodexBarColors.TextSecondary
                        ),
                        border = if (isSelected) BorderStroke(1.dp, CodexBarColors.ProviderGemini) else null
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
                    text = "Show Pace & Warning Indicator",
                    style = CodexBarTypography.bodyLarge,
                    color = CodexBarColors.TextPrimary
                )
                Text(
                    text = "Visualizes burn rate and warning markers on progress bars.",
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.TextSecondary
                )
            }
            Switch(
                checked = showPace,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CodexBarColors.ProviderGemini,
                    uncheckedTrackColor = CodexBarColors.SurfaceCardElevated
                )
            )
        }
    }
}

@Composable
private fun ConnectionStatusRow(isConfigured: Boolean) {
    val (color, text) = if (isConfigured) {
        CodexBarColors.StatusGreen to "Connected"
    } else {
        CodexBarColors.TextTertiary to "Not Connected"
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

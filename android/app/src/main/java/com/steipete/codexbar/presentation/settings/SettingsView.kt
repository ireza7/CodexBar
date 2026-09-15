package com.steipete.codexbar.presentation.settings

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steipete.codexbar.data.remote.GoogleOAuthManager
import com.steipete.codexbar.domain.model.AntigravityAccount
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import com.steipete.codexbar.domain.repository.AccountRepository
import com.steipete.codexbar.domain.repository.SecureStorage
import com.steipete.codexbar.domain.repository.SettingsRepository
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    settingsRepository: SettingsRepository,
    secureStorage: SecureStorage,
    accountRepository: AccountRepository? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userSettings by settingsRepository.getSettingsFlow()
        .collectAsStateWithLifecycle(initialValue = UserSettings())

    val accounts by (accountRepository?.getAccountsFlow() ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            // Multi-Account Management Section
            item {
                MultiAccountCard(
                    context = context,
                    accounts = accounts,
                    onSwitchAccount = { id ->
                        scope.launch { accountRepository?.switchActiveAccount(id) }
                    },
                    onAddAccount = { label, token, email ->
                        scope.launch { accountRepository?.addAccount(label, token, email) }
                    },
                    onDeleteAccount = { id ->
                        scope.launch { accountRepository?.deleteAccount(id) }
                    }
                )
            }

            item {
                RefreshIntervalCard(
                    currentMinutes = userSettings.refreshIntervalMinutes,
                    onSelectInterval = { interval ->
                        scope.launch { settingsRepository.setRefreshInterval(interval) }
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

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CodexBar Android v1.0.3 (Build 3)",
                            style = CodexBarTypography.labelSmall,
                            color = CodexBarColors.TextSecondary
                        )
                        Text(
                            text = "Multi-Account & Screenshot-Style Widget",
                            style = CodexBarTypography.labelSmall,
                            color = CodexBarColors.ProviderGemini
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MultiAccountCard(
    context: Context,
    accounts: List<AntigravityAccount>,
    onSwitchAccount: (String) -> Unit,
    onAddAccount: (String, String, String?) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showAddForm by remember { mutableStateOf(accounts.isEmpty()) }
    var newAccountLabel by remember { mutableStateOf("") }
    var newAccountToken by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var accountToDelete by remember { mutableStateOf<AntigravityAccount?>(null) }
    var tokenVisible by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
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
            // Card Header
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
                        text = "Google Accounts (${accounts.size})",
                        style = CodexBarTypography.headlineMedium,
                        color = CodexBarColors.TextPrimary
                    )
                }

                if (!showAddForm && accounts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CodexBarColors.ProviderGemini
                        ),
                        border = BorderStroke(1.dp, CodexBarColors.ProviderGemini.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Account", style = CodexBarTypography.labelSmall)
                    }
                }
            }

            Text(
                text = "Manage multiple Antigravity / Google sessions (Personal, Work, etc.). You can switch active accounts anytime in the app or widget.",
                style = CodexBarTypography.bodyMedium,
                color = CodexBarColors.TextSecondary
            )

            // Existing Accounts List
            if (accounts.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        AccountItemRow(
                            account = account,
                            onSelect = { onSwitchAccount(account.id) },
                            onDelete = { accountToDelete = account }
                        )
                    }
                }
            }

            // Add Account Form
            AnimatedVisibility(visible = showAddForm || accounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CodexBarColors.SurfaceCardElevated, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Google Account",
                            style = CodexBarTypography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CodexBarColors.TextPrimary
                        )

                        if (accounts.isNotEmpty()) {
                            TextButton(onClick = { showAddForm = false }) {
                                Text("Cancel", color = CodexBarColors.TextSecondary, style = CodexBarTypography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newAccountLabel,
                        onValueChange = { newAccountLabel = it },
                        label = { Text("Account Name / Label") },
                        placeholder = { Text("e.g. Work, Personal, Account 2", color = CodexBarColors.TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CodexBarColors.ProviderGemini,
                            unfocusedBorderColor = CodexBarColors.CardBorder,
                            focusedTextColor = CodexBarColors.TextPrimary,
                            unfocusedTextColor = CodexBarColors.TextPrimary
                        )
                    )

                    // 1. One-Click Browser Login
                    OutlinedButton(
                        onClick = {
                            if (isLoggingIn) return@OutlinedButton
                            isLoggingIn = true
                            loginError = null
                            scope.launch {
                                val result = GoogleOAuthManager.loginWithBrowser(context)
                                isLoggingIn = false
                                result.onSuccess { sessionJson ->
                                    val email = extractEmailFromOAuthResponse(sessionJson)
                                    val label = newAccountLabel.ifBlank {
                                        email?.substringBefore("@") ?: "Account ${accounts.size + 1}"
                                    }
                                    onAddAccount(label, sessionJson, email)
                                    newAccountLabel = ""
                                    newAccountToken = ""
                                    showAddForm = false
                                }.onFailure { err ->
                                    loginError = err.message ?: "Login cancelled or failed"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CodexBarColors.TextPrimary
                        ),
                        border = BorderStroke(1.dp, CodexBarColors.ProviderGemini),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoggingIn
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CodexBarColors.ProviderGemini
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Waiting for Google Login...", style = CodexBarTypography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = CodexBarColors.ProviderGemini,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Login with Google in Browser", style = CodexBarTypography.labelMedium)
                        }
                    }

                    if (loginError != null) {
                        Text(
                            text = "⚠ " + loginError!!,
                            style = CodexBarTypography.labelSmall,
                            color = CodexBarColors.StatusRed
                        )
                    }

                    HorizontalDivider(color = CodexBarColors.Divider, thickness = 1.dp)

                    Text(
                        text = "Or paste token / session JSON / SID cookie directly:",
                        style = CodexBarTypography.labelSmall,
                        color = CodexBarColors.TextTertiary
                    )

                    OutlinedTextField(
                        value = newAccountToken,
                        onValueChange = { newAccountToken = it },
                        label = { Text("Token / Session JSON / SID") },
                        placeholder = { Text("ya29... or JSON or SID", color = CodexBarColors.TextTertiary) },
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = CodexBarColors.TextSecondary
                                )
                            }
                        },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CodexBarColors.ProviderGemini,
                            unfocusedBorderColor = CodexBarColors.CardBorder,
                            focusedTextColor = CodexBarColors.TextPrimary,
                            unfocusedTextColor = CodexBarColors.TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            if (newAccountToken.isNotBlank()) {
                                val email = extractEmailFromOAuthResponse(newAccountToken)
                                val label = newAccountLabel.ifBlank {
                                    email?.substringBefore("@") ?: "Account ${accounts.size + 1}"
                                }
                                onAddAccount(label, newAccountToken, email)
                                newAccountLabel = ""
                                newAccountToken = ""
                                showAddForm = false
                            }
                        },
                        enabled = newAccountToken.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CodexBarColors.ProviderGemini,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Account", style = CodexBarTypography.labelSmall)
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting an account
    accountToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account?", style = CodexBarTypography.headlineMedium) },
            text = {
                Text(
                    "Are you sure you want to remove \"${target.label}\"? You can add it back anytime.",
                    style = CodexBarTypography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(target.id)
                        accountToDelete = null
                    }
                ) {
                    Text("Delete", color = CodexBarColors.StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = CodexBarColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AccountItemRow(
    account: AntigravityAccount,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (account.isActive) CodexBarColors.ProviderGemini.copy(alpha = 0.15f) else CodexBarColors.SurfaceCardElevated,
        border = BorderStroke(1.dp, if (account.isActive) CodexBarColors.ProviderGemini else CodexBarColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (account.isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (account.isActive) "Active" else "Select",
                    tint = if (account.isActive) CodexBarColors.ProviderGemini else CodexBarColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = account.label,
                            style = CodexBarTypography.bodyMedium,
                            fontWeight = if (account.isActive) FontWeight.Bold else FontWeight.Medium,
                            color = CodexBarColors.TextPrimary
                        )

                        if (account.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CodexBarColors.ProviderGemini.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "Active",
                                    style = CodexBarTypography.labelSmall,
                                    color = CodexBarColors.ProviderGemini,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (!account.email.isNullOrBlank()) {
                        Text(
                            text = account.email,
                            style = CodexBarTypography.labelSmall,
                            color = CodexBarColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (account.gemini5hPct != null || account.claude5hPct != null) {
                        val quotaText = listOfNotNull(
                            account.gemini5hPct?.let { "Gemini: $it%" },
                            account.claude5hPct?.let { "Claude: $it%" }
                        ).joinToString(" · ")
                        Text(
                            text = quotaText,
                            style = CodexBarTypography.labelSmall,
                            color = CodexBarColors.TextTertiary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = CodexBarColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun extractEmailFromOAuthResponse(tokenJsonOrRaw: String): String? {
    val trimmed = tokenJsonOrRaw.trim()
    if (!trimmed.startsWith("{")) return null
    return try {
        val obj = JSONObject(trimmed)
        if (obj.has("email")) return obj.getString("email")
        if (obj.has("id_token")) {
            val idToken = obj.getString("id_token")
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP))
                val payloadJson = JSONObject(payload)
                if (payloadJson.has("email")) {
                    return payloadJson.getString("email")
                }
            }
        }
        null
    } catch (_: Exception) {
        null
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

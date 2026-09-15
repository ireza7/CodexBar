package com.steipete.codexbar.presentation.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.steipete.codexbar.domain.model.AntigravityAccount
import com.steipete.codexbar.presentation.theme.CodexBarTheme
import kotlinx.coroutines.launch

/**
 * Initial configuration and customization activity for Antigravity App Widgets.
 * Allows users to choose target account (individual account, auto-sync active account, or all accounts),
 * select Neumorphic theme and accent colors, and toggle display density.
 */
class AntigravityWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the result to CANCELED by default in case the user backs out
        setResult(Activity.RESULT_CANCELED)

        // Find the widget id from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val accounts = WidgetConfigManager.getSavedAccounts(this)
        val initialConfig = WidgetConfigManager.getConfig(this, appWidgetId)

        setContent {
            CodexBarTheme {
                WidgetConfigureScreen(
                    accounts = accounts,
                    initialConfig = initialConfig,
                    onSave = { updatedConfig ->
                        saveAndFinish(updatedConfig)
                    }
                )
            }
        }
    }

    private fun saveAndFinish(config: WidgetConfig) {
        val context = applicationContext
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

        scope.launch {
            // Save configuration
            WidgetConfigManager.saveConfig(context, appWidgetId, config)

            // Trigger immediate widget render
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                if (glanceId != null) {
                    AntigravityQuotaWidget().update(context, glanceId)
                } else {
                    AntigravityQuotaWidget().updateAll(context)
                }
            } catch (_: Throwable) {
                try {
                    AntigravityQuotaWidget().updateAll(context)
                } catch (_: Throwable) {}
            }

            // Return success
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigureScreen(
    accounts: List<AntigravityAccount>,
    initialConfig: WidgetConfig,
    onSave: (WidgetConfig) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(initialConfig.targetAccountId) }
    var selectedTheme by remember { mutableStateOf(initialConfig.theme) }
    var selectedAccent by remember { mutableStateOf(initialConfig.accent) }
    var showWeekly by remember { mutableStateOf(initialConfig.showWeekly) }
    var showResetTime by remember { mutableStateOf(initialConfig.showResetTime) }
    var compactDensity by remember { mutableStateOf(initialConfig.compactDensity) }

    val darkBg = Color(0xFF14161F)
    val cardBg = Color(0xFF1B1D28)
    val strokeColor = Color(0xFF2E3345)
    val primaryAccent = Color(0xFF6EA8FE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Λ",
                            fontWeight = FontWeight.Black,
                            color = primaryAccent,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customize Widget",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEAECEF),
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg
                )
            )
        },
        containerColor = darkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Card
            Text(
                text = "NEUMORPHIC PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E95A8),
                letterSpacing = 1.sp
            )
            WidgetPreviewBox(
                target = selectedTarget,
                theme = selectedTheme,
                accent = selectedAccent,
                accounts = accounts,
                showWeekly = showWeekly
            )

            // Section 1: Account Selection
            Text(
                text = "ACCOUNT TO DISPLAY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E95A8),
                letterSpacing = 1.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Option 1: Active Account (Dynamic)
                AccountOptionCard(
                    title = "Active Account (Follows App)",
                    subtitle = "Automatically displays whichever account is active",
                    icon = Icons.Default.Sync,
                    isSelected = selectedTarget == WidgetConfigManager.TARGET_ACTIVE,
                    onClick = { selectedTarget = WidgetConfigManager.TARGET_ACTIVE }
                )

                // Option 2: All Accounts Overview (Multi-Account)
                if (accounts.size > 1) {
                    AccountOptionCard(
                        title = "All Accounts (Multi-Account)",
                        subtitle = "Shows combined overview of all ${accounts.size} accounts",
                        icon = Icons.Default.Dashboard,
                        isSelected = selectedTarget == WidgetConfigManager.TARGET_ALL,
                        onClick = { selectedTarget = WidgetConfigManager.TARGET_ALL }
                    )
                }

                // Individual Accounts
                accounts.forEach { account ->
                    AccountOptionCard(
                        title = account.label,
                        subtitle = account.email ?: (if (account.isActive) "Currently active" else "Saved account"),
                        icon = Icons.Default.Person,
                        isSelected = selectedTarget == account.id,
                        onClick = { selectedTarget = account.id }
                    )
                }
            }

            // Section 2: Neumorphic Theme
            Text(
                text = "NEUMORPHIC THEME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E95A8),
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WidgetTheme.values().forEach { theme ->
                    ThemeSelectionCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTheme = theme }
                    )
                }
            }

            // Section 3: Accent Color
            Text(
                text = "ACCENT COLOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E95A8),
                letterSpacing = 1.sp
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WidgetAccent.values().forEach { accent ->
                    AccentChip(
                        accent = accent,
                        isSelected = selectedAccent == accent,
                        onClick = { selectedAccent = accent }
                    )
                }
            }

            // Section 4: Display Options
            Text(
                text = "METRICS & DENSITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E95A8),
                letterSpacing = 1.sp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, strokeColor, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleRow(
                    title = "Show Weekly Limit",
                    subtitle = "Displays weekly model consumption alongside 5h window",
                    checked = showWeekly,
                    onCheckedChange = { showWeekly = it }
                )
                ToggleRow(
                    title = "Show Reset Countdowns",
                    subtitle = "Shows human-friendly reset times (e.g. Resets Wed 9 AM)",
                    checked = showResetTime,
                    onCheckedChange = { showResetTime = it }
                )
                ToggleRow(
                    title = "Compact Density",
                    subtitle = "Reduces vertical padding for high-density home screens",
                    checked = compactDensity,
                    onCheckedChange = { compactDensity = it }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    val updated = WidgetConfig(
                        targetAccountId = selectedTarget,
                        themeName = selectedTheme.name,
                        accentName = selectedAccent.name,
                        showWeekly = showWeekly,
                        showResetTime = showResetTime,
                        compactDensity = compactDensity
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(selectedAccent.primaryColor)
                )
            ) {
                Text(
                    text = "Apply & Save Widget",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F111A),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WidgetPreviewBox(
    target: String,
    theme: WidgetTheme,
    accent: WidgetAccent,
    accounts: List<AntigravityAccount>,
    showWeekly: Boolean
) {
    val targetAccount = when (target) {
        WidgetConfigManager.TARGET_ALL -> accounts.firstOrNull()
        WidgetConfigManager.TARGET_ACTIVE -> accounts.find { it.isActive } ?: accounts.firstOrNull()
        else -> accounts.find { it.id == target } ?: accounts.firstOrNull()
    }
    val label = when (target) {
        WidgetConfigManager.TARGET_ALL -> "All Accounts (${accounts.size})"
        else -> targetAccount?.label ?: "Account"
    }
    val gPct = targetAccount?.gemini5hPct ?: 85
    val cPct = targetAccount?.claude5hPct ?: 62

    val cardBg = when (theme) {
        WidgetTheme.OBSIDIAN -> Color(0xFF1B1D26)
        WidgetTheme.SLATE -> Color(0xFF222633)
        WidgetTheme.CYBER -> Color(0xFF12141F)
    }
    val borderColor = when (theme) {
        WidgetTheme.OBSIDIAN -> Color(0xFF2E3345)
        WidgetTheme.SLATE -> Color(0xFF3B4459)
        WidgetTheme.CYBER -> Color(0xFF00E5FF)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Λ", fontWeight = FontWeight.Black, color = Color(accent.primaryColor), fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Antigravity · $label",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAECEF),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262A38))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "🔄", fontSize = 10.sp)
                }
            }

            // Gemini Inset Card
            PreviewInsetModelCard(
                modelName = "Gemini Pro",
                percentage = gPct,
                color = Color(accent.primaryColor),
                showWeekly = showWeekly,
                weeklyPct = targetAccount?.geminiWeeklyPct ?: 92
            )

            // Claude Inset Card
            PreviewInsetModelCard(
                modelName = "Claude Sonnet",
                percentage = cPct,
                color = Color(accent.secondaryColor),
                showWeekly = showWeekly,
                weeklyPct = targetAccount?.claudeWeeklyPct ?: 74
            )
        }
    }
}

@Composable
private fun PreviewInsetModelCard(
    modelName: String,
    percentage: Int,
    color: Color,
    showWeekly: Boolean,
    weeklyPct: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13141C))
            .border(0.8.dp, Color(0xFF222634), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = modelName, fontWeight = FontWeight.Bold, color = Color(0xFFEAECEF), fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "$percentage%", fontWeight = FontWeight.Black, color = color, fontSize = 11.sp)
            }
            // Sunken track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0A0B10))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage.coerceIn(0, 100) / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
            if (showWeekly) {
                Text(
                    text = "Weekly: $weeklyPct% remaining",
                    color = Color(0xFF7A8094),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun AccountOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF202638) else Color(0xFF1B1D28)
    val border = if (isSelected) Color(0xFF4D7CFE) else Color(0xFF2E3345)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF6EA8FE) else Color(0xFF8E95A8),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAECEF),
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E95A8),
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4D7CFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    theme: WidgetTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF6EA8FE) else Color(0xFF2E3345)
    val cardBg = when (theme) {
        WidgetTheme.OBSIDIAN -> Color(0xFF1B1D26)
        WidgetTheme.SLATE -> Color(0xFF252A38)
        WidgetTheme.CYBER -> Color(0xFF10121A)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = theme.label,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF6EA8FE) else Color(0xFFEAECEF),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AccentChip(
    accent: WidgetAccent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color.White else Color(0xFF2E3345)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B1D28))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(accent.primaryColor))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = accent.label,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFEAECEF),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFFEAECEF), fontSize = 12.sp)
            Text(text = subtitle, color = Color(0xFF8E95A8), fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4D7CFE)
            )
        )
    }
}

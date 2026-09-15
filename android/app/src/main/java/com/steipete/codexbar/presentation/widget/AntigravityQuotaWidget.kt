package com.steipete.codexbar.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.steipete.codexbar.CodexBarApp
import com.steipete.codexbar.MainActivity
import com.steipete.codexbar.R
import com.steipete.codexbar.domain.model.AntigravityAccount
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modern Neumorphic Android Widget for Antigravity & AI Quotas.
 * Features:
 * - Dedicated tailored layouts for 5 distinct size categories (Compact Mini, Wide Bar, 2x2 Square, Tall Vertical, Expanded Dashboard).
 * - Multi-account support with initial configuration selector and interactive in-widget account cycling.
 * - Neumorphism Soft UI aesthetics (extruded outer cards, sunken carved modules, grooved progress tracks, tactile embossed controls).
 * - High-density information design with zero awkward empty spaces.
 */
class AntigravityQuotaWidget : GlanceAppWidget() {

    companion object {
        // Size breakpoints for responsive layouts
        private val SIZE_MINI = DpSize(100.dp, 50.dp)          // 1x1 / small 2x1
        private val SIZE_WIDE_BAR = DpSize(200.dp, 55.dp)      // 3x1 / 4x1 wide bar
        private val SIZE_SQUARE_2X2 = DpSize(130.dp, 105.dp)   // 2x2 square tile
        private val SIZE_TALL_VERTICAL = DpSize(130.dp, 180.dp)// 2x3 / 2x4 tall vertical
        private val SIZE_EXPANDED = DpSize(230.dp, 110.dp)     // 4x2+ expanded dashboard

        // Base Neumorphic color palette
        val COLOR_WHITE = ColorProvider(Color(0xFFF2F4F8))
        val COLOR_MUTED = ColorProvider(Color(0xFF8E95A8))
        val COLOR_SUBTLE = ColorProvider(Color(0xFF5E6578))
        val COLOR_TRACK_BG = ColorProvider(Color(0xFF0C0D13))
        val COLOR_WARNING = ColorProvider(Color(0xFFFFC53D))
        val COLOR_CRITICAL = ColorProvider(Color(0xFFFF5252))

        fun statusColor(pct: Int, defaultColor: ColorProvider): ColorProvider = when {
            pct > 40 -> defaultColor
            pct > 15 -> COLOR_WARNING
            else -> COLOR_CRITICAL
        }

        fun formatResetDate(epochMs: Long?, now: Long = System.currentTimeMillis()): String? {
            if (epochMs == null || epochMs <= now) return null
            return try {
                val instant = Instant.ofEpochMilli(epochMs)
                val zone = ZoneId.systemDefault()
                val zonedDateTime = instant.atZone(zone)
                val today = LocalDate.now(zone)
                val resetDate = zonedDateTime.toLocalDate()
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                val formattedTime = zonedDateTime.format(timeFormatter)

                when {
                    resetDate.isEqual(today) -> "Resets Today $formattedTime"
                    resetDate.isEqual(today.plusDays(1)) -> "Resets Tomorrow $formattedTime"
                    else -> {
                        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
                        val dayStr = zonedDateTime.format(dayFormatter)
                        "Resets $dayStr $formattedTime"
                    }
                }
            } catch (_: Exception) {
                val deltaMin = (epochMs - now) / 60_000L
                if (deltaMin > 60) "Resets in ${deltaMin / 60}h" else "Resets in ${deltaMin}m"
            }
        }

        fun formatRelativeSync(epochMs: Long): String {
            if (epochMs <= 0) return "Just now"
            val diffSec = (System.currentTimeMillis() - epochMs) / 1000
            return when {
                diffSec < 60 -> "Just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                diffSec < 86400 -> "${diffSec / 3600}h ago"
                else -> "Synced"
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_MINI, SIZE_WIDE_BAR, SIZE_SQUARE_2X2, SIZE_TALL_VERTICAL, SIZE_EXPANDED)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val config = WidgetConfigManager.getConfig(context, appWidgetId)
        val resolvedAccounts = WidgetConfigManager.resolveTargetAccounts(context, config.targetAccountId)
        val allAccounts = WidgetConfigManager.getSavedAccounts(context)
        val primaryAccount = resolvedAccounts.firstOrNull() ?: AntigravityAccount(label = "Default", token = "")

        val gemini5hPct = primaryAccount.gemini5hPct ?: 100
        val geminiWeeklyPct = primaryAccount.geminiWeeklyPct ?: 100
        val claude5hPct = primaryAccount.claude5hPct ?: 100
        val claudeWeeklyPct = primaryAccount.claudeWeeklyPct ?: 100

        val geminiResetStr = formatResetDate(primaryAccount.gemini5hResetEpoch)
        val claudeResetStr = formatResetDate(primaryAccount.claude5hResetEpoch)
        val lastSyncStr = formatRelativeSync(primaryAccount.lastSyncEpochMs)

        // Determine Neumorphic background drawable based on chosen theme
        val bgDrawable = when (config.theme) {
            WidgetTheme.OBSIDIAN -> R.drawable.widget_neumorph_bg_card
            WidgetTheme.SLATE -> R.drawable.widget_neumorph_bg_slate
            WidgetTheme.CYBER -> R.drawable.widget_neumorph_bg_cyber
        }

        provideContent {
            val size = LocalSize.current
            val isTall = size.height >= 85.dp
            val isVeryTall = size.height >= 165.dp
            val isWide = size.width >= 170.dp
            val isVeryWide = size.width >= 230.dp

            when {
                // Category 5: Expanded Dashboard (W >= 230dp, H >= 100dp)
                isVeryWide && isTall -> ExpandedDashboardLayout(
                    config = config,
                    primaryAccount = primaryAccount,
                    allAccounts = allAccounts,
                    isMultiAccount = config.targetAccountId == WidgetConfigManager.TARGET_ALL && allAccounts.size > 1,
                    gemini5hPct = gemini5hPct,
                    geminiWeeklyPct = geminiWeeklyPct,
                    claude5hPct = claude5hPct,
                    claudeWeeklyPct = claudeWeeklyPct,
                    geminiResetStr = geminiResetStr,
                    claudeResetStr = claudeResetStr,
                    lastSyncStr = lastSyncStr,
                    bgDrawable = bgDrawable
                )

                // Category 4: Tall Vertical (W < 230dp, H >= 165dp)
                !isVeryWide && isVeryTall -> TallVerticalLayout(
                    config = config,
                    account = primaryAccount,
                    gemini5hPct = gemini5hPct,
                    geminiWeeklyPct = geminiWeeklyPct,
                    claude5hPct = claude5hPct,
                    claudeWeeklyPct = claudeWeeklyPct,
                    geminiResetStr = geminiResetStr,
                    claudeResetStr = claudeResetStr,
                    lastSyncStr = lastSyncStr,
                    bgDrawable = bgDrawable
                )

                // Category 3: Balanced 2x2 Square Widget (W < 230dp, H 85dp..165dp)
                !isVeryWide && isTall -> NeumorphicSquare2x2Layout(
                    config = config,
                    account = primaryAccount,
                    gemini5hPct = gemini5hPct,
                    geminiWeeklyPct = geminiWeeklyPct,
                    claude5hPct = claude5hPct,
                    claudeWeeklyPct = claudeWeeklyPct,
                    geminiResetStr = geminiResetStr,
                    claudeResetStr = claudeResetStr,
                    lastSyncStr = lastSyncStr,
                    bgDrawable = bgDrawable
                )

                // Category 2: Wide Slim Bar (W >= 170dp, H < 85dp)
                isWide && !isTall -> WideBarLayout(
                    config = config,
                    account = primaryAccount,
                    gemini5hPct = gemini5hPct,
                    claude5hPct = claude5hPct,
                    geminiResetStr = geminiResetStr,
                    claudeResetStr = claudeResetStr
                )

                // Category 1: Compact Mini / 1x1 (W < 170dp, H < 85dp)
                else -> CompactMiniLayout(
                    config = config,
                    account = primaryAccount,
                    gemini5hPct = gemini5hPct,
                    claude5hPct = claude5hPct
                )
            }
        }
    }

    // =========================================================================
    // LAYOUT 1: Compact Mini (1x1 or 2x1 slim)
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun CompactMiniLayout(
        config: WidgetConfig,
        account: AntigravityAccount,
        gemini5hPct: Int,
        claude5hPct: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_neumorph_bg_compact))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header with mini brand and interactive account switcher
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(
                            color = config.geminiColorProvider,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = account.label.take(7),
                        style = TextStyle(
                            color = COLOR_WHITE,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<CycleWidgetAccountCallback>())
                            .defaultWeight()
                    )
                    Text(
                        text = "🔄",
                        style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp),
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(1.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Text(
                        text = "⚙️",
                        style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp),
                        modifier = GlanceModifier
                            .clickable(actionStartActivity<AntigravityWidgetConfigureActivity>())
                            .padding(1.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Gemini compact meter
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "G",
                        style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = gemini5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.defaultWeight().height(4.dp),
                        color = statusColor(gemini5hPct, config.geminiColorProvider),
                        backgroundColor = COLOR_TRACK_BG
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "$gemini5hPct%",
                        style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.height(3.dp))

                // Claude compact meter
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "C",
                        style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = claude5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.defaultWeight().height(4.dp),
                        color = statusColor(claude5hPct, config.claudeColorProvider),
                        backgroundColor = COLOR_TRACK_BG
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "$claude5hPct%",
                        style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // =========================================================================
    // LAYOUT 2: Wide Slim Bar (3x1 / 4x1)
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun WideBarLayout(
        config: WidgetConfig,
        account: AntigravityAccount,
        gemini5hPct: Int,
        claude5hPct: Int,
        geminiResetStr: String?,
        claudeResetStr: String?
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_neumorph_bg_compact))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand + Interactive Account Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<CycleWidgetAccountCallback>())
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(
                            color = config.geminiColorProvider,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Column {
                        Text(
                            text = account.label.take(10),
                            style = TextStyle(color = COLOR_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "⇄ Switch",
                            style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                        )
                    }
                }

                // Gemini Module
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$gemini5hPct%",
                            style = TextStyle(
                                color = statusColor(gemini5hPct, config.geminiColorProvider),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = gemini5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(gemini5hPct, config.geminiColorProvider),
                        backgroundColor = COLOR_TRACK_BG
                    )
                    if (config.showResetTime && geminiResetStr != null) {
                        Spacer(modifier = GlanceModifier.height(1.dp))
                        Text(text = geminiResetStr, style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Claude Module
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claude",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$claude5hPct%",
                            style = TextStyle(
                                color = statusColor(claude5hPct, config.claudeColorProvider),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = claude5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(claude5hPct, config.claudeColorProvider),
                        backgroundColor = COLOR_TRACK_BG
                    )
                    if (config.showResetTime && claudeResetStr != null) {
                        Spacer(modifier = GlanceModifier.height(1.dp))
                        Text(text = claudeResetStr, style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Tactile Refresh Button
                Text(
                    text = "🔄",
                    style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshWidgetCallback>())
                        .padding(2.dp)
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                // Tactile Settings Button
                Text(
                    text = "⚙️",
                    style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionStartActivity<AntigravityWidgetConfigureActivity>())
                        .padding(2.dp)
                )
            }
        }
    }

    // =========================================================================
    // LAYOUT 3: Balanced 2x2 Square Widget (Core daily driver)
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun NeumorphicSquare2x2Layout(
        config: WidgetConfig,
        account: AntigravityAccount,
        gemini5hPct: Int,
        geminiWeeklyPct: Int,
        claude5hPct: Int,
        claudeWeeklyPct: Int,
        geminiResetStr: String?,
        claudeResetStr: String?,
        lastSyncStr: String,
        bgDrawable: Int
    ) {
        val vertPadding = if (config.compactDensity) 8.dp else 10.dp
        val horizPadding = if (config.compactDensity) 9.dp else 11.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(bgDrawable))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = horizPadding, vertical = vertPadding)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header: Logo + Interactive Account Chip + Refresh Button + Settings Button
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(
                            color = config.geminiColorProvider,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))

                    // Tactile Account Pill (tapping cycles account)
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_chip))
                            .clickable(actionRunCallback<CycleWidgetAccountCallback>())
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = account.label.take(10),
                            style = TextStyle(
                                color = COLOR_WHITE,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Text(
                            text = "⇄",
                            style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    // Tactile Refresh Button
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔄",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 10.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(3.dp))

                    // Tactile Settings / Customization Button
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionStartActivity<AntigravityWidgetConfigureActivity>())
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚙️",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 10.sp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Gemini Inset Sunken Module
                NeumorphicModelInsetCard(
                    title = "Gemini Pro",
                    percentage = gemini5hPct,
                    progressColor = statusColor(gemini5hPct, config.geminiColorProvider),
                    resetStr = if (config.showResetTime) geminiResetStr else null,
                    weeklyPct = if (config.showWeekly) geminiWeeklyPct else null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(5.dp))

                // Claude Inset Sunken Module
                NeumorphicModelInsetCard(
                    title = "Claude Sonnet",
                    percentage = claude5hPct,
                    progressColor = statusColor(claude5hPct, config.claudeColorProvider),
                    resetStr = if (config.showResetTime) claudeResetStr else null,
                    weeklyPct = if (config.showWeekly) claudeWeeklyPct else null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Footer Status Row (densely fills space with zero gaps)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!account.plan.isNullOrBlank()) account.plan!! else "Antigravity",
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = lastSyncStr,
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                    )
                }
            }
        }
    }

    // =========================================================================
    // LAYOUT 4: Tall Vertical Widget (2x3 / 2x4)
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun TallVerticalLayout(
        config: WidgetConfig,
        account: AntigravityAccount,
        gemini5hPct: Int,
        geminiWeeklyPct: Int,
        claude5hPct: Int,
        claudeWeeklyPct: Int,
        geminiResetStr: String?,
        claudeResetStr: String?,
        lastSyncStr: String,
        bgDrawable: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(bgDrawable))
                .clickable(actionStartActivity<MainActivity>())
                .padding(10.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ Antigravity",
                        style = TextStyle(
                            color = config.geminiColorProvider,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_chip))
                            .clickable(actionRunCallback<CycleWidgetAccountCallback>())
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${account.label.take(8)} ⇄",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = "🔄", style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp))
                    }
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionStartActivity<AntigravityWidgetConfigureActivity>())
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = "⚙️", style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Gemini 5h Card
                NeumorphicModelInsetCard(
                    title = "Gemini (5h Limit)",
                    percentage = gemini5hPct,
                    progressColor = statusColor(gemini5hPct, config.geminiColorProvider),
                    resetStr = geminiResetStr,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(5.dp))

                // Gemini Weekly Card
                NeumorphicModelInsetCard(
                    title = "Gemini (Weekly)",
                    percentage = geminiWeeklyPct,
                    progressColor = statusColor(geminiWeeklyPct, config.geminiColorProvider),
                    resetStr = null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(5.dp))

                // Claude 5h Card
                NeumorphicModelInsetCard(
                    title = "Claude (5h Limit)",
                    percentage = claude5hPct,
                    progressColor = statusColor(claude5hPct, config.claudeColorProvider),
                    resetStr = claudeResetStr,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(5.dp))

                // Claude Weekly Card
                NeumorphicModelInsetCard(
                    title = "Claude (Weekly)",
                    percentage = claudeWeeklyPct,
                    progressColor = statusColor(claudeWeeklyPct, config.claudeColorProvider),
                    resetStr = null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Footer
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.email ?: "CodexBar",
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(text = lastSyncStr, style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp))
                }
            }
        }
    }

    // =========================================================================
    // LAYOUT 5: Expanded Dashboard (4x2 / 4x3 / Tablet)
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun ExpandedDashboardLayout(
        config: WidgetConfig,
        primaryAccount: AntigravityAccount,
        allAccounts: List<AntigravityAccount>,
        isMultiAccount: Boolean,
        gemini5hPct: Int,
        geminiWeeklyPct: Int,
        claude5hPct: Int,
        claudeWeeklyPct: Int,
        geminiResetStr: String?,
        claudeResetStr: String?,
        lastSyncStr: String,
        bgDrawable: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(bgDrawable))
                .clickable(actionStartActivity<MainActivity>())
                .padding(11.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(
                            color = config.geminiColorProvider,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = if (isMultiAccount) "Antigravity Multi-Account" else "Antigravity · ${primaryAccount.label}",
                        style = TextStyle(color = COLOR_WHITE, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    // Account Cycle chip
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_chip))
                            .clickable(actionRunCallback<CycleWidgetAccountCallback>())
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isMultiAccount) "All Accounts ⇄" else "${primaryAccount.label.take(8)} ⇄",
                            style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    // Refresh button
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(text = "🔄", style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp))
                    }

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    // Settings / Customize button
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_neumorph_button))
                            .clickable(actionStartActivity<AntigravityWidgetConfigureActivity>())
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(text = "⚙️", style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (isMultiAccount) {
                    // Multi-Account Grid view: Renders each account side by side!
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        allAccounts.take(3).forEachIndexed { index, acc ->
                            if (index > 0) Spacer(modifier = GlanceModifier.width(8.dp))
                            MultiAccountSummaryCard(
                                account = acc,
                                config = config,
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            )
                        }
                    }
                } else {
                    // Single Account Deep Inspection: 2 side-by-side Neumorphic columns
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Gemini Deep Panel
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(ImageProvider(R.drawable.widget_neumorph_inset_card))
                                .padding(9.dp)
                        ) {
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gemini Models",
                                        style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    Text(
                                        text = "$gemini5hPct%",
                                        style = TextStyle(
                                            color = statusColor(gemini5hPct, config.geminiColorProvider),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = gemini5hPct.coerceIn(0, 100) / 100f,
                                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                    color = statusColor(gemini5hPct, config.geminiColorProvider),
                                    backgroundColor = COLOR_TRACK_BG
                                )
                                if (geminiResetStr != null) {
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    Text(text = geminiResetStr, style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp))
                                }

                                if (config.showWeekly) {
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Weekly Quota", style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp))
                                        Spacer(modifier = GlanceModifier.defaultWeight())
                                        Text(
                                            text = "$geminiWeeklyPct%",
                                            style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = geminiWeeklyPct.coerceIn(0, 100) / 100f,
                                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                        color = statusColor(geminiWeeklyPct, config.geminiColorProvider),
                                        backgroundColor = COLOR_TRACK_BG
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Claude Deep Panel
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(ImageProvider(R.drawable.widget_neumorph_inset_card))
                                .padding(9.dp)
                        ) {
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Claude & GPT",
                                        style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    Text(
                                        text = "$claude5hPct%",
                                        style = TextStyle(
                                            color = statusColor(claude5hPct, config.claudeColorProvider),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = claude5hPct.coerceIn(0, 100) / 100f,
                                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                    color = statusColor(claude5hPct, config.claudeColorProvider),
                                    backgroundColor = COLOR_TRACK_BG
                                )
                                if (claudeResetStr != null) {
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    Text(text = claudeResetStr, style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp))
                                }

                                if (config.showWeekly) {
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Weekly Quota", style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp))
                                        Spacer(modifier = GlanceModifier.defaultWeight())
                                        Text(
                                            text = "$claudeWeeklyPct%",
                                            style = TextStyle(color = COLOR_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = claudeWeeklyPct.coerceIn(0, 100) / 100f,
                                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                        color = statusColor(claudeWeeklyPct, config.claudeColorProvider),
                                        backgroundColor = COLOR_TRACK_BG
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Footer Bar
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!primaryAccount.plan.isNullOrBlank()) "Plan: ${primaryAccount.plan}" else "Active Account",
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = lastSyncStr,
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                    )
                }
            }
        }
    }

    // =========================================================================
    // REUSABLE NEUMORPHIC COMPONENTS
    // =========================================================================
    @androidx.compose.runtime.Composable
    private fun NeumorphicModelInsetCard(
        title: String,
        percentage: Int,
        progressColor: ColorProvider,
        resetStr: String?,
        weeklyPct: Int? = null,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .background(ImageProvider(R.drawable.widget_neumorph_inset_card))
                .padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Percentage badge
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = COLOR_WHITE,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$percentage%",
                        style = TextStyle(
                            color = progressColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Sunken Track + Glow Progress Indicator
                LinearProgressIndicator(
                    progress = percentage.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                    color = progressColor,
                    backgroundColor = COLOR_TRACK_BG
                )

                // Subtitle metadata (reset string or weekly quota)
                if (resetStr != null || weeklyPct != null) {
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (resetStr != null) {
                            Text(
                                text = resetStr,
                                style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp)
                            )
                        }
                        if (weeklyPct != null) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "W: $weeklyPct%",
                                style = TextStyle(color = COLOR_MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MultiAccountSummaryCard(
        account: AntigravityAccount,
        config: WidgetConfig,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val gPct = account.gemini5hPct ?: 100
        val cPct = account.claude5hPct ?: 100

        Box(
            modifier = modifier
                .background(ImageProvider(R.drawable.widget_neumorph_inset_card))
                .padding(8.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = account.label,
                    style = TextStyle(color = COLOR_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                if (!account.email.isNullOrBlank()) {
                    Text(
                        text = account.email,
                        style = TextStyle(color = COLOR_SUBTLE, fontSize = 8.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.height(6.dp))

                // Gemini Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Gemini", style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$gPct%",
                        style = TextStyle(
                            color = statusColor(gPct, config.geminiColorProvider),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                LinearProgressIndicator(
                    progress = gPct.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = statusColor(gPct, config.geminiColorProvider),
                    backgroundColor = COLOR_TRACK_BG
                )

                Spacer(modifier = GlanceModifier.height(5.dp))

                // Claude Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Claude", style = TextStyle(color = COLOR_MUTED, fontSize = 9.sp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$cPct%",
                        style = TextStyle(
                            color = statusColor(cPct, config.claudeColorProvider),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                LinearProgressIndicator(
                    progress = cPct.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = statusColor(cPct, config.claudeColorProvider),
                    backgroundColor = COLOR_TRACK_BG
                )
            }
        }
    }
}

/**
 * Interactive callback when user taps the account chip on the widget.
 * Cycles to the next saved account (or all-accounts mode) and refreshes the widget immediately!
 */
class CycleWidgetAccountCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            WidgetConfigManager.cycleAccount(context, appWidgetId)
            AntigravityQuotaWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

/**
 * Interactive callback when user taps the tactile refresh icon on the widget.
 * Queries Google/Antigravity API endpoints in the background and re-renders the widget with fresh quotas.
 */
class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val app = context.applicationContext as? CodexBarApp
            val container = app?.container
            if (container != null) {
                val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
                val config = WidgetConfigManager.getConfig(context, appWidgetId)
                val targetAccounts = WidgetConfigManager.resolveTargetAccounts(context, config.targetAccountId)

                for (account in targetAccounts) {
                    val token = account.token.ifBlank {
                        container.secureStorage.getApiKey(UsageProvider.ANTIGRAVITY) ?: ""
                    }
                    if (token.isNotBlank()) {
                        container.usageRepository.refreshUsage(
                            UsageProvider.ANTIGRAVITY,
                            ProviderCredentials(apiKey = token)
                        )
                    }
                }
            }
            AntigravityQuotaWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

class AntigravityQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AntigravityQuotaWidget()
}

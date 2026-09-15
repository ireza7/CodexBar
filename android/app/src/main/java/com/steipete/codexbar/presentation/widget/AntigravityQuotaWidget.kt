package com.steipete.codexbar.presentation.widget

import android.content.Context
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
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AntigravityQuotaWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT_MINI = DpSize(110.dp, 40.dp)       // 2x1 slim
        private val WIDE_BAR = DpSize(210.dp, 40.dp)           // 4x1 wide
        private val SQUARE_TILE = DpSize(120.dp, 100.dp)       // 2x2 square (Primary screenshot style)
        private val EXPANDED_DASHBOARD = DpSize(220.dp, 100.dp)// 4x2 full

        private val SOFT_WHITE = ColorProvider(Color(0xFFEAEAEE))
        private val DIM_LABEL = ColorProvider(Color(0xFF8F92A1))
        private val GEMINI_ACCENT = ColorProvider(Color(0xFF6EA8FE))
        private val CLAUDE_ACCENT = ColorProvider(Color(0xFFFF9F0A))
        private val TRACK_BG = ColorProvider(Color(0xFF1B1C24))

        private fun statusColor(pct: Int, defaultTint: ColorProvider): ColorProvider = when {
            pct > 40 -> defaultTint
            pct > 15 -> ColorProvider(Color(0xFFFFBF60))  // Warning amber
            else -> ColorProvider(Color(0xFFFF6B6B))       // Critical red
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
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_MINI, WIDE_BAR, SQUARE_TILE, EXPANDED_DASHBOARD)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)

        val accountLabel = prefs.getString("active_account_label", null)
        val g5hPct = prefs.getInt(
            "gemini_5h_pct",
            prefs.getString("gemini_5h_text", "100")?.replace("%", "")?.toIntOrNull() ?: 100
        )
        val gWPct = prefs.getInt(
            "gemini_weekly_pct",
            prefs.getString("gemini_weekly_text", "100")?.replace("%", "")?.toIntOrNull() ?: 100
        )
        val c5hPct = prefs.getInt(
            "claude_5h_pct",
            prefs.getString("claude_5h_text", "100")?.replace("%", "")?.toIntOrNull() ?: 100
        )
        val cWPct = prefs.getInt(
            "claude_weekly_pct",
            prefs.getString("claude_weekly_text", "100")?.replace("%", "")?.toIntOrNull() ?: 100
        )

        val g5hReset = prefs.getLong("gemini_5h_reset", 0L)
        val c5hReset = prefs.getLong("claude_5h_reset", 0L)
        val gResetStr = formatResetDate(g5hReset)
        val cResetStr = formatResetDate(c5hReset)

        provideContent {
            val size = LocalSize.current
            val isTall = size.height >= 60.dp
            val isVeryWide = size.width >= 320.dp

            when {
                // Tall widgets (2x2, 2x3, 3x2, 3x3, 4x2 phone):
                // Directly renders the sleek layout matching the user's screenshot
                isTall && !isVeryWide -> ScreenshotStyle2x2Layout(
                    accountLabel = accountLabel,
                    geminiName = "Gemini Pro",
                    geminiPct = g5hPct,
                    geminiResetStr = gResetStr,
                    claudeName = "Claude",
                    claudePct = c5hPct,
                    claudeResetStr = cResetStr
                )
                // Tall and very wide (tablet landscape):
                isTall && isVeryWide -> ExpandedDashboardLayout(
                    accountLabel = accountLabel,
                    g5hPct = g5hPct,
                    gWPct = gWPct,
                    c5hPct = c5hPct,
                    cWPct = cWPct,
                    gResetStr = gResetStr,
                    cResetStr = cResetStr
                )
                // Short and wide (3x1 or 4x1 slim bar):
                size.width >= 170.dp -> WideBarLayout(
                    accountLabel = accountLabel,
                    g5hPct = g5hPct,
                    c5hPct = c5hPct,
                    gResetStr = gResetStr,
                    cResetStr = cResetStr
                )
                // Compact mini (2x1):
                else -> CompactMiniLayout(
                    accountLabel = accountLabel,
                    g5hPct = g5hPct,
                    c5hPct = c5hPct
                )
            }
        }
    }

    /**
     * Iconic 2x2 Widget Layout designed from the user's uploaded reference:
     * - Frosted dark card with 22dp smooth corners
     * - Stylized 'A' logo mark + "Antigravity" + optional active account indicator
     * - Instant tap-to-refresh icon '🔄'
     * - Rounded inset cards for Gemini and Claude
     * - 6dp rounded progress bars emptying as quota is consumed
     * - Human-friendly reset dates (e.g. "Resets Wed 9:00 AM", "Resets Fri 12:00 PM")
     */
    @androidx.compose.runtime.Composable
    private fun ScreenshotStyle2x2Layout(
        accountLabel: String?,
        geminiName: String,
        geminiPct: Int,
        geminiResetStr: String?,
        claudeName: String,
        claudePct: Int,
        claudeResetStr: String?
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_card))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header: Logo 'A', Title, Account badge, Refresh icon
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(
                            color = SOFT_WHITE,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = if (!accountLabel.isNullOrBlank()) "Antigravity · $accountLabel" else "Antigravity",
                        style = TextStyle(
                            color = SOFT_WHITE,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    // Refresh Button (triggers background update immediately)
                    Text(
                        text = "🔄",
                        style = TextStyle(color = SOFT_WHITE, fontSize = 13.sp),
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Gemini Inset Card
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ImageProvider(R.drawable.widget_card_inset))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = geminiName,
                                style = TextStyle(
                                    color = SOFT_WHITE,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "$geminiPct%",
                                style = TextStyle(
                                    color = SOFT_WHITE,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = geminiPct.coerceIn(0, 100) / 100f,
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = statusColor(geminiPct, GEMINI_ACCENT),
                            backgroundColor = TRACK_BG
                        )
                        if (geminiResetStr != null) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = geminiResetStr,
                                style = TextStyle(color = DIM_LABEL, fontSize = 9.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Claude Inset Card
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ImageProvider(R.drawable.widget_card_inset))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = claudeName,
                                style = TextStyle(
                                    color = SOFT_WHITE,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "$claudePct%",
                                style = TextStyle(
                                    color = SOFT_WHITE,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = claudePct.coerceIn(0, 100) / 100f,
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = statusColor(claudePct, CLAUDE_ACCENT),
                            backgroundColor = TRACK_BG
                        )
                        if (claudeResetStr != null) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = claudeResetStr,
                                style = TextStyle(color = DIM_LABEL, fontSize = 9.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WideBarLayout(
        accountLabel: String?,
        g5hPct: Int,
        c5hPct: Int,
        gResetStr: String?,
        cResetStr: String?
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_compact))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini Column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini Pro",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$g5hPct%",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = g5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                        color = statusColor(g5hPct, GEMINI_ACCENT),
                        backgroundColor = TRACK_BG
                    )
                    if (gResetStr != null) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(text = gResetStr, style = TextStyle(color = DIM_LABEL, fontSize = 9.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(14.dp))

                // Claude Column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claude",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$c5hPct%",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = c5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                        color = statusColor(c5hPct, CLAUDE_ACCENT),
                        backgroundColor = TRACK_BG
                    )
                    if (cResetStr != null) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(text = cResetStr, style = TextStyle(color = DIM_LABEL, fontSize = 9.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Quick refresh icon
                Text(
                    text = "🔄",
                    style = TextStyle(color = SOFT_WHITE, fontSize = 12.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshWidgetCallback>())
                        .padding(2.dp)
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CompactMiniLayout(
        accountLabel: String?,
        g5hPct: Int,
        c5hPct: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_compact))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini Mini Column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$g5hPct%",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = g5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(g5hPct, GEMINI_ACCENT),
                        backgroundColor = TRACK_BG
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Claude Mini Column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claude",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$c5hPct%",
                            style = TextStyle(color = SOFT_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = c5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(c5hPct, CLAUDE_ACCENT),
                        backgroundColor = TRACK_BG
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ExpandedDashboardLayout(
        accountLabel: String?,
        g5hPct: Int,
        gWPct: Int,
        c5hPct: Int,
        cWPct: Int,
        gResetStr: String?,
        cResetStr: String?
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_card))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Title Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Λ",
                        style = TextStyle(color = SOFT_WHITE, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = if (!accountLabel.isNullOrBlank()) "Antigravity · $accountLabel" else "Antigravity Quota",
                        style = TextStyle(
                            color = SOFT_WHITE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "🔄",
                        style = TextStyle(color = SOFT_WHITE, fontSize = 13.sp),
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Two Inset Cards Side by Side
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Gemini Card
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ImageProvider(R.drawable.widget_card_inset))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Gemini Models",
                                style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // 5-Hour Limit
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "5h Limit", style = TextStyle(color = DIM_LABEL, fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "$g5hPct% rem",
                                    style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = g5hPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                color = statusColor(g5hPct, GEMINI_ACCENT),
                                backgroundColor = TRACK_BG
                            )
                            if (gResetStr != null) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = gResetStr,
                                    style = TextStyle(color = DIM_LABEL, fontSize = 9.sp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // Weekly Limit
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Weekly", style = TextStyle(color = DIM_LABEL, fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "$gWPct% rem",
                                    style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = gWPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                color = statusColor(gWPct, GEMINI_ACCENT),
                                backgroundColor = TRACK_BG
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Claude & GPT Card
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ImageProvider(R.drawable.widget_card_inset))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Claude & GPT",
                                style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // 5-Hour Limit
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "5h Limit", style = TextStyle(color = DIM_LABEL, fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "$c5hPct% rem",
                                    style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = c5hPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                color = statusColor(c5hPct, CLAUDE_ACCENT),
                                backgroundColor = TRACK_BG
                            )
                            if (cResetStr != null) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = cResetStr,
                                    style = TextStyle(color = DIM_LABEL, fontSize = 9.sp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            // Weekly Limit
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Weekly", style = TextStyle(color = DIM_LABEL, fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "$cWPct% rem",
                                    style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = cWPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                color = statusColor(cWPct, CLAUDE_ACCENT),
                                backgroundColor = TRACK_BG
                            )
                        }
                    }
                }
            }
        }
    }
}

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
                val activeAccount = container.accountRepository.getActiveAccount()
                val token = activeAccount?.token ?: container.secureStorage.getApiKey(UsageProvider.ANTIGRAVITY)
                if (!token.isNullOrBlank()) {
                    container.usageRepository.refreshUsage(
                        UsageProvider.ANTIGRAVITY,
                        ProviderCredentials(apiKey = token)
                    )
                }
            }
            AntigravityQuotaWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

class AntigravityQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AntigravityQuotaWidget()
}

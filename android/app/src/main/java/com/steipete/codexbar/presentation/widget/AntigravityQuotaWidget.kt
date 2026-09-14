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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import com.steipete.codexbar.MainActivity
import com.steipete.codexbar.R

class AntigravityQuotaWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT_MINI = DpSize(110.dp, 40.dp)       // 2x1 slim
        private val WIDE_BAR = DpSize(210.dp, 40.dp)           // 4x1 wide
        private val SQUARE_TILE = DpSize(120.dp, 100.dp)       // 2x2 tile
        private val EXPANDED_DASHBOARD = DpSize(220.dp, 100.dp)// 4x2 full

        private val SOFT_WHITE = ColorProvider(Color(0xFFE8E8ED))
        private val DIM_LABEL = ColorProvider(Color(0xFF8E8E93))
        private val GEMINI_ACCENT = ColorProvider(Color(0xFF6EA8FE))
        private val CLAUDE_ACCENT = ColorProvider(Color(0xFFFF9F0A))
        private val TRACK_BG = ColorProvider(Color(0xFF26272D))

        private fun statusColor(pct: Int): ColorProvider = when {
            pct > 40 -> ColorProvider(Color(0xFF5ED87A))  // Healthy green
            pct > 15 -> ColorProvider(Color(0xFFFFBF60))  // Warning amber
            else -> ColorProvider(Color(0xFFFF6B6B))       // Critical red
        }

        private fun formatResetShort(epochMs: Long, now: Long = System.currentTimeMillis()): String? {
            if (epochMs <= now) return null
            val deltaMs = epochMs - now
            val totalMinutes = deltaMs / 60_000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            val days = hours / 24L
            return when {
                days > 0 -> "${days}d ${hours % 24L}h"
                hours > 0 -> "${hours}h ${minutes}m"
                totalMinutes > 0 -> "${totalMinutes}m"
                else -> "<1m"
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_MINI, WIDE_BAR, SQUARE_TILE, EXPANDED_DASHBOARD)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)

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
        val gResetStr = formatResetShort(g5hReset)
        val cResetStr = formatResetShort(c5hReset)

        provideContent {
            val size = LocalSize.current
            val isWide = size.width >= 200.dp
            val isTall = size.height >= 75.dp

            when {
                !isWide && !isTall -> CompactMiniLayout(g5hPct, c5hPct)
                isWide && !isTall -> WideBarLayout(g5hPct, c5hPct, gResetStr, cResetStr)
                !isWide && isTall -> CompactSquareLayout(g5hPct, gWPct, c5hPct, cWPct)
                else -> ExpandedDashboardLayout(g5hPct, gWPct, c5hPct, cWPct, gResetStr, cResetStr)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CompactMiniLayout(
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
                            text = "Gemini",
                            style = TextStyle(color = GEMINI_ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$g5hPct%",
                            style = TextStyle(color = statusColor(g5hPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = g5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                        color = statusColor(g5hPct),
                        backgroundColor = TRACK_BG
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

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
                            style = TextStyle(color = CLAUDE_ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$c5hPct%",
                            style = TextStyle(color = statusColor(c5hPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = c5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                        color = statusColor(c5hPct),
                        backgroundColor = TRACK_BG
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WideBarLayout(
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini Group
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦ Gemini 5h",
                            style = TextStyle(color = GEMINI_ACCENT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$g5hPct%",
                            style = TextStyle(color = statusColor(g5hPct), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        if (gResetStr != null) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "($gResetStr)",
                                style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = g5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(g5hPct),
                        backgroundColor = TRACK_BG
                    )
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Claude Group
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦ Claude 5h",
                            style = TextStyle(color = CLAUDE_ACCENT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$c5hPct%",
                            style = TextStyle(color = statusColor(c5hPct), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        if (cResetStr != null) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "($cResetStr)",
                                style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = c5hPct.coerceIn(0, 100) / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = statusColor(c5hPct),
                        backgroundColor = TRACK_BG
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CompactSquareLayout(
        g5hPct: Int,
        gWPct: Int,
        c5hPct: Int,
        cWPct: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_card))
                .clickable(actionStartActivity<MainActivity>())
                .padding(10.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header
                Text(
                    text = "⚡ Antigravity",
                    style = TextStyle(color = SOFT_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Gemini Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gemini",
                        style = TextStyle(color = GEMINI_ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "5h: $g5hPct%",
                        style = TextStyle(color = statusColor(g5hPct), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Wk: $gWPct%",
                        style = TextStyle(color = statusColor(gWPct), fontSize = 9.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                LinearProgressIndicator(
                    progress = g5hPct.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                    color = statusColor(g5hPct),
                    backgroundColor = TRACK_BG
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Claude Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Claude",
                        style = TextStyle(color = CLAUDE_ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "5h: $c5hPct%",
                        style = TextStyle(color = statusColor(c5hPct), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Wk: $cWPct%",
                        style = TextStyle(color = statusColor(cWPct), fontSize = 9.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                LinearProgressIndicator(
                    progress = c5hPct.coerceIn(0, 100) / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                    color = statusColor(c5hPct),
                    backgroundColor = TRACK_BG
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ExpandedDashboardLayout(
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
                .background(ImageProvider(R.drawable.widget_bg_raised))
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
                        text = "⚡ Antigravity Quota",
                        style = TextStyle(
                            color = SOFT_WHITE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "Tap to Open ↗",
                        style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
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
                                style = TextStyle(color = GEMINI_ACCENT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    style = TextStyle(color = statusColor(g5hPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = g5hPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                color = statusColor(g5hPct),
                                backgroundColor = TRACK_BG
                            )
                            if (gResetStr != null) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "Resets in $gResetStr",
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
                                    style = TextStyle(color = statusColor(gWPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = gWPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                color = statusColor(gWPct),
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
                                style = TextStyle(color = CLAUDE_ACCENT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    style = TextStyle(color = statusColor(c5hPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = c5hPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                color = statusColor(c5hPct),
                                backgroundColor = TRACK_BG
                            )
                            if (cResetStr != null) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "Resets in $cResetStr",
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
                                    style = TextStyle(color = statusColor(cWPct), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = cWPct.coerceIn(0, 100) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                color = statusColor(cWPct),
                                backgroundColor = TRACK_BG
                            )
                        }
                    }
                }
            }
        }
    }
}

class AntigravityQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AntigravityQuotaWidget()
}

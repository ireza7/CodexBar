package com.steipete.codexbar.presentation.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
        private val SOFT_WHITE = ColorProvider(Color(0xFFE0E0E4))
        private val DIM_LABEL = ColorProvider(Color(0xFF8E8E93))
        private val GEMINI_ACCENT = ColorProvider(Color(0xFF6EA8FE))
        private val CLAUDE_ACCENT = ColorProvider(Color(0xFFFFBF60))

        private fun statusColor(pct: Int): ColorProvider = when {
            pct > 50 -> ColorProvider(Color(0xFF5ED87A))  // soft green
            pct > 20 -> ColorProvider(Color(0xFFFFBF60))  // soft amber
            else -> ColorProvider(Color(0xFFFF6B6B))       // soft red
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)
        val g5h = prefs.getString("gemini_5h_text", "--") ?: "--"
        val gW = prefs.getString("gemini_weekly_text", "--") ?: "--"
        val c5h = prefs.getString("claude_5h_text", "--") ?: "--"
        val cW = prefs.getString("claude_weekly_text", "--") ?: "--"

        val g5hPct = g5h.replace("%", "").toIntOrNull() ?: 50
        val gWPct = gW.replace("%", "").toIntOrNull() ?: 50
        val c5hPct = c5h.replace("%", "").toIntOrNull() ?: 50
        val cWPct = cW.replace("%", "").toIntOrNull() ?: 50

        provideContent {
            // Outer Neumorphic Raised Card
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_raised))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(14.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Title
                    Text(
                        text = "⚡ Antigravity",
                        style = TextStyle(
                            color = SOFT_WHITE,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Two Neumorphic Inset Cards Side by Side
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
                                    text = "Gemini",
                                    style = TextStyle(
                                        color = GEMINI_ACCENT,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(6.dp))

                                // 5-Hour
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "5h ",
                                        style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                                    )
                                    Text(
                                        text = g5h,
                                        style = TextStyle(
                                            color = statusColor(g5hPct),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.height(3.dp))

                                // Weekly
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Wk ",
                                        style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                                    )
                                    Text(
                                        text = gW,
                                        style = TextStyle(
                                            color = statusColor(gWPct),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
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
                                    text = "Claude/GPT",
                                    style = TextStyle(
                                        color = CLAUDE_ACCENT,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(6.dp))

                                // 5-Hour
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "5h ",
                                        style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                                    )
                                    Text(
                                        text = c5h,
                                        style = TextStyle(
                                            color = statusColor(c5hPct),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.height(3.dp))

                                // Weekly
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Wk ",
                                        style = TextStyle(color = DIM_LABEL, fontSize = 10.sp)
                                    )
                                    Text(
                                        text = cW,
                                        style = TextStyle(
                                            color = statusColor(cWPct),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
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

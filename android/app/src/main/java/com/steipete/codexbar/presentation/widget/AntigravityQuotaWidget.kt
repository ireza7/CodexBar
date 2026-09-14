package com.steipete.codexbar.presentation.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class AntigravityQuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)
        val gemini5h = prefs.getString("gemini_5h_text", "Active") ?: "Active"
        val geminiWeekly = prefs.getString("gemini_weekly_text", "Active") ?: "Active"
        val claude5h = prefs.getString("claude_5h_text", "Active") ?: "Active"
        val claudeWeekly = prefs.getString("claude_weekly_text", "Active") ?: "Active"

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF161618))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Antigravity Quota",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Gemini Models Group
                    Text(
                        text = "Gemini Models",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFF4285F4)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = "5h: ",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFB0B0B5)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = gemini5h,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF34C759)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(8.dp))
                        Text(
                            text = "Weekly: ",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFB0B0B5)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = geminiWeekly,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF34C759)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Claude & GPT Group
                    Text(
                        text = "Claude & GPT",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFFFF9F0A)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = "5h: ",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFB0B0B5)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = claude5h,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF34C759)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(8.dp))
                        Text(
                            text = "Weekly: ",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFB0B0B5)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = claudeWeekly,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF34C759)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class AntigravityQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AntigravityQuotaWidget()
}

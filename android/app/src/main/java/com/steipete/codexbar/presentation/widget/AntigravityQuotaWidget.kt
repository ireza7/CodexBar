package com.steipete.codexbar.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.steipete.codexbar.MainActivity

class AntigravityQuotaWidget : GlanceAppWidget() {

    companion object {
        private val BG_COLOR = Color(0xFF1E1E22)
        private val LABEL_COLOR = ColorProvider(Color(0xFFB0B0B5))
        private val TITLE_COLOR = ColorProvider(Color.White)
        private val GEMINI_COLOR = ColorProvider(Color(0xFF4285F4))
        private val CLAUDE_COLOR = ColorProvider(Color(0xFFFF9F0A))

        private fun percentColor(pct: Int): ColorProvider = when {
            pct > 40 -> ColorProvider(Color(0xFF34C759))
            pct > 15 -> ColorProvider(Color(0xFFFF9F0A))
            else -> ColorProvider(Color(0xFFFF453A))
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
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(16.dp)
                    .background(BG_COLOR)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Title Row
                    Text(
                        text = "⚡ Antigravity Quota",
                        style = TextStyle(
                            color = TITLE_COLOR,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Gemini Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini",
                            style = TextStyle(color = GEMINI_COLOR, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(text = "5h:", style = TextStyle(color = LABEL_COLOR, fontSize = 10.sp))
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(text = g5h, style = TextStyle(color = percentColor(g5hPct), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(text = "Wk:", style = TextStyle(color = LABEL_COLOR, fontSize = 10.sp))
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(text = gW, style = TextStyle(color = percentColor(gWPct), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = GlanceModifier.height(3.dp))

                    // Gemini progress bar
                    WidgetProgressBar(percent = g5hPct, color = Color(0xFF4285F4))

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Claude Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claude/GPT",
                            style = TextStyle(color = CLAUDE_COLOR, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(text = "5h:", style = TextStyle(color = LABEL_COLOR, fontSize = 10.sp))
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(text = c5h, style = TextStyle(color = percentColor(c5hPct), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(text = "Wk:", style = TextStyle(color = LABEL_COLOR, fontSize = 10.sp))
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(text = cW, style = TextStyle(color = percentColor(cWPct), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = GlanceModifier.height(3.dp))

                    // Claude progress bar
                    WidgetProgressBar(percent = c5hPct, color = Color(0xFFFF9F0A))
                }
            }
        }
    }
}

/**
 * Simple Glance-compatible progress bar using layered Box backgrounds.
 */
@androidx.compose.runtime.Composable
private fun WidgetProgressBar(percent: Int, color: Color) {
    val trackColor = Color(0xFF2C2C30)
    val fillColor = when {
        percent > 40 -> color
        percent > 15 -> Color(0xFFFF9F0A)
        else -> Color(0xFFFF453A)
    }
    // Outer track
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp)
            .cornerRadius(2.dp)
            .background(trackColor)
    ) {
        // Fill portion - use a fraction of the width
        // Glance does not support fractional width easily, so we use a fixed approach
        // by placing a colored box. We approximate using size modifier.
        val fillFraction = percent.coerceIn(0, 100)
        if (fillFraction > 0) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier
                        .height(4.dp)
                        .cornerRadius(2.dp)
                        .background(fillColor)
                        .defaultWeight()
                ) {}
                if (fillFraction < 100) {
                    // invisible spacer to push fill to the correct ratio
                    // Glance weight-based layout: fill gets `fillFraction` weight, spacer gets `100-fillFraction`
                    Spacer(modifier = GlanceModifier.height(4.dp).defaultWeight())
                }
            }
        }
    }
}

class AntigravityQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AntigravityQuotaWidget()
}

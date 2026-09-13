package com.steipete.codexbar.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.steipete.codexbar.presentation.theme.CodexBarColors
import kotlin.math.max
import kotlin.math.min

/**
 * Visual styling configuration for workday boundary tick marks along the progress bar.
 */
enum class WorkdayTickAppearance {
    HIDDEN,
    SUBTLE,
    HIGH_CONTRAST
}

/**
 * Multi-stage progress bar mirroring macOS CodexBar's Canvas-drawn 6dp bar.
 * Features rounded pill caps, quota warning punchouts, workday boundary ticks,
 * and the iconic 3-stripe pace tip indicator (green for reserve/on-track, red for deficit).
 *
 * @param percent Current usage percentage (0f .. 100f).
 * @param tint Fill accent color (usually provider brand color).
 * @param modifier Composable modifier.
 * @param pacePercent Expected usage percentage according to linear workday pace, or null if uncomputed.
 * @param paceOnTop True if consumption is on-track or in reserve (Green stripe); false if in deficit (Red stripe).
 * @param warningMarkerPercents List of percentage thresholds (e.g. 80f, 90f) to punch warning notches.
 * @param workdayMarkerPercents List of percentage marks representing workday schedule milestones.
 * @param workdayTickAppearance Visual contrast for workday markers (HIDDEN, SUBTLE, HIGH_CONTRAST).
 * @param trackColor Background track color (defaults to 22% white translucent track).
 */
@Composable
fun UsageProgressBar(
    percent: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    pacePercent: Float? = null,
    paceOnTop: Boolean = true,
    warningMarkerPercents: List<Float> = emptyList(),
    workdayMarkerPercents: List<Float> = emptyList(),
    workdayTickAppearance: WorkdayTickAppearance = WorkdayTickAppearance.SUBTLE,
    trackColor: Color = CodexBarColors.ProgressTrack
) {
    val clampedPercent = percent.coerceIn(0f, 100f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clampedPercent,
                    range = 0f..100f
                )
            }
    ) {
        val barHeight = size.height
        val barWidth = size.width
        val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

        // 1. Draw Background Track
        drawRoundRect(
            color = trackColor,
            size = Size(barWidth, barHeight),
            cornerRadius = cornerRadius
        )

        // 2. Draw Usage Fill
        val fillWidth = barWidth * (clampedPercent / 100f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                size = Size(min(fillWidth, barWidth), barHeight),
                cornerRadius = cornerRadius
            )
        }

        // 3. Draw Workday Boundary Markers
        if (workdayTickAppearance != WorkdayTickAppearance.HIDDEN) {
            val tickHeight = if (workdayTickAppearance == WorkdayTickAppearance.HIGH_CONTRAST) {
                barHeight
            } else {
                barHeight * 0.5f
            }
            val tickColor = if (workdayTickAppearance == WorkdayTickAppearance.HIGH_CONTRAST) {
                Color.White.copy(alpha = 0.85f)
            } else {
                Color.White.copy(alpha = 0.30f)
            }
            val tickWidth = 1.5.dp.toPx()

            for (workdayPercent in workdayMarkerPercents) {
                if (workdayPercent in 1f..99f) {
                    val x = barWidth * (workdayPercent / 100f)
                    drawRect(
                        color = tickColor,
                        topLeft = Offset(x - tickWidth / 2f, barHeight - tickHeight),
                        size = Size(tickWidth, tickHeight)
                    )
                }
            }
        }

        // 4. Draw Quota Warning Punchout Markers
        val warningPunchWidth = 4.dp.toPx()
        val warningStripeWidth = 1.dp.toPx()
        for (warningPercent in warningMarkerPercents) {
            if (warningPercent in 1f..99f) {
                val x = barWidth * (warningPercent / 100f)
                // Dark notch punchout
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(x - warningPunchWidth / 2f, 0f),
                    size = Size(warningPunchWidth, barHeight)
                )
                // Contrasting Amber indicator tick
                drawRect(
                    color = CodexBarColors.StatusAmber,
                    topLeft = Offset(x - warningStripeWidth / 2f, 0f),
                    size = Size(warningStripeWidth, barHeight)
                )
            }
        }

        // 5. Draw Pace Indicator (Iconic CodexBar 3-stripe notch punchout)
        if (pacePercent != null) {
            val paceX = barWidth * (pacePercent.coerceIn(0f, 100f) / 100f)
            val paceStripeWidth = 2.dp.toPx()
            val pacePunchWidth = paceStripeWidth * 3f // 3 stripes wide punchout

            val stripeColor = if (paceOnTop) {
                CodexBarColors.StatusGreen // On track / In reserve
            } else {
                CodexBarColors.StatusRed   // In deficit / Behind
            }

            // Punch background notch
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(max(0f, paceX - pacePunchWidth / 2f), 0f),
                size = Size(pacePunchWidth, barHeight)
            )

            // Center pace stripe
            drawRect(
                color = stripeColor,
                topLeft = Offset(max(0f, paceX - paceStripeWidth / 2f), 0f),
                size = Size(paceStripeWidth, barHeight)
            )
        }
    }
}

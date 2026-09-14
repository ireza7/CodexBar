package com.steipete.codexbar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.steipete.codexbar.domain.model.NamedRateWindow
import com.steipete.codexbar.domain.model.ProviderCostSnapshot
import com.steipete.codexbar.domain.model.ProviderDescriptor
import com.steipete.codexbar.domain.model.RateWindow
import com.steipete.codexbar.domain.model.UsagePace
import com.steipete.codexbar.domain.model.UsageSnapshot
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Truncates an email or long identifier in the middle (e.g. "developer@anthropic-user.com" -> "developer@...-user.com").
 */
fun middleTruncate(text: String, maxLength: Int = 24): String {
    if (text.length <= maxLength) return text
    val prefixLen = (maxLength - 3) / 2
    val suffixLen = maxLength - 3 - prefixLen
    return "${text.take(prefixLen)}...${text.takeLast(suffixLen)}"
}

/**
 * Formats epoch reset timestamp to a human-readable countdown string.
 */
fun formatResetCountdown(resetsAtEpochMs: Long?, now: Long = System.currentTimeMillis()): String? {
    if (resetsAtEpochMs == null) return null
    val deltaMs = resetsAtEpochMs - now
    if (deltaMs <= 0) return "Resets now"
    val totalSeconds = deltaMs / 1000L
    val totalMinutes = totalSeconds / 60L
    val hours = totalMinutes / 60L
    val remainingMinutes = totalMinutes % 60L
    val days = hours / 24L
    val remainingHours = hours % 24L

    return when {
        days > 0 -> "Resets in ${days}d ${remainingHours}h"
        hours > 0 -> "Resets in ${hours}h ${remainingMinutes}m"
        totalMinutes > 0 -> "Resets in ${totalMinutes}m"
        else -> "Resets in < 1m"
    }
}

/**
 * Formats token counts into compact K/M notation.
 */
fun formatTokenCount(tokens: Long?): String? {
    if (tokens == null || tokens <= 0) return null
    return when {
        tokens >= 1_000_000 -> String.format(Locale.US, "%.2fM tokens", tokens / 1_000_000.0)
        tokens >= 1_000 -> String.format(Locale.US, "%.1fK tokens", tokens / 1_000.0)
        else -> "$tokens tokens"
    }
}

/**
 * Modern Jetpack Compose card mirroring macOS CodexBar menu card design.
 * Strictly silos data to the provided [UsageSnapshot] and [ProviderDescriptor].
 */
@Composable
fun UsageCard(
    snapshot: UsageSnapshot,
    modifier: Modifier = Modifier,
    descriptor: ProviderDescriptor? = null,
    onCopyError: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val brandColor = CodexBarColors.colorForProvider(snapshot.provider)
    val now = System.currentTimeMillis()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CodexBarColors.SurfaceCard,
        border = BorderStroke(1.dp, CodexBarColors.CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1 & 2: Header section (Brand dot, title, middle-truncated email, subtitle, plan badge)
            UsageCardHeader(
                snapshot = snapshot,
                descriptor = descriptor,
                brandColor = brandColor
            )

            HorizontalDivider(
                color = CodexBarColors.Divider,
                thickness = 1.dp
            )

            // Error banner if probe failed
            if (snapshot.error != null) {
                ErrorStateBanner(
                    errorMessage = snapshot.error,
                    onCopyError = onCopyError,
                    onRetry = onRetry
                )
                // Antigravity Quota Dashboard: Grouped by Gemini and Claude & GPT
                // Gemini Models Group
                Text(
                    text = "Gemini Models",
                    style = CodexBarTypography.headlineMedium,
                    color = CodexBarColors.TextPrimary
                )

                snapshot.primary?.let { primaryWindow ->
                    RateWindowMetricRow(
                        title = "Five Hour Limit Remaining",
                        window = primaryWindow,
                        brandColor = brandColor,
                        nowEpochMs = now,
                        showPaceIndicator = false
                    )
                }

                val geminiWeekly = snapshot.extraRateWindows.find { it.id == "gemini_weekly" }?.window
                    ?: (if (snapshot.secondary?.resetDescription?.contains("Claude") == false) snapshot.secondary else null)

                geminiWeekly?.let { weeklyWindow ->
                    RateWindowMetricRow(
                        title = "Weekly Limit Remaining",
                        window = weeklyWindow,
                        brandColor = brandColor,
                        nowEpochMs = now,
                        showPaceIndicator = false
                    )
                }

                HorizontalDivider(
                    color = CodexBarColors.Divider,
                    thickness = 1.dp
                )

                // Claude and GPT models Group
                Text(
                    text = "Claude and GPT models",
                    style = CodexBarTypography.headlineMedium,
                    color = CodexBarColors.TextPrimary
                )

                val claudeFiveHour = snapshot.extraRateWindows.find { it.id == "claude_5h" }?.window
                claudeFiveHour?.let { fiveHourWindow ->
                    RateWindowMetricRow(
                        title = "Five Hour Limit Remaining",
                        window = fiveHourWindow,
                        brandColor = Color(0xFFFF9F0A),
                        nowEpochMs = now,
                        showPaceIndicator = false
                    )
                }

                val claudeWeekly = snapshot.extraRateWindows.find { it.id == "claude_weekly" }?.window
                    ?: (if (snapshot.secondary?.resetDescription?.contains("Claude") == true) snapshot.secondary else null)

                claudeWeekly?.let { weeklyWindow ->
                    RateWindowMetricRow(
                        title = "Weekly Limit Remaining",
                        window = weeklyWindow,
                        brandColor = Color(0xFFFF9F0A),
                        nowEpochMs = now,
                        showPaceIndicator = false
                    )
                }

                // Cost & Budget Section
                snapshot.costSnapshot?.let { cost ->
                    CostSnapshotRow(
                        cost = cost,
                        brandColor = brandColor
                    )
                }

                // Credits Section
                snapshot.credits?.let { credits ->
                    CreditsSnapshotRow(
                        creditsRemaining = credits.remaining,
                        brandColor = brandColor
                    )
                }

                // Token Usage Summary
                snapshot.tokenUsage?.let { tokens ->
                    TokenUsageRow(tokens = tokens)
                }
            }
        }
    }
}

@Composable
private fun UsageCardHeader(
    snapshot: UsageSnapshot,
    descriptor: ProviderDescriptor?,
    brandColor: Color
) {
    val displayName = descriptor?.displayName ?: snapshot.provider.displayName
    val email = snapshot.accountInfo?.email
    val plan = snapshot.accountInfo?.plan

    val updatedAgoMinutes = ((System.currentTimeMillis() - snapshot.updatedAtEpochMs) / 60_000L).coerceAtLeast(0L)
    val subtitleText = when {
        snapshot.error != null -> "Probe failed"
        updatedAgoMinutes == 0L -> "Updated just now"
        updatedAgoMinutes < 60L -> "Updated ${updatedAgoMinutes}m ago"
        else -> "Updated ${updatedAgoMinutes / 60L}h ago"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Brand dot + Title + Truncated Email
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Provider brand dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Text(
                    text = displayName,
                    style = CodexBarTypography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!email.isNullOrBlank()) {
                Text(
                    text = middleTruncate(email, 26),
                    style = CodexBarTypography.bodyMedium,
                    color = CodexBarColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Row 2: Subtitle + Plan Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = subtitleText,
                style = CodexBarTypography.labelMedium,
                color = if (snapshot.error != null) CodexBarColors.StatusRed else CodexBarColors.TextSecondary
            )

            if (!plan.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CodexBarColors.SurfaceCardElevated
                ) {
                    Text(
                        text = plan,
                        style = CodexBarTypography.labelSmall,
                        color = CodexBarColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RateWindowMetricRow(
    title: String,
    window: RateWindow,
    brandColor: Color,
    nowEpochMs: Long,
    showPaceIndicator: Boolean
) {
    val clampedPercent = window.usedPercent.coerceIn(0.0, 100.0).toFloat()
    val countdown = formatResetCountdown(window.resetsAtEpochMs, nowEpochMs) ?: window.resetDescription
    val pace = if (showPaceIndicator) UsagePace.calculate(window, nowEpochMs) else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Line 1: Title + Remaining Percentage and Countdown
        val remainingPct = window.remainingPercent.toInt()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title: $remainingPct% remaining",
                style = CodexBarTypography.bodyLarge
            )
            if (countdown != null) {
                Text(
                    text = countdown,
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.TextSecondary
                )
            }
        }

        // Line 2: Multi-stage progress bar
        UsageProgressBar(
            percent = clampedPercent,
            tint = brandColor,
            pacePercent = pace?.targetPercent?.toFloat(),
            paceOnTop = pace?.willLastToReset ?: true,
            warningMarkerPercents = listOf(80f)
        )

        // Line 3: Pace / diagnostic detail text
        val detailText = when {
            pace != null -> {
                val paceDesc = if (pace.willLastToReset) "Pace: on track · In reserve" else "Pace: deficit · Will not last to reset"
                window.resetDescription?.let { "$paceDesc · $it" } ?: paceDesc
            }
            window.resetDescription != null -> window.resetDescription
            else -> null
        }

        if (detailText != null) {
            Text(
                text = detailText,
                style = CodexBarTypography.labelMedium,
                color = CodexBarColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NamedRateWindowRow(
    namedWindow: NamedRateWindow,
    brandColor: Color,
    nowEpochMs: Long
) {
    val countdown = formatResetCountdown(namedWindow.window.resetsAtEpochMs, nowEpochMs)
    val clampedPercent = namedWindow.window.usedPercent.coerceIn(0.0, 100.0).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${namedWindow.title} ${namedWindow.window.usedPercent.toInt()}%",
                style = CodexBarTypography.bodyMedium,
                color = CodexBarColors.TextPrimary
            )
            if (countdown != null) {
                Text(
                    text = countdown,
                    style = CodexBarTypography.labelSmall,
                    color = CodexBarColors.TextSecondary
                )
            }
        }
        UsageProgressBar(
            percent = clampedPercent,
            tint = brandColor
        )
    }
}

@Composable
private fun CostSnapshotRow(
    cost: ProviderCostSnapshot,
    brandColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cost.period ?: "Spend Limit",
                style = CodexBarTypography.bodyMedium,
                color = CodexBarColors.TextPrimary
            )
            val costStr = String.format(Locale.US, "$%.2f / $%.2f %s", cost.used, cost.limit, cost.currencyCode)
            Text(
                text = costStr,
                style = CodexBarTypography.labelMedium,
                color = CodexBarColors.TextSecondary
            )
        }
        if (cost.limit > 0.0) {
            UsageProgressBar(
                percent = cost.usedPercent.toFloat(),
                tint = brandColor
            )
        }
    }
}

@Composable
private fun CreditsSnapshotRow(
    creditsRemaining: Double,
    brandColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Credits Remaining", style = CodexBarTypography.bodyMedium)
        Text(
            text = String.format(Locale.US, "$%.2f available", creditsRemaining),
            style = CodexBarTypography.labelMedium,
            color = CodexBarColors.StatusGreen
        )
    }
}

@Composable
private fun TokenUsageRow(
    tokens: com.steipete.codexbar.domain.model.CostUsageTokenSnapshot
) {
    val sessionFormatted = formatTokenCount(tokens.sessionTokens)
    val monthlyFormatted = formatTokenCount(tokens.last30DaysTokens)

    if (sessionFormatted != null || monthlyFormatted != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Token Usage", style = CodexBarTypography.bodyMedium)
            val tokenSummary = listOfNotNull(
                sessionFormatted?.let { "$it (session)" },
                monthlyFormatted?.let { "$it (30d)" }
            ).joinToString(" · ")
            Text(
                text = tokenSummary,
                style = CodexBarTypography.labelMedium,
                color = CodexBarColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorStateBanner(
    errorMessage: String,
    onCopyError: ((String) -> Unit)?,
    onRetry: (() -> Unit)?
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CodexBarColors.StatusRed.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, CodexBarColors.StatusRed.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = CodexBarColors.StatusRed,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = errorMessage,
                    style = CodexBarTypography.labelMedium,
                    color = CodexBarColors.StatusRed,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(errorMessage))
                        onCopyError?.invoke(errorMessage)
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Error",
                        tint = if (copied) CodexBarColors.StatusGreen else CodexBarColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (onRetry != null) {
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CodexBarColors.TextPrimary
                    ),
                    border = BorderStroke(1.dp, CodexBarColors.CardBorder),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " Retry",
                        style = CodexBarTypography.labelSmall
                    )
                }
            }
        }
    }
}

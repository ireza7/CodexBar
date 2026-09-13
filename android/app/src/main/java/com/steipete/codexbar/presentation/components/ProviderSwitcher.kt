package com.steipete.codexbar.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import com.steipete.codexbar.presentation.theme.CodexBarColors
import com.steipete.codexbar.presentation.theme.CodexBarTypography

/**
 * Visual model representing a switcher chip item in the top provider bar.
 */
data class ProviderSwitcherItem(
    val provider: UsageProvider,
    val displayName: String,
    val brandColor: Color,
    val quotaRemainingRatio: Float?
) {
    companion object {
        fun from(
            provider: UsageProvider,
            snapshot: UsageSnapshot?
        ): ProviderSwitcherItem {
            val remainingRatio = snapshot?.primary?.let { window ->
                (window.remainingPercent / 100.0).coerceIn(0.0, 1.0).toFloat()
            }
            return ProviderSwitcherItem(
                provider = provider,
                displayName = provider.displayName,
                brandColor = CodexBarColors.colorForProvider(provider),
                quotaRemainingRatio = remainingRatio
            )
        }
    }
}

/**
 * Horizontal scrollable row of provider chips mirroring macOS CodexBar's segment switcher.
 * Displays provider brand dot, title, and a 2dp mini quota indicator bar below each segment.
 */
@Composable
fun ProviderSwitcher(
    providers: List<UsageProvider>,
    selectedProvider: UsageProvider?,
    onSelectProvider: (UsageProvider) -> Unit,
    modifier: Modifier = Modifier,
    snapshots: Map<UsageProvider, UsageSnapshot> = emptyMap()
) {
    val items = providers.map { provider ->
        ProviderSwitcherItem.from(provider, snapshots[provider])
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.provider.id }) { item ->
            val isSelected = item.provider == selectedProvider
            ProviderSwitcherChip(
                item = item,
                isSelected = isSelected,
                onClick = { onSelectProvider(item.provider) }
            )
        }
    }
}

@Composable
private fun ProviderSwitcherChip(
    item: ProviderSwitcherItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CodexBarColors.SurfaceCardElevated else Color.Transparent,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) CodexBarColors.TextPrimary else CodexBarColors.TextSecondary,
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Provider Brand Indicator Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(item.brandColor)
                )
                Text(
                    text = item.displayName,
                    style = CodexBarTypography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                )
            }

            // 2dp mini quota indicator bar
            if (item.quotaRemainingRatio != null) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(CodexBarColors.ProgressTrack)
                ) {
                    val ratio = item.quotaRemainingRatio.coerceIn(0f, 1f)
                    val barColor = if (ratio < 0.2f) CodexBarColors.StatusRed else item.brandColor
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(barColor)
                        )
                    }
                }
            } else {
                // Subtle spacer to preserve height alignment across chips
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                )
            }
        }
    }
}

package com.steipete.codexbar.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CodexBarDarkColorScheme = darkColorScheme(
    primary = CodexBarColors.ProviderCodex,
    onPrimary = Color.White,
    primaryContainer = CodexBarColors.SurfaceCardElevated,
    onPrimaryContainer = CodexBarColors.TextPrimary,
    secondary = CodexBarColors.ProviderOpenAI,
    onSecondary = Color.White,
    secondaryContainer = CodexBarColors.SurfaceCardElevated,
    onSecondaryContainer = CodexBarColors.TextPrimary,
    tertiary = CodexBarColors.ProviderClaude,
    onTertiary = Color.White,
    background = CodexBarColors.Background,
    onBackground = CodexBarColors.TextPrimary,
    surface = CodexBarColors.SurfaceCard,
    onSurface = CodexBarColors.TextPrimary,
    surfaceVariant = CodexBarColors.SurfaceCardElevated,
    onSurfaceVariant = CodexBarColors.TextSecondary,
    outline = CodexBarColors.CardBorder,
    outlineVariant = CodexBarColors.Divider,
    error = CodexBarColors.StatusRed,
    onError = Color.White
)

/**
 * CodexBar dark modern theme matching macOS vibrancy and deep contrast surfaces.
 */
@Composable
fun CodexBarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CodexBarDarkColorScheme,
        typography = CodexBarTypography,
        content = content
    )
}

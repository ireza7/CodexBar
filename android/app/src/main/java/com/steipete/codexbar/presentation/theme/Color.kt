package com.steipete.codexbar.presentation.theme

import androidx.compose.ui.graphics.Color
import com.steipete.codexbar.domain.model.UsageProvider

/**
 * CodexBar dark theme palette mirroring macOS aesthetics and provider brand accents.
 */
object CodexBarColors {
    // Background & Surfaces
    val Background = Color(0xFF121214)
    val SurfaceCard = Color(0xFF1C1C1E)
    val SurfaceCardElevated = Color(0xFF2C2C2E)
    val SurfaceCardPressed = Color(0xFF3A3A3C)
    val CardBorder = Color(0x1FFFFFFF) // 12% subtle hairline border

    // Typography & Separators
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x99FFFFFF) // 60% white
    val TextTertiary = Color(0x66FFFFFF)  // 40% white
    val Divider = Color(0x1AFFFFFF)       // 10% white

    // Progress Bar Elements
    val ProgressTrack = Color(0x38FFFFFF) // 22% white
    val ProgressTrackDark = Color(0xFF28282C)

    // Status Indicators
    val StatusGreen = Color(0xFF34C759)   // On Track / In Reserve
    val StatusAmber = Color(0xFFFF9F0A)   // Warning Threshold
    val StatusRed = Color(0xFFFF453A)     // Deficit / Over Quota / Error
    val StatusDimmed = Color(0xFF8E8E93)  // Inactive / Unconfigured

    // Provider Brand Accents
    val ProviderOpenAI = Color(0xFF0F826E)
    val ProviderClaude = Color(0xFFCC7C5E)
    val ProviderCursor = Color(0xFF00BFA5)
    val ProviderCopilot = Color(0xFFA855F7)
    val ProviderGemini = Color(0xFFAB87EA)
    val ProviderCodex = Color(0xFF49A3B0)

    /**
     * Converts a hex color string (e.g. "#0F826E" or "#34C759") into a Compose [Color]
     * using pure Kotlin logic without Android platform runtime dependencies.
     */
    fun hexToColor(hex: String, defaultColor: Color = ProviderCodex): Color {
        val clean = hex.removePrefix("#").trim()
        return try {
            when (clean.length) {
                6 -> {
                    val rgb = clean.toLong(16)
                    Color(0xFF000000 or rgb)
                }
                8 -> {
                    val argb = clean.toLong(16)
                    Color(argb)
                }
                else -> defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }

    /**
     * Resolves the canonical brand accent [Color] for the specified [UsageProvider].
     */
    fun colorForProvider(provider: UsageProvider): Color {
        return hexToColor(provider.brandColorHex, ProviderGemini)
    }
}

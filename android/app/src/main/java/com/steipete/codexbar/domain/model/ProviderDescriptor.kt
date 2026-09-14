package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable

/**
 * Static metadata descriptor defining UI labels, branding, auth requirements,
 * and external links for Antigravity (Google DeepMind).
 */
@Serializable
data class ProviderDescriptor(
    val id: UsageProvider = UsageProvider.ANTIGRAVITY,
    val displayName: String = "Antigravity",
    val defaultSessionLabel: String = "Gemini Models",
    val defaultWeeklyLabel: String = "Claude & GPT",
    val brandColorHex: String = "#4285F4",
    val requiresApiKey: Boolean = false,
    val requiresOAuth: Boolean = true,
    val supportsTokenCost: Boolean = false,
    val supportsCredits: Boolean = false,
    val dashboardUrl: String? = "https://aistudio.google.com",
    val statusPageUrl: String? = "https://www.google.com/appsstatus/dashboard"
) {
    companion object {
        fun forProvider(provider: UsageProvider = UsageProvider.ANTIGRAVITY): ProviderDescriptor {
            return ProviderDescriptor()
        }

        fun all(): List<ProviderDescriptor> {
            return listOf(ProviderDescriptor())
        }
    }
}

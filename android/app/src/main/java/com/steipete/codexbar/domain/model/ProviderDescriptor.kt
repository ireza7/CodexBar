package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable

/**
 * Static metadata descriptor defining UI labels, branding, auth requirements,
 * and external links for an AI provider.
 */
@Serializable
data class ProviderDescriptor(
    val id: UsageProvider,
    val displayName: String = id.displayName,
    val defaultSessionLabel: String = "Session",
    val defaultWeeklyLabel: String = "Weekly",
    val brandColorHex: String = id.brandColorHex,
    val requiresApiKey: Boolean = false,
    val requiresOAuth: Boolean = false,
    val supportsTokenCost: Boolean = false,
    val supportsCredits: Boolean = false,
    val dashboardUrl: String? = null,
    val statusPageUrl: String? = null
) {
    companion object {
        private val descriptorsByProvider: Map<UsageProvider, ProviderDescriptor> = mapOf(
            UsageProvider.CODEX to ProviderDescriptor(
                id = UsageProvider.CODEX,
                defaultSessionLabel = "5-Hour Session",
                defaultWeeklyLabel = "Weekly Quota",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://chatgpt.com",
                statusPageUrl = "https://status.openai.com"
            ),
            UsageProvider.OPENAI to ProviderDescriptor(
                id = UsageProvider.OPENAI,
                defaultSessionLabel = "Usage Rate",
                defaultWeeklyLabel = "Monthly Budget",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://platform.openai.com/usage",
                statusPageUrl = "https://status.openai.com"
            ),
            UsageProvider.CLAUDE to ProviderDescriptor(
                id = UsageProvider.CLAUDE,
                defaultSessionLabel = "5-Hour Window",
                defaultWeeklyLabel = "7-Day Quota",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = true,
                supportsCredits = false,
                dashboardUrl = "https://claude.ai/settings/billing",
                statusPageUrl = "https://status.anthropic.com"
            ),
            UsageProvider.CURSOR to ProviderDescriptor(
                id = UsageProvider.CURSOR,
                defaultSessionLabel = "Fast Requests",
                defaultWeeklyLabel = "Monthly Cycle",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = true,
                supportsCredits = false,
                dashboardUrl = "https://cursor.com/dashboard?tab=usage",
                statusPageUrl = "https://status.cursor.com"
            ),
            UsageProvider.COPILOT to ProviderDescriptor(
                id = UsageProvider.COPILOT,
                defaultSessionLabel = "Premium Quota",
                defaultWeeklyLabel = "Chat Quota",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = true,
                dashboardUrl = "https://github.com/settings/copilot",
                statusPageUrl = "https://www.githubstatus.com"
            ),
            UsageProvider.GEMINI to ProviderDescriptor(
                id = UsageProvider.GEMINI,
                defaultSessionLabel = "Pro Quota",
                defaultWeeklyLabel = "Flash Quota",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false,
                dashboardUrl = "https://gemini.google.com",
                statusPageUrl = "https://www.google.com/appsstatus/dashboard"
            ),
            UsageProvider.ANTIGRAVITY to ProviderDescriptor(
                id = UsageProvider.ANTIGRAVITY,
                defaultSessionLabel = "Session Quota",
                defaultWeeklyLabel = "Daily Quota",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.OPENROUTER to ProviderDescriptor(
                id = UsageProvider.OPENROUTER,
                defaultSessionLabel = "Daily Usage",
                defaultWeeklyLabel = "Monthly Cap",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://openrouter.ai/credits"
            ),
            UsageProvider.DEEPSEEK to ProviderDescriptor(
                id = UsageProvider.DEEPSEEK,
                defaultSessionLabel = "Daily Usage",
                defaultWeeklyLabel = "Prepaid Balance",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://platform.deepseek.com/usage"
            ),
            UsageProvider.MISTRAL to ProviderDescriptor(
                id = UsageProvider.MISTRAL,
                defaultSessionLabel = "Rate Limit",
                defaultWeeklyLabel = "Monthly Usage",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://console.mistral.ai/billing"
            ),
            UsageProvider.XAI to ProviderDescriptor(
                id = UsageProvider.XAI,
                defaultSessionLabel = "Hourly Rate",
                defaultWeeklyLabel = "Team Balance",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://console.x.ai"
            ),
            UsageProvider.GROK to ProviderDescriptor(
                id = UsageProvider.GROK,
                defaultSessionLabel = "Hourly Rate",
                defaultWeeklyLabel = "Team Balance",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true,
                dashboardUrl = "https://console.x.ai"
            ),
            UsageProvider.PERPLEXITY to ProviderDescriptor(
                id = UsageProvider.PERPLEXITY,
                defaultSessionLabel = "Daily Quota",
                defaultWeeklyLabel = "Credits Remaining",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = false,
                supportsCredits = true,
                dashboardUrl = "https://www.perplexity.ai/settings/api"
            ),
            UsageProvider.OLLAMA to ProviderDescriptor(
                id = UsageProvider.OLLAMA,
                defaultSessionLabel = "Local Active",
                defaultWeeklyLabel = "Local Models",
                requiresApiKey = false,
                requiresOAuth = false,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.MINIMAX to ProviderDescriptor(
                id = UsageProvider.MINIMAX,
                defaultSessionLabel = "Coding Plan",
                defaultWeeklyLabel = "Token Plan",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true
            ),
            UsageProvider.MOONSHOT to ProviderDescriptor(
                id = UsageProvider.MOONSHOT,
                defaultSessionLabel = "Daily Usage",
                defaultWeeklyLabel = "Prepaid Balance",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true
            ),
            UsageProvider.KIMI to ProviderDescriptor(
                id = UsageProvider.KIMI,
                defaultSessionLabel = "Daily Usage",
                defaultWeeklyLabel = "Prepaid Balance",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true
            ),
            UsageProvider.DEVIN to ProviderDescriptor(
                id = UsageProvider.DEVIN,
                defaultSessionLabel = "ACU Sessions",
                defaultWeeklyLabel = "Monthly ACUs",
                requiresApiKey = true,
                requiresOAuth = false,
                supportsTokenCost = false,
                supportsCredits = true
            ),
            UsageProvider.WINDSURF to ProviderDescriptor(
                id = UsageProvider.WINDSURF,
                defaultSessionLabel = "Prompt Quota",
                defaultWeeklyLabel = "Monthly Plan",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.ZED to ProviderDescriptor(
                id = UsageProvider.ZED,
                defaultSessionLabel = "Assistant Tokens",
                defaultWeeklyLabel = "Monthly Allowance",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.WARP to ProviderDescriptor(
                id = UsageProvider.WARP,
                defaultSessionLabel = "AI Requests",
                defaultWeeklyLabel = "Monthly Allowance",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.AUGMENT to ProviderDescriptor(
                id = UsageProvider.AUGMENT,
                defaultSessionLabel = "Session Quota",
                defaultWeeklyLabel = "Subscription Tier",
                requiresApiKey = false,
                requiresOAuth = true,
                supportsTokenCost = false,
                supportsCredits = false
            ),
            UsageProvider.SYNTHETIC to ProviderDescriptor(
                id = UsageProvider.SYNTHETIC,
                defaultSessionLabel = "Synthetic Session",
                defaultWeeklyLabel = "Synthetic Weekly",
                requiresApiKey = false,
                requiresOAuth = false,
                supportsTokenCost = true,
                supportsCredits = true
            )
        )

        fun forProvider(provider: UsageProvider): ProviderDescriptor {
            return descriptorsByProvider[provider] ?: ProviderDescriptor(id = provider)
        }

        fun all(): List<ProviderDescriptor> {
            return UsageProvider.entries.map { forProvider(it) }
        }
    }
}

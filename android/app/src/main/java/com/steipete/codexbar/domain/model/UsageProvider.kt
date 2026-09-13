package com.steipete.codexbar.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enumeration of all supported AI providers in CodexBar.
 * Contains provider identifier, display name, and canonical brand accent color hex.
 */
@Serializable
enum class UsageProvider(
    val id: String,
    val displayName: String,
    val brandColorHex: String
) {
    @SerialName("codex")
    CODEX("codex", "ChatGPT / Codex", "#49A3B0"),

    @SerialName("openai")
    OPENAI("openai", "OpenAI", "#0F826E"),

    @SerialName("claude")
    CLAUDE("claude", "Claude", "#CC7C5E"),

    @SerialName("cursor")
    CURSOR("cursor", "Cursor", "#00BFA5"),

    @SerialName("copilot")
    COPILOT("copilot", "GitHub Copilot", "#A855F7"),

    @SerialName("gemini")
    GEMINI("gemini", "Gemini", "#AB87EA"),

    @SerialName("antigravity")
    ANTIGRAVITY("antigravity", "Antigravity", "#4285F4"),

    @SerialName("openrouter")
    OPENROUTER("openrouter", "OpenRouter", "#6366F1"),

    @SerialName("deepseek")
    DEEPSEEK("deepseek", "DeepSeek", "#4D6BFE"),

    @SerialName("mistral")
    MISTRAL("mistral", "Mistral", "#FF7000"),

    @SerialName("xai")
    XAI("xai", "xAI (Grok)", "#E2E8F0"),

    @SerialName("grok")
    GROK("grok", "Grok", "#E2E8F0"),

    @SerialName("perplexity")
    PERPLEXITY("perplexity", "Perplexity", "#20B2AA"),

    @SerialName("ollama")
    OLLAMA("ollama", "Ollama", "#E5E7EB"),

    @SerialName("minimax")
    MINIMAX("minimax", "MiniMax", "#FF4D4F"),

    @SerialName("moonshot")
    MOONSHOT("moonshot", "Moonshot", "#1890FF"),

    @SerialName("kimi")
    KIMI("kimi", "Kimi", "#1890FF"),

    @SerialName("devin")
    DEVIN("devin", "Devin", "#10B981"),

    @SerialName("windsurf")
    WINDSURF("windsurf", "Windsurf", "#0EA5E9"),

    @SerialName("zed")
    ZED("zed", "Zed", "#F59E0B"),

    @SerialName("warp")
    WARP("warp", "Warp", "#00E599"),

    @SerialName("augment")
    AUGMENT("augment", "Augment", "#8B5CF6"),

    @SerialName("synthetic")
    SYNTHETIC("synthetic", "Synthetic Test", "#6B7280");

    companion object {
        private val idMap = entries.associateBy { it.id.lowercase() }

        fun fromId(id: String?): UsageProvider? {
            if (id.isNullOrBlank()) return null
            return idMap[id.trim().lowercase()]
        }
    }
}

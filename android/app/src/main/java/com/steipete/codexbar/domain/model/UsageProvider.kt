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
    CODEX("codex", "Codex", "#736BD4"),

    @SerialName("openai")
    OPENAI("openai", "OpenAI", "#000000"),

    @SerialName("azureopenai")
    AZUREOPENAI("azureopenai", "Azure OpenAI", "#0078D4"),

    @SerialName("claude")
    CLAUDE("claude", "Claude", "#D97757"),

    @SerialName("clinepass")
    CLINEPASS("clinepass", "ClinePass", "#61A3FA"),

    @SerialName("cursor")
    CURSOR("cursor", "Cursor", "#1B1913"),

    @SerialName("opencode")
    OPENCODE("opencode", "OpenCode", "#211E1E"),

    @SerialName("opencodego")
    OPENCODEGO("opencodego", "OpenCode Go", "#211E1E"),

    @SerialName("alibaba")
    ALIBABA("alibaba", "Alibaba Coding Plan", "#FF6A00"),

    @SerialName("alibabatokenplan")
    ALIBABATOKENPLAN("alibabatokenplan", "Alibaba Token Plan", "#FF6A00"),

    @SerialName("qwencloud")
    QWENCLOUD("qwencloud", "Qwen Cloud", "#615CED"),

    @SerialName("factory")
    FACTORY("factory", "Droid", "#EE6018"),

    @SerialName("fireworks")
    FIREWORKS("fireworks", "Fireworks", "#E65618"),

    @SerialName("gemini")
    GEMINI("gemini", "Gemini", "#4285F4"),

    @SerialName("antigravity")
    ANTIGRAVITY("antigravity", "Antigravity", "#4285F4"),

    @SerialName("copilot")
    COPILOT("copilot", "Copilot", "#8534F3"),

    @SerialName("devin")
    DEVIN("devin", "Devin", "#000000"),

    @SerialName("zai")
    ZAI("zai", "z.ai", "#126EF6"),

    @SerialName("minimax")
    MINIMAX("minimax", "MiniMax", "#181E25"),

    @SerialName("manus")
    MANUS("manus", "Manus", "#34322D"),

    @SerialName("kimi")
    KIMI("kimi", "Kimi Code", "#000000"),

    @SerialName("kilo")
    KILO("kilo", "Kilo", "#FA483A"),

    @SerialName("kiro")
    KIRO("kiro", "Kiro", "#8F4AFF"),

    @SerialName("vertexai")
    VERTEXAI("vertexai", "Vertex AI", "#4285F4"),

    @SerialName("augment")
    AUGMENT("augment", "Augment", "#F97316"),

    @SerialName("jetbrains")
    JETBRAINS("jetbrains", "JetBrains AI", "#6B57FF"),

    @SerialName("moonshot")
    MOONSHOT("moonshot", "Moonshot", "#121212"),

    @SerialName("amp")
    AMP("amp", "Amp", "#091C1E"),

    @SerialName("t3chat")
    T3CHAT("t3chat", "T3 Chat", "#970B72"),

    @SerialName("ollama")
    OLLAMA("ollama", "Ollama", "#000000"),

    @SerialName("synthetic")
    SYNTHETIC("synthetic", "Synthetic", "#6366F1"),

    @SerialName("openrouter")
    OPENROUTER("openrouter", "OpenRouter", "#96A5B9"),

    @SerialName("elevenlabs")
    ELEVENLABS("elevenlabs", "ElevenLabs", "#000000"),

    @SerialName("warp")
    WARP("warp", "Warp", "#C7AEFF"),

    @SerialName("windsurf")
    WINDSURF("windsurf", "Windsurf", "#000000"),

    @SerialName("zed")
    ZED("zed", "Zed", "#084CCF"),

    @SerialName("perplexity")
    PERPLEXITY("perplexity", "Perplexity", "#016A71"),

    @SerialName("mimo")
    MIMO("mimo", "Xiaomi MiMo", "#3D3834"),

    @SerialName("doubao")
    DOUBAO("doubao", "Doubao", "#0057FF"),

    @SerialName("sakana")
    SAKANA("sakana", "Sakana AI", "#E10600"),

    @SerialName("abacus")
    ABACUS("abacus", "Abacus AI", "#35BEE2"),

    @SerialName("mistral")
    MISTRAL("mistral", "Mistral", "#FA500F"),

    @SerialName("deepseek")
    DEEPSEEK("deepseek", "DeepSeek", "#4D6BFE"),

    @SerialName("deepinfra")
    DEEPINFRA("deepinfra", "DeepInfra", "#2A3275"),

    @SerialName("codebuff")
    CODEBUFF("codebuff", "Codebuff", "#9EFC62"),

    @SerialName("crof")
    CROF("crof", "Crof", "#0A0A0A"),

    @SerialName("venice")
    VENICE("venice", "Venice", "#0E2942"),

    @SerialName("commandcode")
    COMMANDCODE("commandcode", "Command Code", "#A04DFD"),

    @SerialName("qoder")
    QODER("qoder", "Qoder", "#2ADB5C"),

    @SerialName("stepfun")
    STEPFUN("stepfun", "StepFun", "#000000"),

    @SerialName("bedrock")
    BEDROCK("bedrock", "AWS Bedrock", "#01A88D"),

    @SerialName("grok")
    GROK("grok", "Grok", "#000000"),

    @SerialName("groq")
    GROQ("groq", "Groq", "#F43E01"),

    @SerialName("llmproxy")
    LLMPROXY("llmproxy", "LLM Proxy", "#00FFFF"),

    @SerialName("litellm")
    LITELLM("litellm", "LiteLLM", "#191938"),

    @SerialName("deepgram")
    DEEPGRAM("deepgram", "Deepgram", "#13EF95"),

    @SerialName("poe")
    POE("poe", "Poe", "#5D5CDE"),

    @SerialName("chutes")
    CHUTES("chutes", "Chutes", "#121212"),

    @SerialName("neuralwatt")
    NEURALWATT("neuralwatt", "Neuralwatt", "#38D98C"),

    @SerialName("clawrouter")
    CLAWROUTER("clawrouter", "ClawRouter", "#332CB3"),

    @SerialName("longcat")
    LONGCAT("longcat", "LongCat", "#FFD100"),

    @SerialName("sub2api")
    SUB2API("sub2api", "sub2api", "#1F62FF"),

    @SerialName("wayfinder")
    WAYFINDER("wayfinder", "Wayfinder", "#10A37F"),

    @SerialName("zenmux")
    ZENMUX("zenmux", "ZenMux", "#6C5CE7"),

    @SerialName("aiand")
    AIAND("aiand", "ai&", "#E25C2B"),

    @SerialName("zoommate")
    ZOOMMATE("zoommate", "ZoomMate", "#0B5CFF"),

    @SerialName("xai")
    XAI("xai", "xAI", "#1A1A1A"),

    @SerialName("notion")
    NOTION("notion", "Notion AI", "#337EA9"),

    @SerialName("ibmbob")
    IBMBOB("ibmbob", "IBM Bob", "#0E61FA");

    companion object {
        private val idMap = entries.associateBy { it.id.lowercase() }

        fun fromId(id: String?): UsageProvider? {
            if (id.isNullOrBlank()) return null
            return idMap[id.trim().lowercase()]
        }
    }
}

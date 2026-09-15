package com.steipete.codexbar.presentation.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.steipete.codexbar.domain.model.AntigravityAccount
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class WidgetTheme(val label: String, val bgDrawableRes: Int) {
    OBSIDIAN("Dark Obsidian", com.steipete.codexbar.R.drawable.widget_neumorph_bg_card),
    SLATE("Slate Steel", com.steipete.codexbar.R.drawable.widget_neumorph_bg_slate),
    CYBER("Cyber Glow", com.steipete.codexbar.R.drawable.widget_neumorph_bg_cyber)
}

enum class WidgetAccent(val label: String, val primaryColor: Long, val secondaryColor: Long) {
    DEFAULT("Original Dual", 0xFF6EA8FE, 0xFFFF9F0A),
    CYAN("Electric Cyan", 0xFF00E5FF, 0xFF40C4FF),
    AMBER("Sunset Amber", 0xFFFF9F0A, 0xFFFFB74D),
    EMERALD("Mint Emerald", 0xFF00E676, 0xFF69F0AE),
    PURPLE("Neon Violet", 0xFFB388FF, 0xFF7C4DFF)
}

@Serializable
data class WidgetConfig(
    val targetAccountId: String = WidgetConfigManager.TARGET_ACTIVE,
    val themeName: String = WidgetTheme.OBSIDIAN.name,
    val accentName: String = WidgetAccent.DEFAULT.name,
    val showWeekly: Boolean = true,
    val showResetTime: Boolean = true,
    val compactDensity: Boolean = false
) {
    val theme: WidgetTheme
        get() = try { WidgetTheme.valueOf(themeName) } catch (_: Exception) { WidgetTheme.OBSIDIAN }

    val accent: WidgetAccent
        get() = try { WidgetAccent.valueOf(accentName) } catch (_: Exception) { WidgetAccent.DEFAULT }

    val geminiColorProvider: ColorProvider
        get() = ColorProvider(Color(accent.primaryColor))

    val claudeColorProvider: ColorProvider
        get() = ColorProvider(Color(accent.secondaryColor))
}

object WidgetConfigManager {
    const val PREFS_NAME = "antigravity_widget_configs"
    const val TARGET_ACTIVE = "active"
    const val TARGET_ALL = "all"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun getConfig(context: Context, appWidgetId: Int): WidgetConfig {
        if (appWidgetId <= 0) return WidgetConfig()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("config_$appWidgetId", null) ?: return WidgetConfig()
        return try {
            json.decodeFromString<WidgetConfig>(raw)
        } catch (_: Exception) {
            WidgetConfig()
        }
    }

    fun saveConfig(context: Context, appWidgetId: Int, config: WidgetConfig) {
        if (appWidgetId <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val raw = json.encodeToString(WidgetConfig.serializer(), config)
            prefs.edit().putString("config_$appWidgetId", raw).apply()
        } catch (_: Exception) {}
    }

    fun removeConfig(context: Context, appWidgetId: Int) {
        if (appWidgetId <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("config_$appWidgetId").apply()
    }

    fun getSavedAccounts(context: Context): List<AntigravityAccount> {
        val prefs = context.getSharedPreferences("antigravity_accounts_prefs", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("saved_accounts_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<AntigravityAccount>>(rawJson)
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Fallback: construct account from active widget cache if available
        val cache = context.getSharedPreferences("antigravity_widget_cache", Context.MODE_PRIVATE)
        val label = cache.getString("active_account_label", null)
        val email = cache.getString("active_account_email", null)
        val g5h = cache.getInt("gemini_5h_pct", 100)
        val c5h = cache.getInt("claude_5h_pct", 100)
        val gW = cache.getInt("gemini_weekly_pct", 100)
        val cW = cache.getInt("claude_weekly_pct", 100)
        val gReset = cache.getLong("gemini_5h_reset", 0L)
        val cReset = cache.getLong("claude_5h_reset", 0L)

        return listOf(
            AntigravityAccount(
                id = "default_fallback",
                label = label ?: "Default Account",
                email = email,
                token = "",
                isActive = true,
                gemini5hPct = g5h,
                geminiWeeklyPct = gW,
                claude5hPct = c5h,
                claudeWeeklyPct = cW,
                gemini5hResetEpoch = gReset,
                claude5hResetEpoch = cReset,
                lastSyncEpochMs = cache.getLong("last_updated_epoch", System.currentTimeMillis())
            )
        )
    }

    fun cycleAccount(context: Context, appWidgetId: Int): String {
        val currentConfig = getConfig(context, appWidgetId)
        val accounts = getSavedAccounts(context)
        if (accounts.isEmpty()) return TARGET_ACTIVE

        // Candidate targets: TARGET_ACTIVE, account IDs, and if more than 1 account TARGET_ALL
        val candidates = mutableListOf(TARGET_ACTIVE)
        candidates.addAll(accounts.map { it.id })
        if (accounts.size > 1) {
            candidates.add(TARGET_ALL)
        }

        val currentIndex = candidates.indexOf(currentConfig.targetAccountId).coerceAtLeast(0)
        val nextIndex = (currentIndex + 1) % candidates.size
        val nextTarget = candidates[nextIndex]

        val newConfig = currentConfig.copy(targetAccountId = nextTarget)
        saveConfig(context, appWidgetId, newConfig)
        return nextTarget
    }

    fun resolveTargetAccounts(context: Context, targetId: String): List<AntigravityAccount> {
        val accounts = getSavedAccounts(context)
        if (accounts.isEmpty()) return emptyList()

        return when (targetId) {
            TARGET_ALL -> accounts
            TARGET_ACTIVE -> {
                val active = accounts.find { it.isActive } ?: accounts.first()
                listOf(active)
            }
            else -> {
                val found = accounts.find { it.id == targetId }
                if (found != null) listOf(found) else listOf(accounts.first())
            }
        }
    }
}

package com.steipete.codexbar.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Single-provider focus: Antigravity (Google DeepMind).
 */
@Serializable
enum class UsageProvider(
    val id: String,
    val displayName: String,
    val brandColorHex: String
) {
    @SerialName("antigravity")
    ANTIGRAVITY("antigravity", "Antigravity", "#4285F4");

    companion object {
        private val idMap = entries.associateBy { it.id.lowercase() }

        fun fromId(id: String?): UsageProvider? {
            if (id.isNullOrBlank()) return null
            return idMap[id.trim().lowercase()] ?: ANTIGRAVITY
        }
    }
}

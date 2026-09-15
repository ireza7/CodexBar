package com.steipete.codexbar.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a saved Google/Antigravity account with associated auth token and quota cache.
 */
@Serializable
data class AntigravityAccount(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val email: String? = null,
    val token: String,
    val plan: String? = null,
    val isActive: Boolean = false,
    val lastSyncEpochMs: Long = 0L,
    val gemini5hPct: Int? = null,
    val geminiWeeklyPct: Int? = null,
    val claude5hPct: Int? = null,
    val claudeWeeklyPct: Int? = null,
    val gemini5hResetEpoch: Long? = null,
    val claude5hResetEpoch: Long? = null
)

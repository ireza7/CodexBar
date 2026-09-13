package com.steipete.codexbar.data.remote

import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UsageSnapshot
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Common base exception for remote provider fetch operations.
 */
open class ProviderFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when an API returns HTTP 429 Too Many Requests.
 * Carries the optional epoch timestamp in milliseconds when requests may resume.
 */
class RateLimitedException(
    val retryAfterEpochMs: Long?,
    message: String
) : ProviderFetchException(message)

/**
 * Thrown when an API rejects credentials (HTTP 401 or 403).
 */
class AuthenticationException(message: String) : ProviderFetchException(message)

/**
 * Contract for provider-specific API fetchers.
 */
interface ProviderFetcher {
    val provider: UsageProvider

    /**
     * Fetches current usage from the provider's API using the provided [credentials].
     */
    suspend fun fetchUsage(credentials: ProviderCredentials): Result<UsageSnapshot>
}

/**
 * Utility for parsing HTTP `Retry-After` header values (supports integer delta-seconds and RFC 1123 date strings).
 */
fun parseRetryAfter(headerValue: String?, nowEpochMs: Long = System.currentTimeMillis()): Long? {
    if (headerValue.isNullOrBlank()) return null
    val trimmed = headerValue.trim()

    // 1. Try parsing as delta-seconds integer
    val seconds = trimmed.toLongOrNull()
    if (seconds != null && seconds >= 0) {
        return nowEpochMs + (seconds * 1000L)
    }

    // 2. Try parsing as RFC 1123 date string (e.g. "Wed, 21 Oct 2026 07:28:00 GMT")
    return try {
        val parsed = DateTimeFormatter.RFC_1123_DATE_TIME
            .withLocale(Locale.US)
            .parse(trimmed, Instant::from)
        parsed.toEpochMilli()
    } catch (_: Exception) {
        try {
            Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

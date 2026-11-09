package com.example.fishing.data.report

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Date and instant parsing for values exchanged with Supabase.
 *
 * The backend has produced several timestamp shapes over time, so reads accept
 * all of them while writes always use the canonical form. Kept separate from the
 * repository so the rules can be unit-tested without Room or a Supabase client.
 */
internal object SupabaseDateFormats {

    private val datePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )

    private fun formatter(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US)

    /** Parses any of the timestamp formats the backend has used. */
    fun parseDate(value: String): Date? {
        for (pattern in datePatterns) {
            try {
                return formatter(pattern).parse(value)
            } catch (_: Exception) {
                // Try the next pattern.
            }
        }
        return null
    }

    /** Canonical format used for `created_at` / `updated_at` writes. */
    fun formatDate(date: Date): String = formatter(datePatterns.first()).format(date)

    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value)
        } catch (_: Exception) {
            try {
                parseDate(value)?.toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun formatInstant(instant: Instant?): String? =
        instant?.let { DateTimeFormatter.ISO_INSTANT.format(it) }
}

/**
 * Case-insensitive enum lookup. Underscores in the enum constant name are
 * ignored so a stored value that lost its underscores still resolves.
 * Returns `null` when nothing matches, letting callers apply their default.
 */
internal fun <T : Enum<T>> parseEnumValue(name: String?, values: Array<T>): T? {
    if (name.isNullOrEmpty()) return null
    return values.firstOrNull { value ->
        value.name.equals(name, ignoreCase = true) ||
            value.name.replace("_", "").equals(name, ignoreCase = true)
    }
}

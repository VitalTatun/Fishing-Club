package com.example.fishing

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression guards: timestamps come from Supabase REST as ISO-8601 strings
 * with an explicit `+00:00` offset (and possibly fractional seconds), while a
 * legacy report may have no end time at all (`fishing_end_at = null`).
 */
class InstantParsingTest {

    @Test
    fun `Instant parse actual REST format with millis and plus-zero offset`() {
        assertEquals(
            Instant.parse("2026-07-15T06:26:53.197Z"),
            Instant.parse("2026-07-15T06:26:53.197+00:00")
        )
    }

    @Test
    fun `Instant parse REST format with microsecond fraction`() {
        assertEquals(
            Instant.parse("2026-08-29T06:37:00.123456Z"),
            Instant.parse("2026-08-29T06:37:00.123456+00:00")
        )
    }

    @Test
    fun `Instant parse REST format without fractional seconds`() {
        assertEquals(
            Instant.parse("2026-07-15T06:26:53Z"),
            Instant.parse("2026-07-15T06:26:53+00:00")
        )
    }

    @Test
    fun `legacy report has null end - must stay representable`() {
        val end: Instant? = null
        assertNull(end)
        val start = Instant.now().minusSeconds(3600)
        val duration = end?.takeIf { it.isAfter(start) }?.let { java.time.Duration.between(start, it) }?.toMinutes()
        assertNull(duration)
    }
}
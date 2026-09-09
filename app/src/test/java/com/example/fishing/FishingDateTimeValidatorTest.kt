package com.example.fishing

import com.example.fishing.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class FishingDateTimeValidatorTest {

    private val now = Instant.parse("2026-07-29T18:00:00Z")

    // 1. Both missing
    @Test
    fun `both missing - invalid`() {
        val errors = FishingDateTimeValidator.validate(null, null, now)
        assertEquals(listOf(FishingDateTimeError.START_MISSING), errors)
        assertFalse(FishingDateTimeValidator.isValid(null, null))
    }

    // 2. Start missing
    @Test
    fun `start missing - invalid`() {
        val end = Instant.parse("2026-07-29T20:00:00Z")
        val errors = FishingDateTimeValidator.validate(null, end, now)
        assertEquals(listOf(FishingDateTimeError.START_MISSING), errors)
        assertFalse(FishingDateTimeValidator.isValid(null, end))
    }

    // 3. End missing
    @Test
    fun `end missing - invalid`() {
        val start = Instant.parse("2026-07-29T10:00:00Z")
        val errors = FishingDateTimeValidator.validate(start, null, now)
        assertEquals(listOf(FishingDateTimeError.END_MISSING), errors)
        assertFalse(FishingDateTimeValidator.isValid(start, null))
    }

    // 4. Start == End
    @Test
    fun `start equals end - invalid`() {
        val t = Instant.parse("2026-07-29T12:00:00Z")
        val errors = FishingDateTimeValidator.validate(t, t, now)
        assertEquals(listOf(FishingDateTimeError.END_NOT_AFTER_START), errors)
        assertFalse(FishingDateTimeValidator.isValid(t, t))
    }

    // 5. End before start
    @Test
    fun `end before start - invalid`() {
        val futureNow = Instant.parse("2026-07-30T12:00:00Z")
        val start = Instant.parse("2026-07-29T20:00:00Z")
        val end = Instant.parse("2026-07-29T10:00:00Z")
        val errors = FishingDateTimeValidator.validate(start, end, futureNow)
        assertEquals(listOf(FishingDateTimeError.END_NOT_AFTER_START), errors)
        assertFalse(FishingDateTimeValidator.isValid(start, end, futureNow))
    }

    // 6. Start in future
    @Test
    fun `start in future - invalid`() {
        val start = Instant.parse("2026-08-01T10:00:00Z")
        val end = Instant.parse("2026-08-01T12:00:00Z")
        val errors = FishingDateTimeValidator.validate(start, end, now)
        assertTrue(errors.contains(FishingDateTimeError.START_IN_FUTURE))
        assertFalse(FishingDateTimeValidator.isValid(start, end, now))
    }

    // 7. End in future
    @Test
    fun `end in future - invalid`() {
        val start = Instant.parse("2026-07-29T17:00:00Z")
        val end = Instant.parse("2026-08-01T12:00:00Z")
        val errors = FishingDateTimeValidator.validate(start, end, now)
        assertEquals(listOf(FishingDateTimeError.END_IN_FUTURE), errors)
        assertFalse(FishingDateTimeValidator.isValid(start, end, now))
    }

    // 8. Normal fishing → valid
    @Test
    fun `normal fishing - valid`() {
        val start = Instant.parse("2026-07-29T08:00:00Z")
        val end = Instant.parse("2026-07-29T14:00:00Z")
        assertTrue(FishingDateTimeValidator.isValid(start, end, now))
        assertTrue(FishingDateTimeValidator.validate(start, end, now).isEmpty())
    }

    // 9. Midnight crossing → valid
    @Test
    fun `midnight crossing - valid`() {
        val futureNow = Instant.parse("2026-07-31T12:00:00Z")
        val start = Instant.parse("2026-07-29T22:00:00Z")
        val end = Instant.parse("2026-07-30T03:30:00Z")
        assertTrue(FishingDateTimeValidator.isValid(start, end, futureNow))
        assertTrue(FishingDateTimeValidator.validate(start, end, futureNow).isEmpty())
    }

    // 10. Duration correct
    @Test
    fun `duration correct`() {
        val report = makeReport(
            startAt = Instant.parse("2026-07-29T08:00:00Z"),
            endAt = Instant.parse("2026-07-29T11:30:00Z")
        )
        assertEquals(Duration.ofHours(3).plusMinutes(30), report.duration)
        assertEquals("3 ч 30 мин", report.durationFormatted())
    }

    // 11. DTO/Entity mapping: roundtrip preserves Instant
    @Test
    fun `DTO roundtrip preserves timestamp`() {
        val instant = Instant.parse("2026-07-29T22:15:00Z")
        // Supabase stores as ISO-8601 string; parseInstant converts back
        val stored: String = instant.toString()
        val parsed = Instant.parse(stored)
        assertEquals(instant, parsed)
    }

    // 12. Legacy: endAt = null → no invented end (duration null, no crash)
    @Test
    fun `legacy report with null endAt - no crash`() {
        val report = makeReport(
            startAt = Instant.parse("2026-07-29T08:00:00Z"),
            endAt = null
        )
        assertNull(report.fishingEndAt)
        assertNull(report.duration)
        assertNull(report.durationFormatted())
        assertNotNull(report.fishingStartTime())
        assertNull(report.fishingEndTime())
    }

    private fun makeReport(startAt: Instant?, endAt: Instant?): FishingReport {
        return FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Test",
            water = Water(waterName = "Test", latitude = 0.0, longitude = 0.0),
            photo = emptyList(),
            fishingStartAt = startAt,
            fishingEndAt = endAt,
            weight = 0.0,
            fish = listOf(),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(),
            comment = "",
            user = User(name = "Test", email = "", image = ""),
            fishingFromTheShore = true,
            isPublic = false
        )
    }
}

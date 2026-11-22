package com.example.fishing.model

import com.example.fishing.testutil.testReport
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FishSuggestionsTest {

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val catalog = listOf(
        FishSpecies("rutilus_rutilus", "Плотва", listOf("сорога"), isPopular = true, sortOrder = 1),
        FishSpecies("carassius_gibelio", "Карась", listOf("караси"), isPopular = true, sortOrder = 2),
        FishSpecies("esox_lucius", "Щука", emptyList(), isPopular = true, sortOrder = 3),
        FishSpecies("abramis_brama", "Лещ", listOf("подлещик", "лещи"), isPopular = true, sortOrder = 4),
        FishSpecies("perca_fluviatilis", "Окунь", emptyList(), isPopular = true, sortOrder = 5),
        FishSpecies("silurus_glanis", "Сом", emptyList(), isPopular = false, sortOrder = 9)
    )

    private fun report(
        fish: List<Fish>,
        at: Instant = Instant.parse("2026-05-01T08:00:00Z"),
        owner: UUID = userId
    ) = testReport(userId = owner, fishingStartAt = at, fish = fish)

    private fun fish(name: String, count: Int = 1, speciesId: String? = null) =
        Fish(id = UUID.randomUUID(), name = name, count = count, speciesId = speciesId)

    @Test
    fun `counts reports, not caught fish`() {
        val reports = listOf(
            report(listOf(fish("Карась", count = 200)), at = Instant.parse("2026-05-03T08:00:00Z")),
            report(listOf(fish("Плотва")), at = Instant.parse("2026-05-01T08:00:00Z")),
            report(listOf(fish("Плотва")), at = Instant.parse("2026-05-02T08:00:00Z"))
        )

        val result = recentsFrom(reports, catalog, userId, limit = 3)

        assertEquals(listOf("Плотва", "Карась"), result.map { it.name })
        assertEquals(2, result.first().reportCount)
        assertEquals(1, result.last().reportCount)
    }

    @Test
    fun `newer report wins when report counts are equal`() {
        val reports = listOf(
            report(listOf(fish("Щука")), at = Instant.parse("2026-05-01T08:00:00Z")),
            report(listOf(fish("Окунь")), at = Instant.parse("2026-05-02T08:00:00Z"))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(listOf("Окунь", "Щука"), result.map { it.name })
    }

    @Test
    fun `respects the limit`() {
        val reports = listOf(
            report(listOf(fish("Щука")), at = Instant.parse("2026-05-04T08:00:00Z")),
            report(listOf(fish("Окунь")), at = Instant.parse("2026-05-03T08:00:00Z")),
            report(listOf(fish("Карась")), at = Instant.parse("2026-05-02T08:00:00Z")),
            report(listOf(fish("Плотва")), at = Instant.parse("2026-05-01T08:00:00Z"))
        )

        assertEquals(3, recentsFrom(reports, catalog, userId, limit = 3).size)
        assertEquals(4, recentsFrom(reports, catalog, userId, limit = 10).size)
    }

    @Test
    fun `ignores other users reports`() {
        val reports = listOf(
            report(listOf(fish("Щука")), owner = UUID.randomUUID()),
            report(listOf(fish("Плотва")))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(listOf("Плотва"), result.map { it.name })
    }

    @Test
    fun `merges aliases into one catalog species`() {
        val reports = listOf(
            report(listOf(fish("Подлещик")), at = Instant.parse("2026-05-02T08:00:00Z")),
            report(listOf(fish("Лещ")), at = Instant.parse("2026-05-01T08:00:00Z"))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(1, result.size)
        assertEquals("Лещ", result.single().name)
        assertEquals("abramis_brama", result.single().speciesId)
        assertEquals(2, result.single().reportCount)
    }

    @Test
    fun `counts a species once per report even with two spellings`() {
        val reports = listOf(
            report(listOf(fish("Лещ"), fish("Подлещик")))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(1, result.single().reportCount)
    }

    @Test
    fun `groups by species id even when names differ`() {
        val reports = listOf(
            report(listOf(fish("Щучка", speciesId = "esox_lucius"))),
            report(listOf(fish("Щука", speciesId = "esox_lucius")))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(1, result.size)
        assertEquals("esox_lucius", result.single().speciesId)
        assertEquals(2, result.single().reportCount)
    }

    @Test
    fun `keeps custom names and skips blanks`() {
        val reports = listOf(
            report(listOf(fish("Пескарик"), fish("  ")))
        )

        val result = recentsFrom(reports, catalog, userId)

        assertEquals(1, result.size)
        assertEquals("Пескарик", result.single().name)
        assertNull(result.single().speciesId)
    }

    @Test
    fun `popular fallback filters, sorts and limits`() {
        val popular = popularFrom(catalog, limit = 3)

        assertEquals(listOf("Плотва", "Карась", "Щука"), popular.map { it.name })
        assertTrue(popular.all { it.source == FishSuggestionSource.POPULAR })
        assertTrue(popular.all { it.speciesId != null })
        assertEquals(5, popularFrom(catalog, limit = 10).size)
    }
}

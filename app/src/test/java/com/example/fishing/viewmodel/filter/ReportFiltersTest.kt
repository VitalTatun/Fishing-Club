package com.example.fishing.viewmodel.filter

import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.testutil.testMarker
import com.example.fishing.testutil.testReport
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFiltersTest {

    @Test
    fun `empty filters match every report and marker`() {
        val report = testReport()
        val marker = testMarker()

        assertTrue(ReportFilters().matches(report, emptySet()))
        assertTrue(ReportFilters().matches(marker, emptySet()))
        assertFalse(ReportFilters().isActive)
    }

    @Test
    fun `favorites filter uses the provided id set`() {
        val report = testReport()
        val marker = testMarker(id = report.id)
        val filters = ReportFilters(isFavoritesSelected = true)

        assertTrue(filters.matches(report, setOf(report.id)))
        assertTrue(filters.matches(marker, setOf(marker.id)))
        assertFalse(filters.matches(report, emptySet()))
        assertFalse(filters.matches(marker, emptySet()))
    }

    @Test
    fun `trophy paid catch and method filters are shared between reports and markers`() {
        val trophy = testReport(type = FishingType.HAUL, isPaidWater = true)
        val marker = testMarker(
            id = trophy.id,
            type = FishingType.HAUL,
            isPaidWater = true
        )
        val filters = ReportFilters(
            isTrophySelected = true,
            isPaidSelected = true,
            selectedCatch = "Pike",
            selectedMethod = FishingMethod.SPINNING
        )

        assertTrue(filters.matches(trophy, emptySet()))
        assertTrue(filters.matches(marker, emptySet()))
        assertFalse(filters.matches(trophy.copy(type = FishingType.FISHING_LOG), emptySet()))
        assertFalse(filters.matches(trophy.copy(water = trophy.water.copy(isPaid = false)), emptySet()))
        assertFalse(
            filters.matches(
                trophy.copy(fish = trophy.fish.map { it.copy(name = "Perch") }),
                emptySet()
            )
        )
        assertFalse(
            filters.matches(trophy.copy(fishingMethod = FishingMethod.FEEDER), emptySet())
        )
        assertFalse(filters.matches(marker.copy(fishNames = listOf("Perch")), emptySet()))
        assertFalse(filters.matches(marker.copy(fishingMethod = FishingMethod.FEEDER), emptySet()))
    }

    @Test
    fun `species filter matches by catalog id or canonical name`() {
        val speciesId = "esox_lucius"
        val report = testReport(
            fish = listOf(Fish(UUID.randomUUID(), "щуки", 1, speciesId))
        )
        val marker = testMarker().copy(
            fishNames = listOf("Щука"),
            fishSpeciesIds = setOf(speciesId)
        )

        val byId = ReportFilters(selectedSpeciesId = speciesId)
        assertTrue(byId.matches(report, emptySet()))
        assertTrue(byId.matches(marker, emptySet()))
        assertFalse(byId.matches(report.copy(fish = emptyList()), emptySet()))
        assertFalse(byId.matches(marker.copy(fishSpeciesIds = emptySet()), emptySet()))

        val byName = ReportFilters(selectedCatch = "Щука")
        assertTrue(byName.matches(marker, emptySet()))
        // The report kept the raw alias, only its catalog id resolves it.
        assertFalse(byName.matches(report, emptySet()))
        assertTrue(ReportFilters(selectedCatch = "щуки").matches(report, emptySet()))
    }

    @Test
    fun `species and own-only filters make filters active`() {
        assertTrue(ReportFilters(selectedSpeciesId = "esox_lucius").isActive)
        assertTrue(ReportFilters(isOwnOnly = true).isActive)
    }

    @Test
    fun `active count groups the fish condition and ignores favorites`() {
        assertEquals(0, ReportFilters(isFavoritesSelected = true).activeCount)
        assertEquals(
            3,
            ReportFilters(
                isOwnOnly = true,
                isTrophySelected = true,
                selectedCatch = "Щука",
                selectedSpeciesId = "esox_lucius"
            ).activeCount
        )
    }

    @Test
    fun `search state filters by query fields`() {
        val report = testReport(
            name = "Evening pike",
            waterName = "Deep Lake",
            comment = "Caught on a wobbler"
        )

        assertTrue(ReportSearchState(query = "evening").matches(report, emptySet()))
        assertTrue(ReportSearchState(query = "deep").matches(report, emptySet()))
        assertTrue(ReportSearchState(query = "wobbler").matches(report, emptySet()))
        assertTrue(ReportSearchState(query = "pike").matches(report, emptySet()))
        assertFalse(ReportSearchState(query = "morning").matches(report, emptySet()))
    }

    @Test
    fun `search state filters by calendar day`() {
        val instant = Instant.parse("2026-05-01T08:00:00Z")
        val report = testReport(fishingStartAt = instant)
        val sameDay = ReportSearchState(selectedDateMillis = instant.toEpochMilli())

        assertTrue(sameDay.matches(report, emptySet()))
        assertFalse(sameDay.matches(report.copy(fishingStartAt = null), emptySet()))
        assertFalse(
            sameDay.matches(
                report.copy(fishingStartAt = instant.plusSeconds(2 * 24 * 60 * 60)),
                emptySet()
            )
        )
    }

    @Test
    fun `search state combines query date and filters`() {
        val report = testReport(
            type = FishingType.HAUL,
            isPaidWater = true,
            name = "Trophy pike"
        )
        val state = ReportSearchState(
            query = "trophy",
            selectedDateMillis = report.fishingStartAt!!.toEpochMilli(),
            filters = ReportFilters(
                isTrophySelected = true,
                isPaidSelected = true,
                selectedMethod = FishingMethod.SPINNING
            )
        )

        assertTrue(state.matches(report, emptySet()))
        assertFalse(
            state.matches(report.copy(fishingMethod = FishingMethod.FEEDER), emptySet())
        )
        assertFalse(
            state.matches(report.copy(water = report.water.copy(isPaid = false)), emptySet())
        )
    }

    @Test
    fun `empty search state is detected for the placeholder`() {
        assertTrue(ReportSearchState().isEmpty)
        assertFalse(ReportSearchState(query = "x").isEmpty)
        assertFalse(ReportSearchState(selectedDateMillis = 1L).isEmpty)
        assertFalse(
            ReportSearchState(filters = ReportFilters(isTrophySelected = true)).isEmpty
        )
    }
}

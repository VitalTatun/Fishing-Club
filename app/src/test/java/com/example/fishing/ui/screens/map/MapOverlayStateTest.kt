package com.example.fishing.ui.screens.map

import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class MapOverlayStateTest {

    private fun marker(id: UUID = UUID.randomUUID()): MarkerDomain = MarkerDomain(
        id = id,
        name = "m",
        waterName = "w",
        waterLat = 0.0,
        waterLng = 0.0,
        type = FishingType.FISHING_LOG,
        fishingMethod = FishingMethod.NONE,
        fishingStartAt = null,
        isPublic = true
    )

    @Test
    fun `no cache and loading shows Loading even if a previous error exists`() {
        assertEquals(
            MapOverlayState.Loading,
            mapOverlayState(
                rawMarkers = emptyList(),
                filteredMarkers = emptyList(),
                isLoading = true,
                errorMessage = "previous failure",
                isFilterActive = false
            )
        )
    }

    @Test
    fun `no cache and refresh failure is Error`() {
        assertEquals(
            MapOverlayState.Error,
            mapOverlayState(
                rawMarkers = emptyList(),
                filteredMarkers = emptyList(),
                isLoading = false,
                errorMessage = "network down",
                isFilterActive = false
            )
        )
    }

    @Test
    fun `no cache and load finished is Empty`() {
        assertEquals(
            MapOverlayState.Empty,
            mapOverlayState(
                rawMarkers = emptyList(),
                filteredMarkers = emptyList(),
                isLoading = false,
                errorMessage = null,
                isFilterActive = false
            )
        )
    }

    @Test
    fun `raw markers exist but filter matched none is FilteredEmpty`() {
        val m = marker()
        assertEquals(
            MapOverlayState.FilteredEmpty,
            mapOverlayState(
                rawMarkers = listOf(m),
                filteredMarkers = emptyList(),
                isLoading = false,
                errorMessage = null,
                isFilterActive = true
            )
        )
    }

    @Test
    fun `cached markers with failed refresh keeps map visible`() {
        val m = marker()
        assertEquals(
            MapOverlayState.None,
            mapOverlayState(
                rawMarkers = listOf(m),
                filteredMarkers = listOf(m),
                isLoading = false,
                errorMessage = "network down",
                isFilterActive = false
            )
        )
    }

    @Test
    fun `markers present without filters is None`() {
        val m = marker()
        assertEquals(
            MapOverlayState.None,
            mapOverlayState(
                rawMarkers = listOf(m),
                filteredMarkers = listOf(m),
                isLoading = false,
                errorMessage = null,
                isFilterActive = false
            )
        )
    }
}
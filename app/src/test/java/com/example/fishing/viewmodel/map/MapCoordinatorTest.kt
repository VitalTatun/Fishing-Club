package com.example.fishing.viewmodel.map

import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testMarker
import com.example.fishing.viewmodel.filter.ReportFilters
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapCoordinatorTest {

    @Test
    fun `loads markers and clears the loading state`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val marker = testMarker()
        repository.mapMarkersValue = listOf(marker)
        val coordinator = MapCoordinator(repository, backgroundScope)

        coordinator.loadMapMarkers()
        advanceUntilIdle()

        assertEquals(1, repository.refreshMapMarkersCallCount)
        assertEquals(listOf(marker), coordinator.mapMarkers.value)
        assertFalse(coordinator.mapIsLoading.value)
        assertFalse(coordinator.mapIsRefreshing.value)
        assertNull(coordinator.mapRefreshError.value)
    }

    @Test
    fun `cached markers survive a failed refresh`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val marker = testMarker()
        repository.mapMarkersValue = listOf(marker)
        repository.refreshMapMarkersException = RuntimeException("offline")
        val coordinator = MapCoordinator(repository, backgroundScope)

        coordinator.loadMapMarkers()
        advanceUntilIdle()

        assertEquals(listOf(marker), coordinator.mapMarkers.value)
        assertNotNull(coordinator.mapRefreshError.value)
        assertFalse(coordinator.mapIsLoading.value)
    }

    @Test
    fun `filters and camera are reset on clear`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        repository.mapMarkersValue = listOf(testMarker())
        val coordinator = MapCoordinator(repository, backgroundScope)
        coordinator.loadMapMarkers()
        advanceUntilIdle()

        coordinator.updateMapFilters {
            ReportFilters(
                isTrophySelected = true,
                selectedCatch = "Pike"
            )
        }
        coordinator.mapLastCenterLat = 55.0
        coordinator.mapLastCenterLon = 37.0
        coordinator.mapLastZoom = 12.0

        coordinator.clear()

        assertEquals(ReportFilters(), coordinator.mapFilters.value)
        assertTrue(coordinator.mapMarkers.value.isEmpty())
        assertTrue(coordinator.mapIsLoading.value)
        assertNull(coordinator.mapRefreshError.value)
        assertNull(coordinator.mapLastCenterLat)
        assertNull(coordinator.mapLastCenterLon)
        assertEquals(6.0, coordinator.mapLastZoom, 0.0)
    }
}

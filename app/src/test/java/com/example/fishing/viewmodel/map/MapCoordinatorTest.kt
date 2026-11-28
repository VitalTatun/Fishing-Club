package com.example.fishing.viewmodel.map

import com.example.fishing.data.report.MapMarkerPage
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MapMarkerFilters
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.SelectedFish
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.viewmodel.filter.ReportFilters
import com.example.fishing.viewmodel.session.UserSession
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapCoordinatorTest {

    private val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val zoneA = MapBounds(south = 53.0, north = 54.0, west = 27.0, east = 28.0)
    private val zoneB = MapBounds(south = 55.0, north = 56.0, west = 29.0, east = 30.0)

    private fun TestScope.coordinator(
        repository: FakeFishingRepository,
        cacheLimit: Int = 2000
    ): MapCoordinator {
        val auth = FakeAuthRepository().apply { sessionUser = testUser(userId) }
        return MapCoordinator(
            repository = repository,
            session = UserSession(auth),
            scope = backgroundScope,
            cacheLimit = cacheLimit
        )
    }

    @Test
    fun `loads the visible viewport and publishes markers`() = runTest {
        val repository = FakeFishingRepository()
        val marker = marker(lat = 53.5, lng = 27.5)
        repository.loadPublicMapMarkersResult = page(marker)
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        assertEquals(1, repository.loadPublicMapMarkersCallCount)
        assertEquals(listOf(marker), coordinator.mapMarkers.value)
        assertFalse(coordinator.mapIsLoading.value)
        assertNull(coordinator.mapRefreshError.value)
        // Requested with the 25% margin around the visible box.
        assertEquals(zoneA.expandedBy(0.25), repository.loadPublicMapMarkersRequests.single().bounds)
    }

    @Test
    fun `panning inside the loaded margin does not request again`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertEquals(1, repository.loadPublicMapMarkersCallCount)

        val inside = MapBounds(south = 53.1, north = 54.1, west = 27.1, east = 28.1)
        coordinator.onViewportChanged(inside, 12.0)
        advanceTimeBy(500)

        assertEquals(1, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `below the zoom threshold nothing is requested`() = runTest {
        val repository = FakeFishingRepository()
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 9.9)
        advanceTimeBy(500)

        assertEquals(0, repository.loadPublicMapMarkersCallCount)
        assertTrue(coordinator.mapIsZoomedOut.value)
    }

    @Test
    fun `rapid camera moves are debounced into one request`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(100)
        coordinator.onViewportChanged(zoneA.expandedBy(0.01), 12.0)
        advanceTimeBy(100)
        coordinator.onViewportChanged(zoneA.expandedBy(0.02), 12.0)
        advanceTimeBy(500)

        assertEquals(1, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `previously loaded markers survive panning and duplicates are merged`() = runTest {
        val repository = FakeFishingRepository()
        val first = marker(lat = 53.5, lng = 27.5)
        val second = marker(lat = 55.5, lng = 29.5)
        repository.loadPublicMapMarkersResult = page(first)
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        repository.loadPublicMapMarkersResult = page(first.copy(name = "updated"), second)
        coordinator.onViewportChanged(zoneB, 12.0)
        advanceTimeBy(500)

        val markers = coordinator.mapMarkers.value
        assertEquals(2, markers.size)
        assertEquals("updated", markers.first { it.id == first.id }.name)
        assertTrue(markers.any { it.id == second.id })
    }

    @Test
    fun `a truncated response does not mark the zone as loaded`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = MapMarkerPage(
            markers = listOf(marker(lat = 53.5, lng = 27.5)),
            truncated = true
        )
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertTrue(coordinator.mapTruncated.value)

        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        assertEquals(2, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `applying filters clears the cache and reloads with server filters`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertEquals(1, coordinator.mapMarkers.value.size)

        val filters = ReportFilters(
            isTrophySelected = true,
            selectedFish = listOf(SelectedFish("Щука", "esox_lucius")),
            selectedMethod = FishingMethod.SPINNING
        )
        coordinator.onFiltersApplied(filters)
        advanceTimeBy(500)

        val request = repository.loadPublicMapMarkersRequests.last()
        assertEquals(
            MapMarkerFilters(
                trophyOnly = true,
                method = FishingMethod.SPINNING,
                speciesIds = setOf("esox_lucius")
            ),
            request.filters
        )
        assertEquals(filters, coordinator.mapFilters.value)
    }

    @Test
    fun `hidden map clears the cache and shown map loads again`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertEquals(1, coordinator.mapMarkers.value.size)

        coordinator.onMapHidden()
        assertTrue(coordinator.mapMarkers.value.isEmpty())

        coordinator.onMapShown()
        advanceTimeBy(500)

        assertEquals(2, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `refresh drops the cache and reloads the current viewport`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertEquals(1, repository.loadPublicMapMarkersCallCount)

        coordinator.onRefreshRequested()
        advanceTimeBy(500)

        assertEquals(2, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `a failed refresh keeps previously loaded markers`() = runTest {
        val repository = FakeFishingRepository()
        val marker = marker(lat = 53.5, lng = 27.5)
        repository.loadPublicMapMarkersResult = page(marker)
        val coordinator = coordinator(repository)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        repository.loadPublicMapMarkersResult = null
        repository.loadPublicMapMarkersException = RuntimeException("offline")
        coordinator.onViewportChanged(zoneB, 12.0)
        advanceTimeBy(500)

        assertEquals(listOf(marker), coordinator.mapMarkers.value)
        assertNotNull(coordinator.mapRefreshError.value)
    }

    @Test
    fun `the cache is capped by dropping markers farthest from the camera`() = runTest {
        val repository = FakeFishingRepository()
        val near = marker(lat = 53.4, lng = 27.4)
        val middle = marker(lat = 53.5, lng = 27.5)
        val far = marker(lat = 53.9, lng = 27.9)
        val farthest = marker(lat = 54.0, lng = 28.0)
        repository.loadPublicMapMarkersResult = page(near, middle, far, farthest)
        val coordinator = coordinator(repository, cacheLimit = 2)

        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        val ids = coordinator.mapMarkers.value.map { it.id }.toSet()
        assertEquals(2, ids.size)
        assertTrue(ids.containsAll(listOf(near.id, middle.id)))
    }

    @Test
    fun `own markers are merged on top of public ones and survive without network`() = runTest {
        val repository = FakeFishingRepository()
        val publicMarker = marker(lat = 53.5, lng = 27.5)
        val ownMarker = marker(lat = 60.0, lng = 30.0, isPublic = false)
        repository.loadPublicMapMarkersResult = page(publicMarker)
        repository.ownMapMarkersValue = listOf(ownMarker)
        val coordinator = coordinator(repository)

        coordinator.onUserChanged(userId)
        advanceTimeBy(500)
        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        assertEquals(2, coordinator.mapMarkers.value.size)

        repository.loadPublicMapMarkersException = RuntimeException("offline")
        coordinator.onRefreshRequested()
        advanceTimeBy(500)

        assertTrue(coordinator.mapMarkers.value.any { it.id == ownMarker.id })
    }

    @Test
    fun `own markers obey client-side filters`() = runTest {
        val repository = FakeFishingRepository()
        repository.ownMapMarkersValue = listOf(
            marker(lat = 53.5, lng = 27.5, type = FishingType.HAUL)
        )
        val coordinator = coordinator(repository)

        coordinator.onUserChanged(userId)
        advanceTimeBy(500)
        coordinator.onFiltersApplied(ReportFilters(isTrophySelected = true))
        advanceTimeBy(500)

        assertEquals(1, coordinator.mapMarkers.value.size)

        coordinator.onFiltersApplied(ReportFilters(isPaidSelected = true))
        advanceTimeBy(500)

        assertTrue(coordinator.mapMarkers.value.isEmpty())
    }

    @Test
    fun `own-only shows own markers and skips the public request`() = runTest {
        val repository = FakeFishingRepository()
        val publicMarker = marker(lat = 53.5, lng = 27.5)
        val ownMarker = marker(lat = 60.0, lng = 30.0, isPublic = false)
        repository.loadPublicMapMarkersResult = page(publicMarker)
        repository.ownMapMarkersValue = listOf(ownMarker)
        val coordinator = coordinator(repository)

        coordinator.onUserChanged(userId)
        advanceTimeBy(500)
        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)
        assertEquals(1, repository.loadPublicMapMarkersCallCount)

        coordinator.onFiltersApplied(ReportFilters(isOwnOnly = true))
        advanceTimeBy(500)

        assertEquals(listOf(ownMarker), coordinator.mapMarkers.value)
        assertEquals(1, repository.loadPublicMapMarkersCallCount)
        assertTrue(coordinator.mapShowingOwnOnly.value)

        coordinator.onFiltersApplied(ReportFilters())
        advanceTimeBy(500)

        assertEquals(2, repository.loadPublicMapMarkersCallCount)
    }

    @Test
    fun `clear resets filters and camera`() = runTest {
        val repository = FakeFishingRepository()
        repository.loadPublicMapMarkersResult = page(marker(lat = 53.5, lng = 27.5))
        val coordinator = coordinator(repository)
        coordinator.onUserChanged(userId)
        coordinator.onMapShown()
        coordinator.onViewportChanged(zoneA, 12.0)
        advanceTimeBy(500)

        coordinator.updateMapFilters { ReportFilters(isTrophySelected = true) }
        advanceTimeBy(500)
        coordinator.mapLastCenterLat = 55.0
        coordinator.mapLastCenterLon = 37.0
        coordinator.mapLastZoom = 12.0

        coordinator.clear()

        assertEquals(ReportFilters(), coordinator.mapFilters.value)
        assertTrue(coordinator.mapMarkers.value.isEmpty())
        assertFalse(coordinator.mapIsLoading.value)
        assertNull(coordinator.mapRefreshError.value)
        assertNull(coordinator.mapLastCenterLat)
        assertNull(coordinator.mapLastCenterLon)
        assertEquals(6.0, coordinator.mapLastZoom, 0.0)
    }

    private fun marker(
        lat: Double,
        lng: Double,
        isPublic: Boolean = true,
        type: FishingType = FishingType.FISHING_LOG
    ) = MarkerDomain(
        id = UUID.randomUUID(),
        name = "Marker",
        waterName = "Lake",
        waterLat = lat,
        waterLng = lng,
        type = type,
        fishingMethod = FishingMethod.SPINNING,
        fishingStartAt = null,
        isPublic = isPublic,
        fishNames = listOf("Pike")
    )

    private fun page(vararg markers: MarkerDomain) = MapMarkerPage(markers.toList(), false)

    private fun testUser(id: UUID) = com.example.fishing.model.User(
        id = id,
        name = "Test",
        email = "test@example.com",
        image = ""
    )
}

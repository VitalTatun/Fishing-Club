package com.example.fishing.data.report

import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.ReportDetailsEntity
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MapMarkerFilters
import com.example.fishing.testutil.FakeFishCatalogRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkersStoreTest {

    private val queries = mockk<ReportRemoteQueries>()
    private val dao = mockk<ReportDetailsDao>()
    private val catalog = FakeFishCatalogRepository(
        listOf(FishSpecies(id = SPECIES_ID, canonicalName = "Щука"))
    )
    private val store = DefaultMapMarkersStore(queries, dao, catalog)

    private val userId = UUID.randomUUID()
    private val bounds = MapBounds(south = 53.0, north = 54.0, west = 27.0, east = 28.0)

    @Test
    fun `public markers are mapped and truncation is preserved`() = runTest {
        val marker = RemoteMapMarker(
            id = UUID.randomUUID(),
            name = "Вечерняя щука",
            waterName = "Озеро",
            waterLat = 53.5,
            waterLng = 27.5,
            type = FishingType.HAUL.name,
            fishingMethod = FishingMethod.SPINNING.name,
            fishingStartAt = "2026-05-01T08:00:00Z",
            isPaidWater = true,
            fishNames = listOf("Щука"),
            fishSpeciesIds = listOf(SPECIES_ID)
        )
        coEvery {
            queries.fetchPublicMapMarkers(bounds, MapMarkerFilters(), 300)
        } returns RemoteMapMarkerPage(listOf(marker), truncated = true)

        val page = store.loadPublicMarkers(bounds, MapMarkerFilters(), 300).getOrThrow()

        assertTrue(page.truncated)
        val mapped = page.markers.single()
        assertEquals(marker.id, mapped.id)
        assertEquals(FishingType.HAUL, mapped.type)
        assertEquals(FishingMethod.SPINNING, mapped.fishingMethod)
        assertEquals(setOf(SPECIES_ID), mapped.fishSpeciesIds)
        assertTrue(mapped.isPublic)
        assertTrue(mapped.isPaidWater)
    }

    @Test
    fun `public marker failure is returned as a result`() = runTest {
        coEvery {
            queries.fetchPublicMapMarkers(bounds, MapMarkerFilters(), 300)
        } throws IOException("offline")

        val result = store.loadPublicMarkers(bounds, MapMarkerFilters(), 300)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `own markers resolve canonical fish names from the catalog`() = runTest {
        every { dao.getOwnMapMarkers(userId) } returns flowOf(
            listOf(
                detailsEntity(
                    fishJson = """
                        [{"id":"${UUID.randomUUID()}","fishing_id":"$REPORT_ID",
                          "name":"щуки","count":1,"species_id":"$SPECIES_ID"}]
                    """.trimIndent()
                )
            )
        )

        val marker = store.observeOwnMarkers(userId).first().single()

        assertEquals(listOf("Щука"), marker.fishNames)
        assertEquals(setOf(SPECIES_ID), marker.fishSpeciesIds)
        assertEquals(FishingType.FISHING_LOG, marker.type)
    }

    @Test
    fun `own markers keep raw names when the species is unknown`() = runTest {
        every { dao.getOwnMapMarkers(userId) } returns flowOf(
            listOf(
                detailsEntity(
                    fishJson = """
                        [{"id":"${UUID.randomUUID()}","fishing_id":"$REPORT_ID",
                          "name":"Неизвестная рыба","count":1}]
                    """.trimIndent()
                )
            )
        )

        val marker = store.observeOwnMarkers(userId).first().single()

        assertEquals(listOf("Неизвестная рыба"), marker.fishNames)
        assertTrue(marker.fishSpeciesIds.isEmpty())
    }

    @Test
    fun `own reports without coordinates are skipped`() = runTest {
        every { dao.getOwnMapMarkers(userId) } returns flowOf(
            listOf(detailsEntity(waterLat = null, waterLng = null))
        )

        val markers = store.observeOwnMarkers(userId).first()

        assertTrue(markers.isEmpty())
    }

    @Test
    fun `own markers keep visibility so private reports stay private`() = runTest {
        every { dao.getOwnMapMarkers(userId) } returns flowOf(
            listOf(detailsEntity(isPublic = false))
        )

        val marker = store.observeOwnMarkers(userId).first().single()

        assertFalse(marker.isPublic)
    }

    private fun detailsEntity(
        waterLat: Double? = 53.5,
        waterLng: Double? = 27.5,
        isPublic: Boolean = true,
        fishJson: String = "[]"
    ) = ReportDetailsEntity(
        id = REPORT_ID,
        userId = userId,
        publishedAt = null,
        type = FishingType.FISHING_LOG.name,
        name = "Тестовый отчёт",
        waterName = "Озеро",
        waterLat = waterLat,
        waterLng = waterLng,
        waterPaid = false,
        spotLat = null,
        spotLng = null,
        fishingStartAt = "2026-05-01T08:00:00Z",
        fishingEndAt = null,
        weight = 1.0,
        fishingMethod = FishingMethod.SPINNING.name,
        comment = null,
        shore = true,
        isPublic = isPublic,
        imageUrls = emptyList(),
        fishJson = fishJson,
        baitsJson = "[]",
        authorName = null,
        authorAvatar = null,
        createdAt = null,
        likesCount = 0
    )

    private companion object {
        const val SPECIES_ID = "esox_lucius"
        val REPORT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000042")
    }
}

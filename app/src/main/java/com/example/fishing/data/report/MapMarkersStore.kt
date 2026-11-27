package com.example.fishing.data.report

import com.example.fishing.data.FishCatalogRepository
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MapMarkerFilters
import com.example.fishing.model.MarkerDomain
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Public page of a viewport together with the "limit was hit" flag. */
data class MapMarkerPage(
    val markers: List<MarkerDomain>,
    val truncated: Boolean
)

/**
 * Read model for the map.
 *
 * Public markers come from the network and are bounded by the visible viewport;
 * the caller keeps them in a session cache. Own markers are a projection of the
 * offline `report_details` cache and never leave the device.
 */
interface MapMarkersStore {
    suspend fun loadPublicMarkers(
        bounds: MapBounds,
        filters: MapMarkerFilters,
        limit: Int
    ): Result<MapMarkerPage>

    /** Own public and private reports that have water coordinates. */
    fun observeOwnMarkers(userId: UUID): Flow<List<MarkerDomain>>
}

@Singleton
class DefaultMapMarkersStore @Inject constructor(
    private val reportRemoteQueries: ReportRemoteQueries,
    private val reportDetailsDao: ReportDetailsDao,
    private val fishCatalogRepository: FishCatalogRepository
) : MapMarkersStore {

    override suspend fun loadPublicMarkers(
        bounds: MapBounds,
        filters: MapMarkerFilters,
        limit: Int
    ): Result<MapMarkerPage> = try {
        val page = reportRemoteQueries.fetchPublicMapMarkers(bounds, filters, limit)
        Result.success(
            MapMarkerPage(
                markers = page.markers.map(RemoteMapMarker::toDomain),
                truncated = page.truncated
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * The catalog flow makes the mapping react to aliases and canonical names
     * becoming available after the first launch.
     */
    override fun observeOwnMarkers(userId: UUID): Flow<List<MarkerDomain>> =
        combine(
            reportDetailsDao.getOwnMapMarkers(userId),
            fishCatalogRepository.observeAll()
        ) { rows, species ->
            val canonicalNames = species.associate { it.id to it.canonicalName }
            rows.mapNotNull { it.toMarkerDomainOrNull(canonicalNames) }
        }
}

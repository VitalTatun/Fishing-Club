package com.example.fishing.data.report

import com.example.fishing.data.supabase.BaitDto
import com.example.fishing.data.supabase.FavoriteDto
import com.example.fishing.data.supabase.FishDto
import com.example.fishing.data.supabase.FishingDto
import com.example.fishing.data.supabase.LikeDto
import com.example.fishing.data.supabase.PhotoDto
import com.example.fishing.data.supabase.ProfileDto
import com.example.fishing.model.FishingType
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MapMarkerFilters
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * One report as stored on the server, together with its child rows and author.
 * Mapping into Room/domain types is the mapper's job, so the transport layer
 * stays free of local-cache concerns.
 */
data class RemoteReport(
    val fishing: FishingDto,
    val fish: List<FishDto>,
    val baits: List<BaitDto>,
    val photos: List<PhotoDto>,
    val author: ProfileDto?
)

data class RemoteReportPage(
    val reports: List<RemoteReport>,
    val hasMore: Boolean
)

/**
 * One public marker as returned by the viewport query. Transport types stay
 * strings; the mapper turns them into [com.example.fishing.model.MarkerDomain].
 */
data class RemoteMapMarker(
    val id: UUID,
    val name: String,
    val waterName: String,
    val waterLat: Double,
    val waterLng: Double,
    val type: String,
    val fishingMethod: String?,
    val fishingStartAt: String?,
    val isPaidWater: Boolean,
    val fishNames: List<String>,
    val fishSpeciesIds: List<String>
)

data class RemoteMapMarkerPage(
    val markers: List<RemoteMapMarker>,
    /**
     * True when the server had more rows than the requested limit. The caller
     * must not treat the viewport as fully loaded in that case.
     */
    val truncated: Boolean
)

/**
 * Read side of the Supabase schema. Keeps table names, projections and ordering
 * out of the repository, and gives tests a seam without a Supabase project.
 */
interface ReportRemoteQueries {
    /** One page of [userId]'s own reports, newest fishing time first. */
    suspend fun fetchOwnReportPage(userId: UUID, offset: Int, limit: Int): RemoteReportPage

    /**
     * One page of [userId]'s published reports, newest fishing time first.
     * Used by the public user screen, so drafts never show up here.
     */
    suspend fun fetchUserReportPage(userId: UUID, offset: Int, limit: Int): RemoteReportPage

    /** Public projection of one profile (no email), or null when it is gone. */
    suspend fun fetchPublicProfile(userId: UUID): ProfileDto?

    /**
     * Reports by id, with children. [ids] is used for the child queries so the
     * payload matches the requested set even if a row is missing.
     */
    suspend fun fetchReportsByIds(ids: List<UUID>): List<RemoteReport>

    suspend fun fetchReport(id: UUID): RemoteReport?

    /** Ids of reports bookmarked by [userId]. */
    suspend fun fetchFavoriteReportIds(userId: UUID): List<UUID>

    /** Ids of reports liked by [userId]. */
    suspend fun fetchLikedReportIds(userId: UUID): List<UUID>

    /**
     * Public markers inside [bounds] that pass [filters], newest-first order is
     * intentionally absent so the database can use the coordinate index.
     * Requests `limit + 1` rows to report truncation.
     */
    suspend fun fetchPublicMapMarkers(
        bounds: MapBounds,
        filters: MapMarkerFilters,
        limit: Int
    ): RemoteMapMarkerPage
}

@Singleton
class SupabaseReportRemoteQueries @Inject constructor(
    private val supabase: SupabaseClient
) : ReportRemoteQueries {

    override suspend fun fetchOwnReportPage(
        userId: UUID,
        offset: Int,
        limit: Int
    ): RemoteReportPage {
        // One extra row is requested to detect whether a further page exists.
        val fetched = supabase.postgrest["fishing"].select {
            filter { eq("user_id", userId) }
            order("fishing_start_at", Order.DESCENDING)
            range(offset.toLong(), (offset + limit).toLong())
        }.decodeList<FishingDto>()

        val hasMore = fetched.size > limit
        val page = if (hasMore) fetched.take(limit) else fetched
        if (page.isEmpty()) return RemoteReportPage(emptyList(), hasMore = false)
        return RemoteReportPage(buildReports(page, page.map { it.id }), hasMore)
    }

    override suspend fun fetchUserReportPage(
        userId: UUID,
        offset: Int,
        limit: Int
    ): RemoteReportPage {
        val fetched = supabase.postgrest["fishing"].select {
            filter {
                eq("user_id", userId)
                eq("is_public", true)
            }
            order("fishing_start_at", Order.DESCENDING)
            range(offset.toLong(), (offset + limit).toLong())
        }.decodeList<FishingDto>()

        val hasMore = fetched.size > limit
        val page = if (hasMore) fetched.take(limit) else fetched
        if (page.isEmpty()) return RemoteReportPage(emptyList(), hasMore = false)
        return RemoteReportPage(buildReports(page, page.map { it.id }), hasMore)
    }

    override suspend fun fetchPublicProfile(userId: UUID): ProfileDto? =
        supabase.postgrest["profiles_public"].select {
            filter { eq("id", userId) }
            limit(1)
        }.decodeList<ProfileDto>().firstOrNull()

    override suspend fun fetchReportsByIds(ids: List<UUID>): List<RemoteReport> {
        if (ids.isEmpty()) return emptyList()

        val fishings = supabase.postgrest["fishing"].select {
            filter { isIn("id", ids) }
            order("fishing_start_at", Order.DESCENDING)
        }.decodeList<FishingDto>()

        return buildReports(fishings, ids)
    }

    override suspend fun fetchReport(id: UUID): RemoteReport? {
        val fishing = supabase.postgrest["fishing"].select {
            filter { eq("id", id) }
        }.decodeList<FishingDto>().firstOrNull() ?: return null

        return buildReports(listOf(fishing), listOf(id)).firstOrNull()
    }

    override suspend fun fetchFavoriteReportIds(userId: UUID): List<UUID> =
        supabase.postgrest["favorites"].select {
            filter { eq("user_id", userId) }
        }.decodeList<FavoriteDto>().map { it.fishingId }

    override suspend fun fetchLikedReportIds(userId: UUID): List<UUID> =
        supabase.postgrest["likes"].select {
            filter { eq("user_id", userId) }
        }.decodeList<LikeDto>()
            .filter { it.userId == userId }
            .map { it.fishingId }

    override suspend fun fetchPublicMapMarkers(
        bounds: MapBounds,
        filters: MapMarkerFilters,
        limit: Int
    ): RemoteMapMarkerPage {
        // An inner join is only needed to filter parents by fish species. Without
        // the species filter a plain embed keeps fishless reports on the map.
        val fishEmbed = if (filters.speciesIds.isNotEmpty()) {
            "fishing_fish!inner(id,fishing_id,name,species_id,fish_species(canonical_name))"
        } else {
            "fishing_fish(id,fishing_id,name,species_id,fish_species(canonical_name))"
        }
        val columns = Columns.raw(
            "id,user_id,name,water_name,water_lat,water_lng,type,fishing_method," +
                "fishing_start_at,water_paid,is_public,$fishEmbed"
        )

        val fetched = supabase.postgrest["fishing"].select(columns = columns) {
            filter {
                eq("is_public", true)
                gte("water_lat", bounds.south)
                lte("water_lat", bounds.north)
                gte("water_lng", bounds.west)
                lte("water_lng", bounds.east)
                if (filters.trophyOnly) eq("type", FishingType.HAUL.name)
                if (filters.paidOnly) eq("water_paid", true)
                filters.method?.let { eq("fishing_method", it.name) }
                when (filters.speciesIds.size) {
                    1 -> eq("fishing_fish.species_id", filters.speciesIds.first())
                    in 2..Int.MAX_VALUE ->
                        isIn("fishing_fish.species_id", filters.speciesIds.toList())
                }
            }
            // One extra row tells the caller whether the viewport was truncated.
            limit((limit + 1).toLong())
        }.decodeList<FishingDto>()

        val truncated = fetched.size > limit
        val page = if (truncated) fetched.take(limit) else fetched
        return RemoteMapMarkerPage(
            markers = page.mapNotNull { it.toRemoteMapMarker() },
            truncated = truncated
        )
    }

    /**
     * Resolves child rows only for the given report ids so the payload scales
     * with the page size instead of the whole database. All child queries run
     * concurrently.
     */
    private suspend fun buildReports(
        fishings: List<FishingDto>,
        fishingIds: List<UUID>
    ): List<RemoteReport> = coroutineScope {
        val fishDeferred = async {
            supabase.postgrest["fishing_fish"].select {
                filter { isIn("fishing_id", fishingIds) }
            }.decodeList<FishDto>()
        }
        val baitsDeferred = async {
            supabase.postgrest["fishing_baits"].select {
                filter { isIn("fishing_id", fishingIds) }
            }.decodeList<BaitDto>()
        }
        val photosDeferred = async {
            supabase.postgrest["fishing_photos"].select {
                filter { isIn("fishing_id", fishingIds) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<PhotoDto>()
        }
        val profilesDeferred = async {
            val authorIds = fishings.map { it.userId }
            // Public projection: id, name, avatar_url, created_at (no email).
            supabase.postgrest["profiles_public"].select {
                filter { isIn("id", authorIds) }
            }.decodeList<ProfileDto>()
        }

        val fishByFishing = fishDeferred.await().groupBy { it.fishingId }
        val baitsByFishing = baitsDeferred.await().groupBy { it.fishingId }
        val photosByFishing = photosDeferred.await().groupBy { it.fishingId }
        val profilesById = profilesDeferred.await().associateBy { it.id }

        fishings.map { dto ->
            RemoteReport(
                fishing = dto,
                fish = fishByFishing[dto.id].orEmpty(),
                baits = baitsByFishing[dto.id].orEmpty(),
                photos = photosByFishing[dto.id].orEmpty(),
                author = profilesById[dto.userId]
            )
        }
    }
}

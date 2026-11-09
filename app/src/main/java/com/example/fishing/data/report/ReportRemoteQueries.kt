package com.example.fishing.data.report

import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.data.supabase.BaitDto
import com.example.fishing.data.supabase.FavoriteDto
import com.example.fishing.data.supabase.FishDto
import com.example.fishing.data.supabase.FishingDto
import com.example.fishing.data.supabase.LikeDto
import com.example.fishing.data.supabase.PhotoDto
import com.example.fishing.data.supabase.ProfileDto
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
 * Read side of the Supabase schema. Keeps table names, projections and ordering
 * out of the repository, and gives tests a seam without a Supabase project.
 */
interface ReportRemoteQueries {
    /** One page of [userId]'s own reports, newest fishing time first. */
    suspend fun fetchOwnReportPage(userId: UUID, offset: Int, limit: Int): RemoteReportPage

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

    /** Public reports plus [userId]'s own ones, ready for the map cache. */
    suspend fun fetchMapMarkers(userId: UUID): List<MarkerEntity>
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

    override suspend fun fetchMapMarkers(userId: UUID): List<MarkerEntity> {
        val fishings = supabase.postgrest["fishing"].select(
            columns = Columns.raw("*, fishing_fish(*)")
        ) {
            filter {
                or {
                    eq("is_public", true)
                    eq("user_id", userId)
                }
            }
            order("fishing_start_at", Order.DESCENDING)
        }.decodeList<FishingDto>()

        return fishings.map { dto -> dto.toMarkerEntity(dto.fish.map { it.name }) }
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

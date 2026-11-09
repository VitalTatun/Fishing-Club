package com.example.fishing.data

import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.ReportLikeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Outcome of the first home page refresh. [loadedCount] is the number of reports
 * (own + bookmarked) written to the cache, so the UI can tell "genuinely empty"
 * apart from "the cache has not caught up with the network yet".
 */
data class HomeRefreshResult(
    val hasMore: Boolean,
    val loadedCount: Int
) {
    companion object {
        val Empty = HomeRefreshResult(hasMore = false, loadedCount = 0)
    }
}

interface FishingRepository {
    fun getHomeReports(userId: UUID): Flow<List<FishingReport>>
    suspend fun loadFavoriteReports(userId: UUID): List<FishingReport>

    /**
     * Ids of the reports the user has bookmarked. Backed by the local cache, so
     * the UI can render the correct state immediately and react to every write.
     */
    fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>>

    /** Pulls bookmarked ids from the backend into the local cache. */
    suspend fun refreshFavoriteIds(userId: UUID)
    fun getMapMarkers(): Flow<List<MarkerDomain>>
    fun getReportDetails(id: UUID): Flow<FishingReport?>
    /** Loads the first page of home reports, replacing the cache. */
    suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult
    /** Appends the next page of home reports. Returns whether more pages exist. */
    suspend fun loadMoreHomeReports(userId: UUID): Boolean
    suspend fun refreshMapMarkers(): Result<List<MarkerDomain>>
    suspend fun addFavorite(report: FishingReport): Result<Unit>
    suspend fun removeFavorite(reportId: UUID): Result<Unit>
    /**
     * Saves (and optionally publishes) a report. [onPhotoUploaded] is called with
     * the number of photos already in storage and the total count, so callers can
     * persist progress that survives process death.
     */
    suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)? = null
    ): Result<Unit>

    /**
     * True only when the server holds [report] **and every photo it references**.
     *
     * Used by the upload queue to recover a report whose publish response was
     * lost, and to refuse to drop the queue row when an attachment is missing.
     * A report-level existence check alone is not enough.
     */
    suspend fun isReportSynced(report: FishingReport): Boolean

    suspend fun deleteReport(id: UUID): Result<Unit>
    suspend fun getPhotoSignedUrl(storagePath: String): String?
    /** Resolves many storage paths in as few requests as possible. */
    suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String>
    fun isStoragePath(path: String): Boolean
    suspend fun addLike(reportId: UUID): Result<Unit>
    suspend fun removeLike(reportId: UUID): Result<Unit>
    fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>>
    suspend fun refreshLikes(userId: UUID)
}

class MockFishingRepository : FishingRepository {
    private val favoriteReports = mutableListOf<FishingReport>()
    private val reports = MockData.sampleReports.toMutableList()
    private val _likeStates = MutableStateFlow<Map<UUID, ReportLikeState>>(emptyMap())
    private val _favoriteIds = MutableStateFlow<Set<UUID>>(emptySet())

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> = flow {
        delay(1000)
        emit((reports + favoriteReports).distinctBy { it.id })
    }



    override fun getMapMarkers(): Flow<List<MarkerDomain>> = flow {
        delay(1000)
        emit(reports.map { it.toMarkerDomain() })
    }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = flow {
        delay(1000)
        emit(reports.firstOrNull { it.id == id })
    }

    override suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)?
    ): Result<Unit> {
        reports.add(0, report)
        onPhotoUploaded?.invoke(report.photos.size, report.photos.size)
        return Result.success(Unit)
    }

    override suspend fun isReportSynced(report: FishingReport): Boolean =
        reports.any { it.id == report.id }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        reports.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult {
        // Mock — no-op, single page
        return HomeRefreshResult(hasMore = false, loadedCount = (reports + favoriteReports).size)
    }

    override suspend fun loadMoreHomeReports(userId: UUID): Boolean = false

    override suspend fun loadFavoriteReports(userId: UUID): List<FishingReport> {
        return favoriteReports.toList()
    }

    override fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>> = _favoriteIds.asStateFlow()

    override suspend fun refreshFavoriteIds(userId: UUID) {
        _favoriteIds.value = favoriteReports.map { it.id }.toSet()
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        return Result.success(reports.map { it.toMarkerDomain() })
    }

    override suspend fun addFavorite(report: FishingReport): Result<Unit> {
        if (favoriteReports.none { it.id == report.id }) {
            favoriteReports.add(report)
        }
        _favoriteIds.value = favoriteReports.map { it.id }.toSet()
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> {
        favoriteReports.removeAll { it.id == reportId }
        _favoriteIds.value = favoriteReports.map { it.id }.toSet()
        return Result.success(Unit)
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? {
        return storagePath
    }

    override suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String> {
        return storagePaths.associateWith { it }
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
    }

    override suspend fun addLike(reportId: UUID): Result<Unit> {
        _likeStates.update { current ->
            val prev = current[reportId] ?: ReportLikeState(reportId)
            if (prev.isLiked) current else current + (reportId to prev.copy(
                isLiked = true,
                likesCount = prev.likesCount + 1
            ))
        }
        return Result.success(Unit)
    }

    override suspend fun removeLike(reportId: UUID): Result<Unit> {
        _likeStates.update { current ->
            val prev = current[reportId] ?: ReportLikeState(reportId, isLiked = true, likesCount = 1)
            if (!prev.isLiked) current else current + (reportId to prev.copy(
                isLiked = false,
                likesCount = (prev.likesCount - 1).coerceAtLeast(0)
            ))
        }
        return Result.success(Unit)
    }

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> = _likeStates.asStateFlow()

    override suspend fun refreshLikes(userId: UUID) {
        // Mock — no-op
    }

    private fun FishingReport.toMarkerDomain(): MarkerDomain {
        return MarkerDomain(
            id = id,
            name = name,
            waterName = water.waterName,
            waterLat = water.latitude,
            waterLng = water.longitude,
            type = type,
            fishingMethod = fishingMethod,
            fishingStartAt = fishingStartAt,
            isPublic = isPublic,
            isPaidWater = water.isPaid,
            fishNames = fish.map { it.name }
        )
    }
}

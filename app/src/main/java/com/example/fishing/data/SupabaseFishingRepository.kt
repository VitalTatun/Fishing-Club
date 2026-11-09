package com.example.fishing.data

import com.example.fishing.data.report.FavoritesStore
import com.example.fishing.data.report.HomeFeedStore
import com.example.fishing.data.report.LikesStore
import com.example.fishing.data.report.MarkerStore
import com.example.fishing.data.report.PhotoUrlProvider
import com.example.fishing.data.report.ReportDetailsStore
import com.example.fishing.data.report.ReportWriter
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.ReportLikeState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Supabase-backed implementation of [FishingRepository].
 *
 * The contract is unchanged; each domain lives in its own component under
 * `data.report` so it can be tested and evolved in isolation. This class only
 * wires the delegating calls together.
 */
@Singleton
class SupabaseFishingRepository @Inject constructor(
    private val homeFeedStore: HomeFeedStore,
    private val reportDetailsStore: ReportDetailsStore,
    private val markerStore: MarkerStore,
    private val favoritesStore: FavoritesStore,
    private val likesStore: LikesStore,
    private val reportWriter: ReportWriter,
    private val photoUrlProvider: PhotoUrlProvider
) : FishingRepository {

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> =
        homeFeedStore.getHomeReports(userId)

    override suspend fun loadFavoriteReports(userId: UUID): List<FishingReport> =
        favoritesStore.loadFavoriteReports(userId)

    override fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>> =
        favoritesStore.getFavoriteReportIds(userId)

    override suspend fun refreshFavoriteIds(userId: UUID) =
        favoritesStore.refreshFavoriteIds(userId)

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = markerStore.getMapMarkers()

    override fun getReportDetails(id: UUID): Flow<FishingReport?> =
        reportDetailsStore.getReportDetails(id)

    override suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult =
        homeFeedStore.refreshHomeReports(userId)

    override suspend fun loadMoreHomeReports(userId: UUID): Boolean =
        homeFeedStore.loadMoreHomeReports(userId)

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> =
        markerStore.refreshMapMarkers()

    override suspend fun addFavorite(report: FishingReport): Result<Unit> =
        favoritesStore.addFavorite(report.id)

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> =
        favoritesStore.removeFavorite(reportId)

    override suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)?
    ): Result<Unit> = reportWriter.saveReport(report, onPhotoUploaded)

    override suspend fun isReportSynced(report: FishingReport): Boolean =
        reportWriter.isReportSynced(report)

    override suspend fun deleteReport(id: UUID): Result<Unit> = reportWriter.deleteReport(id)

    override suspend fun getPhotoSignedUrl(storagePath: String): String? =
        photoUrlProvider.getSignedUrl(storagePath)

    override suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String> =
        photoUrlProvider.getSignedUrls(storagePaths)

    override fun isStoragePath(path: String): Boolean = photoUrlProvider.isStoragePath(path)

    override suspend fun addLike(reportId: UUID): Result<Unit> = likesStore.addLike(reportId)

    override suspend fun removeLike(reportId: UUID): Result<Unit> = likesStore.removeLike(reportId)

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> =
        likesStore.getLikeStates(userId)

    override suspend fun refreshLikes(userId: UUID) = likesStore.refreshLikes(userId)

    /**
     * Not part of [FishingRepository] yet: used by the detail screen to learn
     * whether the report still exists on the server.
     */
    suspend fun refreshReportDetails(id: UUID): Boolean =
        reportDetailsStore.refreshReportDetails(id)
}

package com.example.fishing.data

import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.ReportDetailsEntity
import com.example.fishing.data.report.buildPublishPayload
import com.example.fishing.data.report.FavoritesStore
import com.example.fishing.data.report.LikesStore
import com.example.fishing.data.report.PhotoUrlProvider
import com.example.fishing.data.report.RemoteReport
import com.example.fishing.data.report.ReportRemoteQueries
import com.example.fishing.data.report.SupabaseDateFormats
import com.example.fishing.data.report.toDomain
import com.example.fishing.data.report.toFishDto
import com.example.fishing.data.report.toFishingDto
import com.example.fishing.data.report.toMarkerEntity
import com.example.fishing.data.report.toReportDetailsEntity
import com.example.fishing.data.supabase.*
import com.example.fishing.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseFishingRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val reportRemoteDataSource: ReportRemoteDataSource,
    private val reportRemoteQueries: ReportRemoteQueries,
    private val likesStore: LikesStore,
    private val favoritesStore: FavoritesStore,
    private val markerDao: MarkerDao,
    private val reportDetailsDao: ReportDetailsDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val likeDao: LikeDao,
    private val database: AppDatabase,
    private val photoUrlProvider: PhotoUrlProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FishingRepository {

    // Pagination cursor for the home feed. `homeLoadedOwnCount` counts only the
    // current user's own reports already cached (bookmarks are always loaded whole).
    private var homeLoadedOwnCount = 0
    private var homeHasMore = false

    private companion object {
        const val PAGE_SIZE = 20
    }

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> =
        reportDetailsDao.getHomeReports(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult {
        try {
            val likesSnapshotSeq = likesStore.currentMutationVersion()
            val page = reportRemoteQueries.fetchOwnReportPage(userId, offset = 0, limit = PAGE_SIZE)

            database.withTransaction {
                val entities = likesStore.preserveRecentLikes(page.reports.map { it.toEntity() }, likesSnapshotSeq)
                reportDetailsDao.deleteAll()
                reportDetailsDao.insertAll(entities)
            }
            homeLoadedOwnCount = page.reports.size
            homeHasMore = page.hasMore
            return HomeRefreshResult(
                hasMore = page.hasMore,
                loadedCount = page.reports.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun loadMoreHomeReports(userId: UUID): Boolean {
        if (!homeHasMore) return false
        val likesSnapshotSeq = likesStore.currentMutationVersion()
        val page = reportRemoteQueries.fetchOwnReportPage(
            userId,
            offset = homeLoadedOwnCount,
            limit = PAGE_SIZE
        )
        database.withTransaction {
            reportDetailsDao.insertAll(likesStore.preserveRecentLikes(page.reports.map { it.toEntity() }, likesSnapshotSeq))
        }
        homeLoadedOwnCount += page.reports.size
        homeHasMore = page.hasMore
        return page.hasMore
    }

    override suspend fun loadFavoriteReports(userId: UUID): List<FishingReport> {
        val snapshotSeq = favoritesStore.currentMutationVersion()
        val ids = reportRemoteQueries.fetchFavoriteReportIds(userId)
        favoritesStore.persistServerLinks(userId, ids, snapshotSeq)
        if (ids.isEmpty()) return emptyList()
        return reportRemoteQueries.fetchReportsByIds(ids).map { it.toEntity().toDomain() }
    }

    override fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>> =
        favoritesStore.getFavoriteReportIds(userId)

    override suspend fun refreshFavoriteIds(userId: UUID) =
        favoritesStore.refreshFavoriteIds(userId)

    private fun RemoteReport.toEntity(): ReportDetailsEntity =
        toReportDetailsEntity(authRepository::resolveImageUrl)

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = markerDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = reportDetailsDao.getById(id).map { entity ->
        entity?.toDomain()
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        val userId = authRepository.currentUser()?.id
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return try {
            val entities = reportRemoteQueries.fetchMapMarkers(userId)
            // Cache is replaced atomically so a mid-refresh failure cannot wipe
            // previously cached markers out from under the user.
            database.withTransaction {
                markerDao.deleteAll()
                markerDao.insertAll(entities)
            }
            Result.success(entities.map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshReportDetails(id: UUID): Boolean {
        if (authRepository.currentUser() == null) return false
        val likesSnapshotSeq = likesStore.currentMutationVersion()

        val remote = reportRemoteQueries.fetchReport(id) ?: return false
        val entity = remote.toEntity()
        database.withTransaction {
            reportDetailsDao.insert(likesStore.preserveRecentLikes(listOf(entity), likesSnapshotSeq).single())
        }
        return true
    }

    override suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)?
    ): Result<Unit> = withContext(ioDispatcher) {
        val totalPhotos = report.photos.size
        var uploadedPhotos = report.photos.count { isStoragePath(it.url) }
        onPhotoUploaded?.invoke(uploadedPhotos, totalPhotos)
        val now = SupabaseDateFormats.formatDate(Date())
        val dto = report.toFishingDto(now)
        val finalDto = dto.copy(
            isPublic = report.isPublic,
            // Keep the original publish time when a published report is saved again;
            // the RPC does the same with coalesce(f.published_at, excluded.published_at).
            publishedAt = if (report.isPublic) (dto.publishedAt ?: now) else null
        )
        val uploadedPaths = mutableListOf<String>()
        var reportExistedOnServer = false
        var publishedOnServer = false

        try {
            // 1. Never write a report for another user.
            val currentUser = authRepository.currentUser()
                ?: throw IllegalStateException("Not authenticated")
            if (currentUser.id != report.userId) {
                throw IllegalStateException("Cannot save a report for another user")
            }

            // 2. Upload only new photos. Storage is not transactional, so doing this
            // before the report is written lets us compensate a failure safely.
            val hasNewPhotos = report.photos.any { !isStoragePath(it.url) }
            if (hasNewPhotos) {
                // Uploading with upsert=true may overwrite objects that an existing
                // report still references, so their cleanup on failure is not safe.
                // If we cannot tell whether the report exists, keep the files.
                reportExistedOnServer = runCatching {
                    reportRemoteDataSource.reportExists(report.id)
                }.getOrDefault(true)
            }
            val storagePaths = report.photos.map { photo ->
                ensureActive()
                if (isStoragePath(photo.url)) {
                    photo.url
                } else {
                    val ext = File(photo.url).extension.ifEmpty { "jpg" }
                    // Deterministic storage path using stable photo.id
                    val storagePath = "${report.userId}/${report.id}/${photo.id}.$ext"
                    val file = File(photo.url)
                    if (!file.exists()) {
                        throw IllegalStateException("Photo file not found: ${photo.url}")
                    }
                    reportRemoteDataSource.uploadPhoto(storagePath, file.readBytes())
                    uploadedPaths.add(storagePath)
                    uploadedPhotos++
                    onPhotoUploaded?.invoke(uploadedPhotos, totalPhotos)
                    storagePath
                }
            }

            ensureActive()

            // 3. Write the report, its children and the publish flag atomically in a
            // single RPC transaction: either all rows land or none of them do.
            val fishDtos = report.fish.map { it.toFishDto(report.id) }
            val baitDtos = report.bait.map { BaitDto(report.id, it.name) }
            val photoDtos = report.photos.mapIndexed { index, photo ->
                PhotoDto(
                    id = photo.id,
                    fishingId = report.id,
                    storagePath = storagePaths[index],
                    sortOrder = index
                )
            }

            val publishResult = reportRemoteDataSource.publishReport(
                buildPublishPayload(finalDto, fishDtos, baitDtos, photoDtos)
            )
            publishedOnServer = true

            // 4. Photos removed from the form are no longer referenced by the DB.
            // Deleting their objects is best-effort: a failure here only leaves
            // harmless orphans and must not fail an already published report.
            val removedPhotoPaths = publishResult.removedPhotoPaths
            if (removedPhotoPaths.isNotEmpty()) {
                runCatching { reportRemoteDataSource.deletePhotos(removedPhotoPaths) }
            }

            val markerEntity = finalDto.toMarkerEntity(report.fish.map { it.name })
            val detailsEntity = finalDto.toReportDetailsEntity(
                fish = fishDtos,
                baits = baitDtos,
                photos = photoDtos,
                authorName = report.user.name,
                authorAvatar = report.user.image
            )

            database.withTransaction {
                // The upsert DTO never carries likes_count (see FishingDto), so the
                // locally mirrored counter must be read inside the transaction to
                // avoid clobbering a like that landed while this save was running.
                val preservedLikesCount = reportDetailsDao.getByIdOneShot(report.id)?.likesCount ?: 0
                reportDetailsDao.insert(detailsEntity.copy(likesCount = preservedLikesCount))
                if (finalDto.isPublic || finalDto.userId == authRepository.currentUser()?.id) {
                    markerDao.insertAll(listOf(markerEntity))
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            // Cancelling a save is not an error. Never delete files here: the user
            // may retry, and deterministic paths overwrite previous uploads.
            throw e
        } catch (e: Exception) {
            // Only delete files uploaded in THIS call, and only when the server
            // holds no report that may already reference those deterministic paths.
            if (!reportExistedOnServer && !publishedOnServer && uploadedPaths.isNotEmpty()) {
                runCatching { reportRemoteDataSource.deletePhotos(uploadedPaths) }
            }
            Result.failure(e)
        }
    }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        return try {
            // 1. Fetch storage paths before deleting the report.
            // ON DELETE CASCADE would remove these records, making paths unrecoverable.
            val storagePaths = try {
                supabase.postgrest["fishing_photos"].select {
                    filter { eq("fishing_id", id) }
                }.decodeList<PhotoDto>().map { it.storagePath }
            } catch (e: Exception) {
                // If we can't even fetch paths, we can't proceed with safe cleanup.
                return Result.failure(e)
            }

            // 2. Delete Storage objects BEFORE deleting DB record.
            // If this fails (e.g. network), we don't delete the DB, allowing retry.
            if (storagePaths.isNotEmpty()) {
                try {
                    supabase.storage.from("fishing_photos").delete(storagePaths)
                } catch (e: Exception) {
                    // Suppress "404 Not Found" or similar errors that indicate files are already gone.
                    // If it's a real network/auth error, let it throw to stop the flow.
                    val errorBody = e.message ?: ""
                    val isNotFoundError = errorBody.contains("not found", ignoreCase = true) ||
                            errorBody.contains("404")

                    if (!isNotFoundError) {
                        return Result.failure(e)
                    }
                }
            }

            // 3. Delete the report from Supabase DB.
            // CASCADE removes fishing_fish, fishing_baits, fishing_photos, and favorites.
            supabase.postgrest["fishing"].delete {
                filter { eq("id", id) }
            }

            // 4. Clean local Room cache in a single transaction.
            database.withTransaction {
                reportDetailsDao.deleteById(id)
                markerDao.deleteById(id)
                favoriteReportDao.deleteByReportId(id)
                likeDao.deleteByReportId(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFavorite(report: FishingReport): Result<Unit> =
        favoritesStore.addFavorite(report.id)

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> =
        favoritesStore.removeFavorite(reportId)

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> =
        likesStore.getLikeStates(userId)

    override suspend fun refreshLikes(userId: UUID) = likesStore.refreshLikes(userId)

    override suspend fun addLike(reportId: UUID): Result<Unit> = likesStore.addLike(reportId)

    override suspend fun removeLike(reportId: UUID): Result<Unit> = likesStore.removeLike(reportId)

    override suspend fun getPhotoSignedUrl(storagePath: String): String? =
        photoUrlProvider.getSignedUrl(storagePath)

    override suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String> =
        photoUrlProvider.getSignedUrls(storagePaths)

    override fun isStoragePath(path: String): Boolean = photoUrlProvider.isStoragePath(path)
}

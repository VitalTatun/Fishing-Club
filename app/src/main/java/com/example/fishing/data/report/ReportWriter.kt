package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.IoDispatcher
import com.example.fishing.data.ReportRemoteDataSource
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.supabase.BaitDto
import com.example.fishing.data.supabase.PhotoDto
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Write side of reports: uploads new photos, publishes through the atomic RPC
 * and mirrors the result into Room. Storage is not transactional, so every
 * failure path has to compensate explicitly — that logic lives here.
 */
interface ReportWriter {
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
     * Confirms that the server holds [report] **and every photo it references**.
     *
     * The upload queue calls this before dropping its local row: a published
     * report with a missing attachment must stay queued so the next attempt can
     * upload it. Conversely, a lower-level `reportExists` check is never enough
     * on its own — it says nothing about the photos.
     */
    suspend fun isReportSynced(report: FishingReport): Boolean

    suspend fun deleteReport(id: UUID): Result<Unit>
}

@Singleton
class SupabaseReportWriter @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportRemoteDataSource: ReportRemoteDataSource,
    private val reportDetailsDao: ReportDetailsDao,
    private val markerDao: MarkerDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val likeDao: LikeDao,
    private val database: AppDatabase,
    private val photoUrlProvider: PhotoUrlProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReportWriter {

    override suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)?
    ): Result<Unit> = withContext(ioDispatcher) {
        val totalPhotos = report.photos.size
        var uploadedPhotos = report.photos.count { photoUrlProvider.isStoragePath(it.url) }
        onPhotoUploaded?.invoke(uploadedPhotos, totalPhotos)
        val now = SupabaseDateFormats.formatDate(Date())
        val dto = report.toFishingDto(now)
        val finalDto = dto.copy(
            isPublic = report.isPublic,
            // Keep the original publish time when a published report is saved again;
            // the RPC does the same with coalesce(f.published_at, excluded.published_at).
            publishedAt = if (report.isPublic) (dto.publishedAt ?: now) else null
        )
        // Deterministic paths are what make a retry idempotent: the same photo
        // always maps to the same Storage object, whichever attempt uploads it.
        val expectedPaths = report.photos.map { storagePathFor(report, it) }
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

            // 2. Upload only photos that are not in Storage yet. Checking each
            // object (instead of "the report exists") means a partially uploaded
            // retry still uploads every missing photo.
            val hasNewPhotos = report.photos.any { !photoUrlProvider.isStoragePath(it.url) }
            if (hasNewPhotos) {
                // Uploading with upsert=true may overwrite objects that an existing
                // report still references, so their cleanup on failure is not safe.
                // If we cannot tell whether the report exists, keep the files.
                reportExistedOnServer = runCatching {
                    reportRemoteDataSource.reportExists(report.id)
                }.getOrDefault(true)
            }
            val storagePaths = report.photos.mapIndexed { index, photo ->
                ensureActive()
                if (photoUrlProvider.isStoragePath(photo.url)) {
                    photo.url
                } else {
                    val storagePath = expectedPaths[index]
                    if (!photoExists(storagePath)) {
                        val file = File(photo.url)
                        if (!file.exists()) {
                            throw IllegalStateException("Photo file not found: ${photo.url}")
                        }
                        reportRemoteDataSource.uploadPhoto(storagePath, file.readBytes())
                        uploadedPaths.add(storagePath)
                    }
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

            // The RPC returning means the database transaction committed. The
            // upload queue still verifies the report and its attachments with
            // [isReportSynced] before it drops its payload: a lost response must
            // not be mistaken for a lost report, and a missing photo must keep
            // the row retryable.

            Result.success(Unit)
        } catch (e: CancellationException) {
            // Cancelling a save is not an error. Never delete files here: the user
            // may retry, and deterministic paths overwrite previous uploads.
            throw e
        } catch (e: Exception) {
            // A timeout after the RPC committed leaves a report that references
            // the objects uploaded in this very call. Re-check existence instead
            // of trusting the exception, otherwise compensation would delete
            // photos the server already points at.
            val existsOnServer = runCatching {
                reportRemoteDataSource.reportExists(report.id)
            }.getOrDefault(true)
            // Only delete files uploaded in THIS call, and only when the server
            // holds no report that may already reference those deterministic paths.
            if (!reportExistedOnServer && !publishedOnServer && !existsOnServer &&
                uploadedPaths.isNotEmpty()
            ) {
                runCatching { reportRemoteDataSource.deletePhotos(uploadedPaths) }
            }
            Result.failure(e)
        }
    }

    override suspend fun isReportSynced(report: FishingReport): Boolean =
        withContext(ioDispatcher) {
            val expectedPaths = report.photos.map { storagePathFor(report, it) }
            try {
                if (!reportRemoteDataSource.reportExists(report.id)) {
                    return@withContext false
                }
                val attached = reportRemoteDataSource.reportPhotoPaths(report.id).toSet()
                // Attachments are what the report actually shows; objects prove
                // the bytes are still in Storage.
                expectedPaths.all { it in attached } &&
                    expectedPaths.all { reportRemoteDataSource.photoExists(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Unknown state is not confirmation: keep the queue row.
                false
            }
        }

    /** Stable Storage path for a photo; a local file keeps its deterministic name. */
    private fun storagePathFor(report: FishingReport, photo: FishingPhoto): String =
        if (photoUrlProvider.isStoragePath(photo.url)) {
            photo.url
        } else {
            val extension = File(photo.url).extension.ifEmpty { "jpg" }
            "${report.userId}/${report.id}/${photo.id}.$extension"
        }

    /**
     * Conservative per-object probe: if Storage cannot be asked, the caller
     * uploads the photo (upsert is idempotent) instead of skipping it.
     */
    private suspend fun photoExists(storagePath: String): Boolean =
        try {
            reportRemoteDataSource.photoExists(storagePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        return try {
            reportRemoteDataSource.deleteReport(id)

            // Clean local Room cache in a single transaction.
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
}

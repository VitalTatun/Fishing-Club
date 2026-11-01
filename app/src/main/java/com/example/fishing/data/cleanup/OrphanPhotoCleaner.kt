package com.example.fishing.data.cleanup

import com.example.fishing.data.AuthRepository
import com.example.fishing.data.IoDispatcher
import com.example.fishing.data.supabase.PhotoPathDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.FileObject
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One object in the private photo bucket. */
data class StoragePhotoObject(
    val path: String,
    /** Null for folders; cleaned up only when known and old enough. */
    val createdAt: Instant?
)

/** How many objects were inspected and how many orphaned ones were removed. */
data class OrphanPhotoCleanupReport(
    val scanned: Int,
    val deleted: Int
)

/**
 * Removes photos of the signed-in user that are old enough and no longer
 * referenced by any report. Deletion goes through the Storage API with the
 * user's session, so RLS still guarantees that only own objects are touched.
 */
interface OrphanPhotoCleaner {
    suspend fun cleanup(olderThan: Instant): OrphanPhotoCleanupReport
}

@Singleton
class SupabaseOrphanPhotoCleaner @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OrphanPhotoCleaner {

    override suspend fun cleanup(olderThan: Instant): OrphanPhotoCleanupReport =
        withContext(ioDispatcher) {
            val userId = authRepository.currentUser()?.id
                ?: return@withContext OrphanPhotoCleanupReport(scanned = 0, deleted = 0)

            val bucket = supabase.storage.from(BUCKET)
            val objects = listOwnPhotoObjects(bucket, userId)
            val aged = objects.filter { objectItem ->
                val created = objectItem.createdAt
                created != null && created.isBefore(olderThan)
            }
            if (aged.isEmpty()) {
                return@withContext OrphanPhotoCleanupReport(scanned = 0, deleted = 0)
            }

            val referenced = referencedPaths(aged.map { it.path })
            val orphans = findOrphans(aged, referenced, olderThan)
            orphans.chunked(DELETE_CHUNK).forEach { chunk ->
                bucket.delete(chunk)
            }

            OrphanPhotoCleanupReport(scanned = aged.size, deleted = orphans.size)
        }

    /**
     * Lists `{userId}/{reportId}/{photo}` objects. Folders are opened lazily so a
     * user with many reports does not turn this into one gigantic listing.
     */
    private suspend fun listOwnPhotoObjects(
        bucket: BucketApi,
        userId: UUID
    ): List<StoragePhotoObject> {
        val prefix = userId.toString()
        val folders = listAll(bucket, prefix).filter { it.id == null }
        return folders.flatMap { folder ->
            listAll(bucket, "$prefix/${folder.name}")
                .mapNotNull { file ->
                    // Skip item metadata we cannot age-check: deleting a fresh
                    // upload that is about to be published would be a data loss.
                    file.createdJavaInstant()?.let { created ->
                        StoragePhotoObject("$prefix/${folder.name}/${file.name}", created)
                    }
                }
        }
    }

    private suspend fun listAll(bucket: BucketApi, prefix: String): List<FileObject> {
        val result = mutableListOf<FileObject>()
        var offset = 0
        while (result.size < MAX_LISTED_OBJECTS) {
            val page = bucket.list(prefix) {
                limit = PAGE_SIZE
                this.offset = offset
            }
            result += page
            if (page.size < PAGE_SIZE) break
            offset += PAGE_SIZE
        }
        return result
    }

    /** Storage paths that still have a `fishing_photos` row (RLS applies). */
    private suspend fun referencedPaths(paths: List<String>): Set<String> =
        paths.chunked(REFERENCED_CHUNK).flatMap { chunk ->
            supabase.postgrest["fishing_photos"].select(
                columns = Columns.list("storage_path")
            ) {
                filter { isIn("storage_path", chunk) }
            }.decodeList<PhotoPathDto>().map { it.storagePath }
        }.toSet()

    companion object {
        const val BUCKET = "fishing_photos"
        private const val PAGE_SIZE = 100
        private const val MAX_LISTED_OBJECTS = 5_000
        private const val REFERENCED_CHUNK = 100
        private const val DELETE_CHUNK = 100
    }
}

/**
 * Objects older than [olderThan] whose path has no DB row. Objects of unknown
 * age are never selected: a photo uploaded seconds ago has no row until the
 * publish RPC completes.
 */
internal fun findOrphans(
    objects: List<StoragePhotoObject>,
    referencedPaths: Set<String>,
    olderThan: Instant
): List<String> = objects
    .filter { objectItem ->
        val created = objectItem.createdAt
        created != null &&
            created.isBefore(olderThan) &&
            objectItem.path !in referencedPaths
    }
    .map { it.path }

/** Storage timestamps use `kotlin.time.Instant`; the app works with `java.time`. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun FileObject.createdJavaInstant(): Instant? =
    createdAt?.let { Instant.ofEpochMilli(it.toEpochMilliseconds()) }

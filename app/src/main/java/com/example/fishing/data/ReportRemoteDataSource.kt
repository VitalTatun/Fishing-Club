package com.example.fishing.data

import kotlinx.serialization.json.JsonObject
import java.util.UUID

/** Outcome of the atomic `publish_report` RPC. */
data class PublishReportResult(
    val reportId: UUID?,
    /** Storage objects the server no longer references after the publish. */
    val removedPhotoPaths: List<String>
)

/**
 * Remote half of report publishing: exists-check, photo upload, the atomic
 * publish RPC and best-effort deletion of objects the report dropped.
 *
 * Keeping it behind an interface lets repository tests simulate a failed
 * upload or RPC without a Supabase project.
 */
interface ReportRemoteDataSource {
    /** Whether the server already holds a report with [reportId]. */
    suspend fun reportExists(reportId: UUID): Boolean

    /**
     * Storage paths of the photo rows currently attached to [reportId].
     *
     * A report row alone does not prove that its attachments are complete (a
     * previous attempt may have uploaded every object before the RPC committed,
     * or the opposite). Comparing this set with the expected paths is what makes
     * a retry decide which photos are actually missing.
     */
    suspend fun reportPhotoPaths(reportId: UUID): List<String>

    /**
     * Whether an object already exists at [storagePath].
     *
     * This is deliberately per-path: a report-level check must never be used to
     * skip photos that have not been uploaded yet.
     */
    suspend fun photoExists(storagePath: String): Boolean

    /**
     * Uploads one photo to the private bucket at [storagePath]. Existing objects
     * are overwritten because paths are deterministic (`user/report/photo.ext`).
     */
    suspend fun uploadPhoto(storagePath: String, bytes: ByteArray)

    /** Publishes the report and its children in one transaction. */
    suspend fun publishReport(payload: JsonObject): PublishReportResult

    /**
     * Flips only the publish flag of [reportId].
     *
     * Returns `false` when the server did not update a row (the caller is not the
     * author or the report is gone), so the caller must not pretend the local
     * cache is in sync with the backend.
     */
    suspend fun updateReportVisibility(
        reportId: UUID,
        isPublic: Boolean,
        publishedAt: String?
    ): Boolean

    /** Best-effort removal of photo objects; failures are the caller's concern. */
    suspend fun deletePhotos(paths: List<String>)

    /**
     * Deletes the report row and the storage objects it owns.
     *
     * Photos are removed first so a failed storage call leaves the row in place
     * and the operation can be retried. Objects that are already gone are
     * treated as success; any other storage error aborts the deletion.
     */
    suspend fun deleteReport(reportId: UUID)
}

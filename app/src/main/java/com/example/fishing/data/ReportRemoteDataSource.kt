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
     * Uploads one photo to the private bucket at [storagePath]. Existing objects
     * are overwritten because paths are deterministic (`user/report/photo.ext`).
     */
    suspend fun uploadPhoto(storagePath: String, bytes: ByteArray)

    /** Publishes the report and its children in one transaction. */
    suspend fun publishReport(payload: JsonObject): PublishReportResult

    /** Best-effort removal of photo objects; failures are the caller's concern. */
    suspend fun deletePhotos(paths: List<String>)
}

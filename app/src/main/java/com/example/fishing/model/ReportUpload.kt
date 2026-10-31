package com.example.fishing.model

import java.util.UUID

/** Lifecycle of a report that has been queued for background upload. */
enum class ReportUploadState {
    /** Waiting for a network connection or for the worker to start. */
    QUEUED,

    /** The worker is uploading photos and publishing the report. */
    UPLOADING,

    /** The retry limit was reached; the user can retry or discard the upload. */
    FAILED
}

/**
 * Snapshot of an entry in the persistent upload queue. Progress is stored next
 * to the payload so it survives process death and can be shown on any screen.
 */
data class PendingReportUpload(
    val reportId: UUID,
    val isPublic: Boolean,
    val state: ReportUploadState,
    val attempts: Int,
    val uploadedPhotos: Int,
    val totalPhotos: Int,
    val lastError: String?,
    val createdAt: Long
)

/** Progress of the report that is currently being saved from the form. */
data class ReportSaveProgress(
    val uploadedPhotos: Int,
    val totalPhotos: Int
)

/** One-off notification shown after a background upload finishes. */
sealed interface ReportUploadNotice {
    data class Published(val isPublic: Boolean) : ReportUploadNotice
    data class Failed(val message: String) : ReportUploadNotice
}

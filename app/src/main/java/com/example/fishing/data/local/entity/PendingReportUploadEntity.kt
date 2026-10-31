package com.example.fishing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportUploadState
import java.util.UUID

/**
 * A report that was accepted by the form but not published yet. The payload is
 * stored as JSON rather than relying on Java serialization so it can survive
 * app upgrades without `Serializable` compatibility surprises.
 */
@Entity(tableName = "pending_report_uploads")
data class PendingReportUploadEntity(
    @PrimaryKey val id: UUID,
    val reportJson: String,
    /** Every local photo path referenced by the payload (for success cleanup). */
    val localPhotoPathsJson: String,
    /** Processed copies that are safe to delete when the user cancels. */
    val temporaryPhotoPathsJson: String,
    val isPublic: Boolean,
    val createdAt: Long,
    val attempts: Int,
    /** [ReportUploadState] name. */
    val state: String,
    val uploadedPhotos: Int,
    val totalPhotos: Int,
    val lastError: String?
) {
    fun toDomain(): PendingReportUpload = PendingReportUpload(
        reportId = id,
        isPublic = isPublic,
        state = runCatching { ReportUploadState.valueOf(state) }
            .getOrDefault(ReportUploadState.QUEUED),
        attempts = attempts,
        uploadedPhotos = uploadedPhotos,
        totalPhotos = totalPhotos,
        lastError = lastError,
        createdAt = createdAt
    )
}

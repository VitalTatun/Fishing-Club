package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.PendingReportUploadEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PendingReportUploadDao {
    @Query("SELECT * FROM pending_report_uploads ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingReportUploadEntity>>

    @Query("SELECT * FROM pending_report_uploads ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingReportUploadEntity>

    @Query("SELECT * FROM pending_report_uploads WHERE id = :id")
    suspend fun getById(id: UUID): PendingReportUploadEntity?

    @Query("SELECT * FROM pending_report_uploads WHERE createdAt < :cutoff")
    suspend fun getOlderThan(cutoff: Long): List<PendingReportUploadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingReportUploadEntity)

    @Query("""
        UPDATE pending_report_uploads
        SET state = :state,
            attempts = :attempts,
            uploadedPhotos = 0,
            lastError = NULL
        WHERE id = :id
    """)
    suspend fun markUploading(id: UUID, attempts: Int, state: String = "UPLOADING")

    @Query("""
        UPDATE pending_report_uploads
        SET state = :state,
            attempts = :attempts,
            uploadedPhotos = 0,
            lastError = NULL
        WHERE id = :id
    """)
    suspend fun resetForRetry(id: UUID, attempts: Int = 0, state: String = "QUEUED")

    @Query("""
        UPDATE pending_report_uploads
        SET state = :state,
            attempts = :attempts,
            lastError = :error,
            uploadedPhotos = 0
        WHERE id = :id
    """)
    suspend fun markWaiting(
        id: UUID,
        attempts: Int,
        error: String?,
        state: String = "QUEUED"
    )

    @Query("""
        UPDATE pending_report_uploads
        SET state = :state,
            lastError = :error
        WHERE id = :id
    """)
    suspend fun markFailed(id: UUID, error: String?, state: String = "FAILED")

    @Query("""
        UPDATE pending_report_uploads
        SET uploadedPhotos = :uploaded,
            totalPhotos = :total
        WHERE id = :id
    """)
    suspend fun updateProgress(id: UUID, uploaded: Int, total: Int)

    @Query("DELETE FROM pending_report_uploads WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM pending_report_uploads")
    suspend fun deleteAll()
}

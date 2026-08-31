package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.ReportDetailsEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ReportDetailsDao {
    @Query("SELECT * FROM report_details WHERE id = :id")
    fun getById(id: UUID): Flow<ReportDetailsEntity?>

    @Query("SELECT * FROM report_details ORDER BY fishingTime DESC")
    fun getAll(): Flow<List<ReportDetailsEntity>>

    @Query("SELECT * FROM report_details WHERE userId = :userId ORDER BY fishingTime DESC")
    fun getByUserId(userId: UUID): Flow<List<ReportDetailsEntity>>

    @Query("""
        SELECT report_details.* FROM report_details
        LEFT JOIN favorites ON favorites.reportId = report_details.id
            AND favorites.userId = :userId
        WHERE report_details.userId = :userId OR favorites.reportId IS NOT NULL
        ORDER BY report_details.fishingTime DESC
    """)
    fun getHomeReports(userId: UUID): Flow<List<ReportDetailsEntity>>

    @Query("""
        SELECT report_details.* FROM report_details
        INNER JOIN favorites ON favorites.reportId = report_details.id
        WHERE favorites.userId = :userId
        ORDER BY report_details.fishingTime DESC
    """)
    fun getFavorites(userId: UUID): Flow<List<ReportDetailsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportDetailsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportDetailsEntity)

    @Query("DELETE FROM report_details")
    suspend fun deleteAll()

    @Query("DELETE FROM report_details WHERE id = :id")
    suspend fun deleteById(id: UUID)
}

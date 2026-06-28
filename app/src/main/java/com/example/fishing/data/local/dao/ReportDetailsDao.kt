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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportDetailsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportDetailsEntity)

    @Query("DELETE FROM report_details")
    suspend fun deleteAll()
}
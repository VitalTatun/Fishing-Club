package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.FavoriteReportEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface FavoriteReportDao {
    @Query("SELECT * FROM favorite_report_details ORDER BY fishingTime DESC")
    fun getAll(): Flow<List<FavoriteReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<FavoriteReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: FavoriteReportEntity)

    @Query("DELETE FROM favorite_report_details WHERE id = :fishingId")
    suspend fun deleteById(fishingId: UUID)

    @Query("DELETE FROM favorite_report_details")
    suspend fun deleteAll()
}
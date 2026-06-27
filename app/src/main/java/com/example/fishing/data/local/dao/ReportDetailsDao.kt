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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportDetailsEntity)

    @Query("DELETE FROM report_details")
    suspend fun deleteAll()
}
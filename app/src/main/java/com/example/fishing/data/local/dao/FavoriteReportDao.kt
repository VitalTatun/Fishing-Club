package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.FavoriteReportEntity
import java.util.UUID

@Dao
interface FavoriteReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteReportEntity>)

    @Query("DELETE FROM favorites WHERE userId = :userId AND reportId = :reportId")
    suspend fun delete(userId: UUID, reportId: UUID)

    @Query("DELETE FROM favorites WHERE reportId = :reportId")
    suspend fun deleteByReportId(reportId: UUID)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: UUID)
}

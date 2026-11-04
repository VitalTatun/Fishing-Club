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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteReportEntity>)

    @Query("DELETE FROM favorites WHERE userId = :userId AND reportId = :reportId")
    suspend fun delete(userId: UUID, reportId: UUID)

    @Query("DELETE FROM favorites WHERE reportId = :reportId")
    suspend fun deleteByReportId(reportId: UUID)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: UUID)

    @Query("DELETE FROM favorites WHERE userId = :userId AND reportId NOT IN (:reportIds)")
    suspend fun deleteNotIn(userId: UUID, reportIds: List<UUID>)

    @Query("SELECT reportId FROM favorites WHERE userId = :userId")
    fun getFavoriteIds(userId: UUID): Flow<List<UUID>>

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}

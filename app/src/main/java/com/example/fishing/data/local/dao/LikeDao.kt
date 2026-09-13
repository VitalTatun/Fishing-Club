package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.LikeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(likes: List<LikeEntity>)

    @Query("DELETE FROM likes WHERE userId = :userId AND reportId = :reportId")
    suspend fun delete(userId: UUID, reportId: UUID)

    @Query("DELETE FROM likes WHERE reportId = :reportId")
    suspend fun deleteByReportId(reportId: UUID)

    @Query("DELETE FROM likes WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: UUID)

    @Query("DELETE FROM likes")
    suspend fun deleteAll()

    @Query("SELECT reportId FROM likes WHERE userId = :userId")
    fun getLikedIds(userId: UUID): Flow<List<UUID>>
}

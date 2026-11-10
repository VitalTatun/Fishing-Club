package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fishing.data.local.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow

import java.util.UUID

@Dao
interface MarkerDao {
    /**
     * Markers visible to [viewerId]: every public report plus the viewer's own
     * reports. Mirrors the server rule (`is_public = true OR user_id = viewer`)
     * so a stale cache can never expose another user's private markers.
     * There is intentionally no "all markers" query.
     */
    @Query("SELECT * FROM markers WHERE isPublic = 1 OR userId = :viewerId")
    fun getVisibleForUser(viewerId: UUID): Flow<List<MarkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markers: List<MarkerEntity>)

    @Query("UPDATE markers SET isPublic = :isPublic WHERE id = :id")
    suspend fun updateVisibility(id: UUID, isPublic: Boolean)

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM markers")
    suspend fun deleteAll()
}
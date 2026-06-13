package com.example.fishing.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface FishingDao {

    @Transaction
    @Query("SELECT * FROM fishing WHERE is_deleted = 0 ORDER BY fishing_time DESC")
    fun getAllReports(): Flow<List<FishingReportWithDetails>>

    @Transaction
    @Query("SELECT * FROM fishing WHERE id = :id AND is_deleted = 0")
    fun getReportById(id: UUID): Flow<FishingReportWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFishing(fishing: FishingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFish(fish: List<FishEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaits(baits: List<BaitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Transaction
    suspend fun insertReportWithDetails(
        fishing: FishingEntity,
        fish: List<FishEntity>,
        baits: List<BaitEntity>,
        photos: List<PhotoEntity>
    ) {
        insertFishing(fishing)
        insertFish(fish)
        insertBaits(baits)
        insertPhotos(photos)
    }

    @Query("UPDATE fishing SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteReport(id: UUID)

    // Profiles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: UUID): ProfileEntity?
}

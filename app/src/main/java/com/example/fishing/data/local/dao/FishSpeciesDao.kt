package com.example.fishing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fishing.data.local.entity.FishSpeciesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FishSpeciesDao {

    @Query("SELECT * FROM fish_species ORDER BY sortOrder, canonicalName")
    fun observeAll(): Flow<List<FishSpeciesEntity>>

    @Query("SELECT * FROM fish_species WHERE isPopular = 1 ORDER BY sortOrder, canonicalName")
    fun observePopular(): Flow<List<FishSpeciesEntity>>

    @Query("SELECT COUNT(*) FROM fish_species")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(species: List<FishSpeciesEntity>)

    @Query("DELETE FROM fish_species")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(species: List<FishSpeciesEntity>) {
        deleteAll()
        upsertAll(species)
    }
}

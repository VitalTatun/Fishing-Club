package com.example.fishing.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        FishingEntity::class,
        FishEntity::class,
        BaitEntity::class,
        PhotoEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FishingDatabase : RoomDatabase() {
    abstract fun fishingDao(): FishingDao
}

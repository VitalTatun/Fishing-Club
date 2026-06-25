package com.example.fishing.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "fishing",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FishingEntity(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(name = "user_id")
    val userId: UUID,
    @ColumnInfo(name = "published_at")
    val publishedAt: Date?,
    val type: String,
    val name: String,
    @ColumnInfo(name = "water_name")
    val waterName: String?,
    @ColumnInfo(name = "water_lat")
    val waterLat: Double?,
    @ColumnInfo(name = "water_lng")
    val waterLng: Double?,
    @ColumnInfo(name = "water_paid")
    val waterPaid: Boolean = false,
    @ColumnInfo(name = "spot_lat")
    val spotLat: Double?,
    @ColumnInfo(name = "spot_lng")
    val spotLng: Double?,
    @ColumnInfo(name = "fishing_time")
    val fishingTime: Date,
    @ColumnInfo(name = "fishing_method")
    val fishingMethod: String?,
    val comment: String?,
    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,
    val weight: Double = 0.0,
    val shore: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date(),

    // Metadata for sync/cache
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)

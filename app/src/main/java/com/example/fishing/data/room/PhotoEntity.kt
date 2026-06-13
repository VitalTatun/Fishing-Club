package com.example.fishing.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "fishing_photos",
    foreignKeys = [
        ForeignKey(
            entity = FishingEntity::class,
            parentColumns = ["id"],
            childColumns = ["fishing_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "fishing_id")
    val fishingId: UUID,
    @ColumnInfo(name = "storage_path")
    val storagePath: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)

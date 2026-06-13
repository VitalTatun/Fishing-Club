package com.example.fishing.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "fishing_fish",
    foreignKeys = [
        ForeignKey(
            entity = FishingEntity::class,
            parentColumns = ["id"],
            childColumns = ["fishing_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FishEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "fishing_id")
    val fishingId: UUID,
    val name: String,
    val count: Int = 0
)

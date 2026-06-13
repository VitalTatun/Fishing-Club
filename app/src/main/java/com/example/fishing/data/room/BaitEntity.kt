package com.example.fishing.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "fishing_baits",
    primaryKeys = ["fishing_id", "bait_code"],
    foreignKeys = [
        ForeignKey(
            entity = FishingEntity::class,
            parentColumns = ["id"],
            childColumns = ["fishing_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BaitEntity(
    @ColumnInfo(name = "fishing_id")
    val fishingId: UUID,
    @ColumnInfo(name = "bait_code")
    val baitCode: String
)

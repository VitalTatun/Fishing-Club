package com.example.fishing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "markers")
data class MarkerEntity(
    @PrimaryKey
    val id: UUID,
    /**
     * Author of the report. The map cache is shared by all accounts on the
     * device, so visibility is decided per row: public markers are visible to
     * everyone, non-public ones only to [userId]. Never store the viewer here —
     * that would hide public markers of other users.
     */
    val userId: UUID,
    val name: String,
    val waterName: String,
    val waterLat: Double,
    val waterLng: Double,
    val type: String,
    val fishingMethod: String,
    val fishingStartAt: String?,
    val isPublic: Boolean,
    val isPaidWater: Boolean = false,
    val fishNames: List<String> = emptyList()
)

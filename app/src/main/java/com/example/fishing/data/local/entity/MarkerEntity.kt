package com.example.fishing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "markers")
data class MarkerEntity(
    @PrimaryKey
    val id: UUID,
    val name: String,
    val waterName: String,
    val waterLat: Double,
    val waterLng: Double,
    val type: String,
    val fishingMethod: String,
    val fishingTime: String,
    val isPublic: Boolean
)
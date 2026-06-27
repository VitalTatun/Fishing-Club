package com.example.fishing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "report_details")
data class ReportDetailsEntity(
    @PrimaryKey
    val id: UUID,
    val userId: UUID,
    val publishedAt: String?,
    val type: String,
    val name: String,
    val waterName: String?,
    val waterLat: Double?,
    val waterLng: Double?,
    val waterPaid: Boolean,
    val spotLat: Double?,
    val spotLng: Double?,
    val fishingTime: String,
    val weight: Double,
    val fishingMethod: String?,
    val comment: String?,
    val shore: Boolean,
    val isPublic: Boolean,
    val imageUrls: List<String>,
    val fishJson: String,
    val baitsJson: String
)
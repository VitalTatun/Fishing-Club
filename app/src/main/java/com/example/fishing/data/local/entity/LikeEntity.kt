package com.example.fishing.data.local.entity

import androidx.room.Entity
import java.util.UUID

/** A user's like of a report. Count mirror lives in [ReportDetailsEntity.likesCount]. */
@Entity(tableName = "likes", primaryKeys = ["userId", "reportId"])
data class LikeEntity(
    val userId: UUID,
    val reportId: UUID,
)

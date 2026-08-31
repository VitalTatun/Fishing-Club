package com.example.fishing.data.local.entity

import androidx.room.Entity
import java.util.UUID

/** A user's bookmark of a report. Report data itself is stored only in report_details. */
@Entity(tableName = "favorites", primaryKeys = ["userId", "reportId"])
data class FavoriteReportEntity(
    val userId: UUID,
    val reportId: UUID,
)

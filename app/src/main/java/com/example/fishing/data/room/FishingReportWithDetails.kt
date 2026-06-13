package com.example.fishing.data.room

import androidx.room.Embedded
import androidx.room.Relation

data class FishingReportWithDetails(
    @Embedded
    val fishing: FishingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "fishing_id"
    )
    val fish: List<FishEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "fishing_id"
    )
    val baits: List<BaitEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "fishing_id"
    )
    val photos: List<PhotoEntity>
)

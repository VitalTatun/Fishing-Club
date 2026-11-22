package com.example.fishing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of the server fish catalog. Mirrors `public.fish_species`;
 * refreshed by [com.example.fishing.data.FishCatalogRepository] and seeded
 * from the bundled asset on the first run.
 */
@Entity(tableName = "fish_species")
data class FishSpeciesEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val aliases: List<String>,
    val isPopular: Boolean,
    val sortOrder: Int
)

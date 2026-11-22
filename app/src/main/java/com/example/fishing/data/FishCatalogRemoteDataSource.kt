package com.example.fishing.data

import com.example.fishing.data.supabase.FishSpeciesDto
import com.example.fishing.model.FishSpecies
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/** Read side of the public `fish_species` catalog. */
interface FishCatalogRemoteDataSource {
    suspend fun fetchAll(): List<FishSpecies>
}

@Singleton
class SupabaseFishCatalogRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient
) : FishCatalogRemoteDataSource {

    override suspend fun fetchAll(): List<FishSpecies> =
        supabase.postgrest[TABLE_FISH_SPECIES]
            .select { order("sort_order", Order.ASCENDING) }
            .decodeList<FishSpeciesDto>()
            .map(FishSpeciesDto::toDomain)

    private companion object {
        const val TABLE_FISH_SPECIES = "fish_species"
    }
}

internal fun FishSpeciesDto.toDomain(): FishSpecies = FishSpecies(
    id = id,
    canonicalName = canonicalName,
    aliases = aliases,
    isPopular = isPopular,
    sortOrder = sortOrder
)

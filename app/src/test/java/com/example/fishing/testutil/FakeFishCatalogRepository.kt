package com.example.fishing.testutil

import com.example.fishing.data.FishCatalogRepository
import com.example.fishing.model.FishSpecies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFishCatalogRepository(
    initial: List<FishSpecies> = emptyList()
) : FishCatalogRepository {

    private val species = MutableStateFlow(initial)

    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls: Int = 0
        private set

    fun setSpecies(value: List<FishSpecies>) {
        species.value = value
    }

    override fun observeAll(): Flow<List<FishSpecies>> = species

    override fun observePopular(): Flow<List<FishSpecies>> =
        species.map { list -> list.filter { it.isPopular }.sortedBy { it.sortOrder } }

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        return refreshResult
    }
}

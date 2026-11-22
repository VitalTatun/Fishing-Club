package com.example.fishing.data

import android.content.Context
import com.example.fishing.data.local.dao.FishSpeciesDao
import com.example.fishing.data.local.entity.FishSpeciesEntity
import com.example.fishing.model.FishSpecies
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Offline-first access to the fish catalog.
 *
 * Supabase is the source of truth, Room is the cache and the bundled asset is
 * the seed for the first launch (and for users who have never been online).
 */
interface FishCatalogRepository {
    fun observeAll(): Flow<List<FishSpecies>>
    fun observePopular(): Flow<List<FishSpecies>>
    suspend fun refresh(): Result<Unit>
}

@Singleton
class DefaultFishCatalogRepository @Inject constructor(
    private val dao: FishSpeciesDao,
    private val remoteDataSource: FishCatalogRemoteDataSource,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FishCatalogRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val seedMutex = Mutex()

    @Volatile
    private var seeded = false

    override fun observeAll(): Flow<List<FishSpecies>> = observe(dao::observeAll)

    override fun observePopular(): Flow<List<FishSpecies>> = observe(dao::observePopular)

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val remote = remoteDataSource.fetchAll()
            if (remote.isNotEmpty()) {
                dao.replaceAll(remote.map(FishSpecies::toEntity))
                seeded = true
            }
        }
    }

    private fun observe(source: () -> Flow<List<FishSpeciesEntity>>) = flow {
        ensureSeeded()
        emitAll(source().map { entities -> entities.map(FishSpeciesEntity::toDomain) })
    }

    private suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            withContext(ioDispatcher) {
                if (dao.count() == 0) {
                    val seed = readSeed()
                    if (seed.isNotEmpty()) dao.upsertAll(seed.map(FishSpecies::toEntity))
                }
                seeded = true
            }
        }
    }

    private fun readSeed(): List<FishSpecies> = runCatching {
        context.assets.open(SEED_ASSET)
            .bufferedReader()
            .use { it.readText() }
            .let(::parseFishSpeciesSeed)
    }.getOrDefault(emptyList())

    private companion object {
        const val SEED_ASSET = "fish_species.json"
    }
}

internal fun parseFishSpeciesSeed(raw: String): List<FishSpecies> {
    val seed = Json { ignoreUnknownKeys = true }.decodeFromString<FishSpeciesSeed>(raw)
    return seed.species.map(FishSpeciesSeedEntry::toDomain)
}

@Serializable
private data class FishSpeciesSeed(
    val version: Int = 1,
    val species: List<FishSpeciesSeedEntry> = emptyList()
)

@Serializable
private data class FishSpeciesSeedEntry(
    val id: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val isPopular: Boolean = false,
    val sortOrder: Int = 0
)

private fun FishSpeciesSeedEntry.toDomain(): FishSpecies = FishSpecies(
    id = id,
    canonicalName = canonicalName,
    aliases = aliases,
    isPopular = isPopular,
    sortOrder = sortOrder
)

private fun FishSpeciesEntity.toDomain(): FishSpecies = FishSpecies(
    id = id,
    canonicalName = canonicalName,
    aliases = aliases,
    isPopular = isPopular,
    sortOrder = sortOrder
)

private fun FishSpecies.toEntity(): FishSpeciesEntity = FishSpeciesEntity(
    id = id,
    canonicalName = canonicalName,
    aliases = aliases,
    isPopular = isPopular,
    sortOrder = sortOrder
)

package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.model.MarkerDomain
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Map markers are a projection of the reports cache: Room feeds the map, the
 * network refresh replaces the whole snapshot.
 */
interface MarkerStore {
    fun getMapMarkers(): Flow<List<MarkerDomain>>

    suspend fun refreshMapMarkers(): Result<List<MarkerDomain>>
}

@Singleton
class RoomMarkerStore @Inject constructor(
    private val reportRemoteQueries: ReportRemoteQueries,
    private val markerDao: MarkerDao,
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) : MarkerStore {

    override fun getMapMarkers(): Flow<List<MarkerDomain>> =
        markerDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        val userId = authRepository.currentUser()?.id
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return try {
            val entities = reportRemoteQueries.fetchMapMarkers(userId)
            // Cache is replaced atomically so a mid-refresh failure cannot wipe
            // previously cached markers out from under the user.
            database.withTransaction {
                markerDao.deleteAll()
                markerDao.insertAll(entities)
            }
            Result.success(entities.map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

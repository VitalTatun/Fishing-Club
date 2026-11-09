package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.model.AuthState
import com.example.fishing.model.MarkerDomain
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    /**
     * Follows the signed-in user: the cache holds reports of several authors
     * (all public ones plus the viewer's private ones), so the visible subset
     * is resolved per viewer. Re-subscribing on a user change makes another
     * account's private markers disappear immediately, before any refresh.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMapMarkers(): Flow<List<MarkerDomain>> =
        authRepository.authState
            .map { (it as? AuthState.Authenticated)?.user?.id }
            .distinctUntilChanged()
            .flatMapLatest { viewerId ->
                if (viewerId == null) {
                    flowOf(emptyList())
                } else {
                    markerDao.getVisibleForUser(viewerId).map { entities ->
                        entities.map { it.toDomain() }
                    }
                }
            }

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

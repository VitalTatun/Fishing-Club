package com.example.fishing.data

import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.ReportLikeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.util.UUID

interface FishingRepository {
    fun getHomeReports(userId: UUID): Flow<List<FishingReport>>
    fun getFavoriteReports(userId: UUID): Flow<List<FishingReport>>
    fun getMapMarkers(): Flow<List<MarkerDomain>>
    fun getReportDetails(id: UUID): Flow<FishingReport?>
    suspend fun refreshHomeReports(userId: UUID)
    suspend fun refreshMapMarkers(): Result<List<MarkerDomain>>
    suspend fun addFavorite(report: FishingReport): Result<Unit>
    suspend fun removeFavorite(reportId: UUID): Result<Unit>
    suspend fun saveReport(report: FishingReport): Result<Unit>
    suspend fun deleteReport(id: UUID): Result<Unit>
    suspend fun getPhotoSignedUrl(storagePath: String): String?
    fun isStoragePath(path: String): Boolean
    suspend fun addLike(reportId: UUID): Result<Unit>
    suspend fun removeLike(reportId: UUID): Result<Unit>
    fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>>
    suspend fun refreshLikes(userId: UUID)
}

class MockFishingRepository : FishingRepository {
    private val favoriteReports = mutableListOf<FishingReport>()
    private val reports = MockData.sampleReports.toMutableList()
    private val _likeStates = MutableStateFlow<Map<UUID, ReportLikeState>>(emptyMap())

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> = flow {
        delay(1000)
        emit((reports + favoriteReports).distinctBy { it.id })
    }

    override fun getFavoriteReports(userId: UUID): Flow<List<FishingReport>> = flow {
        delay(1000)
        emit(favoriteReports.toList())
    }

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = flow {
        delay(1000)
        emit(reports.map { it.toMarkerDomain() })
    }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = flow {
        delay(1000)
        emit(reports.firstOrNull { it.id == id })
    }

    override suspend fun saveReport(report: FishingReport): Result<Unit> {
        reports.add(0, report)
        return Result.success(Unit)
    }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        reports.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun refreshHomeReports(userId: UUID) {
        // Mock — no-op
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        return Result.success(reports.map { it.toMarkerDomain() })
    }

    override suspend fun addFavorite(report: FishingReport): Result<Unit> {
        if (favoriteReports.none { it.id == report.id }) {
            favoriteReports.add(report)
        }
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> {
        favoriteReports.removeAll { it.id == reportId }
        return Result.success(Unit)
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? {
        return storagePath
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
    }

    override suspend fun addLike(reportId: UUID): Result<Unit> {
        _likeStates.update { current ->
            val prev = current[reportId]
            current + (reportId to ReportLikeState(reportId, true, (prev?.likesCount ?: 0) + 1))
        }
        return Result.success(Unit)
    }

    override suspend fun removeLike(reportId: UUID): Result<Unit> {
        _likeStates.update { current ->
            val prev = current[reportId]
            current + (reportId to ReportLikeState(reportId, false, ((prev?.likesCount ?: 1) - 1).coerceAtLeast(0)))
        }
        return Result.success(Unit)
    }

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> = _likeStates.asStateFlow()

    override suspend fun refreshLikes(userId: UUID) {
        // Mock — no-op
    }

    private fun FishingReport.toMarkerDomain(): MarkerDomain {
        return MarkerDomain(
            id = id,
            name = name,
            waterName = water.waterName,
            waterLat = water.latitude,
            waterLng = water.longitude,
            type = type,
            fishingMethod = fishingMethod,
            fishingStartAt = fishingStartAt,
            isPublic = isPublic,
            isPaidWater = water.isPaid,
            fishNames = fish.map { it.name }
        )
    }
}

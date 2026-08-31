package com.example.fishing.data

import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

interface FishingRepository {
    fun getHomeReports(userId: UUID): Flow<List<FishingReport>>
    fun getFavoriteReports(userId: UUID): Flow<List<FishingReport>>
    fun getMapMarkers(): Flow<List<MarkerDomain>>
    fun getReportDetails(id: UUID): Flow<FishingReport?>
    suspend fun refreshHomeReports(userId: UUID)
    suspend fun addFavorite(report: FishingReport)
    suspend fun removeFavorite(reportId: UUID)
    suspend fun saveReport(report: FishingReport)
    suspend fun deleteReport(id: UUID)
    suspend fun getPhotoSignedUrl(storagePath: String): String?
    fun isStoragePath(path: String): Boolean
}

class MockFishingRepository : FishingRepository {
    private val favoriteReports = mutableListOf<FishingReport>()
    private val reports = MockData.sampleReports.toMutableList()

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

    override suspend fun saveReport(report: FishingReport) {
        reports.add(0, report)
    }

    override suspend fun deleteReport(id: UUID) {
        reports.removeAll { it.id == id }
    }

    override suspend fun refreshHomeReports(userId: UUID) {
        // Mock — no-op
    }

    override suspend fun addFavorite(report: FishingReport) {
        if (favoriteReports.none { it.id == report.id }) {
            favoriteReports.add(report)
        }
    }

    override suspend fun removeFavorite(reportId: UUID) {
        favoriteReports.removeAll { it.id == reportId }
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? {
        return storagePath
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
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
            fishingTime = fishingTime,
            isPublic = isPublic,
            isPaidWater = water.isPaid,
            fishNames = fish.map { it.name }
        )
    }
}

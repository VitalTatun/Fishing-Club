package com.example.fishing.data

import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

interface FishingRepository {
    fun getAllReports(userId: UUID? = null): Flow<List<FishingReport>>
    fun getMapMarkers(): Flow<List<MarkerDomain>>
    fun getReportDetails(id: UUID): Flow<FishingReport?>
    suspend fun saveReport(report: FishingReport)
    suspend fun deleteReport(id: UUID)
    suspend fun getPhotoSignedUrl(storagePath: String): String?
    fun isStoragePath(path: String): Boolean
}

class MockFishingRepository : FishingRepository {
    override fun getAllReports(userId: UUID?): Flow<List<FishingReport>> = flow {
        delay(1000)
        emit(MockData.sampleReports)
    }

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = flow {
        delay(1000)
        emit(MockData.sampleReports.map { it.toMarkerDomain() })
    }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = flow {
        delay(1000)
        emit(MockData.sampleReports.firstOrNull { it.id == id })
    }

    override suspend fun saveReport(report: FishingReport) {
        // Mock save
    }

    override suspend fun deleteReport(id: UUID) {
        // Mock delete
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
            isPublic = isPublic
        )
    }
}

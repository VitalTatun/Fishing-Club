package com.example.fishing.data

import com.example.fishing.model.FishingReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

interface FishingRepository {
    fun getAllReports(userId: UUID? = null): Flow<List<FishingReport>>
    suspend fun saveReport(report: FishingReport)
    suspend fun deleteReport(id: UUID)
}

class MockFishingRepository : FishingRepository {
    override fun getAllReports(userId: UUID?): Flow<List<FishingReport>> = flow {
        // Имитируем задержку сети
        delay(1000)
        emit(MockData.sampleReports)
    }

    override suspend fun saveReport(report: FishingReport) {
        // Mock save
    }

    override suspend fun deleteReport(id: UUID) {
        // Mock delete
    }
}

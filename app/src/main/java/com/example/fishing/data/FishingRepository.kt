package com.example.fishing.data

import com.example.fishing.model.FishingReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface FishingRepository {
    fun getAllReports(): Flow<List<FishingReport>>
}

class MockFishingRepository : FishingRepository {
    override fun getAllReports(): Flow<List<FishingReport>> = flow {
        // Имитируем задержку сети
        delay(1000)
        emit(MockData.sampleReports)
    }
}

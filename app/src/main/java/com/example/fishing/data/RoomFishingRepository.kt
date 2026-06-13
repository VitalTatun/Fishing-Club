package com.example.fishing.data

import com.example.fishing.data.room.*
import com.example.fishing.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

class RoomFishingRepository @Inject constructor(
    private val fishingDao: FishingDao
) : FishingRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            if (fishingDao.getProfileById(LOCAL_USER_ID) == null) {
                seedDatabase()
            }
        }
    }

    // Временная константа для имитации текущего пользователя
    companion object {
        val LOCAL_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val LOCAL_USER = User(
            id = LOCAL_USER_ID,
            name = "Local User",
            email = "local@example.com",
            image = ""
        )
    }

    override fun getAllReports(): Flow<List<FishingReport>> {
        return fishingDao.getAllReports().map { entities ->
            entities.map { it.toDomain(LOCAL_USER) }
        }
    }

    private suspend fun seedDatabase() {
        fishingDao.insertProfile(LOCAL_USER.toEntity())
        MockData.sampleReports.forEach { saveReport(it) }
    }

    override suspend fun saveReport(report: FishingReport) {
        val fishingEntity = report.toEntity().copy(userId = LOCAL_USER_ID)
        val fishEntities = report.fish.map { it.toEntity(report.id) }
        val baitEntities = report.bait.map { BaitEntity(report.id, it.name) }
        val photoEntities = report.photo.mapIndexed { index, path ->
            PhotoEntity(
                fishingId = report.id,
                storagePath = path,
                sortOrder = index
            )
        }

        fishingDao.insertReportWithDetails(
            fishingEntity,
            fishEntities,
            baitEntities,
            photoEntities
        )
    }

    override suspend fun deleteReport(id: UUID) {
        fishingDao.softDeleteReport(id)
    }
}

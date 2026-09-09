package com.example.fishing.testutil

import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeFishingRepository : FishingRepository {

    private val _homeReports = MutableStateFlow<List<FishingReport>>(emptyList())
    private val _favoriteReports = MutableStateFlow<List<FishingReport>>(emptyList())
    private val _mapMarkers = MutableStateFlow<List<MarkerDomain>>(emptyList())
    private val _reportDetails = MutableStateFlow<FishingReport?>(null)

    var homeReportsValue: List<FishingReport>
        get() = _homeReports.value
        set(value) { _homeReports.value = value }

    var favoriteReportsValue: List<FishingReport>
        get() = _favoriteReports.value
        set(value) { _favoriteReports.value = value }

    var mapMarkersValue: List<MarkerDomain>
        get() = _mapMarkers.value
        set(value) { _mapMarkers.value = value }

    var reportDetailsValue: FishingReport?
        get() = _reportDetails.value
        set(value) { _reportDetails.value = value }

    var refreshHomeReportsCallCount = 0
    var refreshHomeReportsException: Exception? = null
    var refreshHomeReportsGate: CompletableDeferred<Unit>? = null

    var refreshMapMarkersCallCount = 0
    var refreshMapMarkersException: Exception? = null
    var refreshMapMarkersGate: CompletableDeferred<Unit>? = null
    var refreshMapMarkersResult: List<MarkerDomain>? = null

    var addFavoriteCallCount = 0
    var removeFavoriteCallCount = 0
    var deleteReportCallCount = 0
    var deleteReportException: Exception? = null
    var deleteReportGate: CompletableDeferred<Unit>? = null

    var saveReportCallCount = 0
    var saveReportException: Exception? = null
    val savedReports = mutableListOf<FishingReport>()

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> = _homeReports.asStateFlow()

    override fun getFavoriteReports(userId: UUID): Flow<List<FishingReport>> = _favoriteReports.asStateFlow()

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = _mapMarkers.asStateFlow()

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = _reportDetails.asStateFlow()

    override suspend fun refreshHomeReports(userId: UUID) {
        refreshHomeReportsCallCount++
        refreshHomeReportsGate?.await()
        refreshHomeReportsException?.let { throw it }
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        refreshMapMarkersCallCount++
        refreshMapMarkersGate?.await()
        refreshMapMarkersException?.let { return Result.failure(it) }
        return Result.success(refreshMapMarkersResult ?: mapMarkersValue)
    }

    override suspend fun addFavorite(report: FishingReport) {
        addFavoriteCallCount++
        _favoriteReports.value = (_favoriteReports.value + report).distinctBy { it.id }
    }

    override suspend fun removeFavorite(reportId: UUID) {
        removeFavoriteCallCount++
        _favoriteReports.value = _favoriteReports.value.filterNot { it.id == reportId }
    }

    override suspend fun saveReport(report: FishingReport): Result<Unit> {
        saveReportCallCount++
        saveReportException?.let { return Result.failure(it) }
        savedReports.add(report)
        _homeReports.value = listOf(report) + _homeReports.value
        return Result.success(Unit)
    }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        deleteReportCallCount++
        deleteReportGate?.await()
        deleteReportException?.let { return Result.failure(it) }
        _homeReports.value = _homeReports.value.filterNot { it.id == id }
        _favoriteReports.value = _favoriteReports.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? = storagePath

    override fun isStoragePath(path: String): Boolean = false
}

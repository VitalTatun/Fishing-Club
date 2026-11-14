package com.example.fishing.testutil

import com.example.fishing.data.FishingRepository
import com.example.fishing.data.HomeRefreshResult
import com.example.fishing.data.report.UserProfileData
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.ReportLikeState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeFishingRepository : FishingRepository {

    private val _homeReports = MutableStateFlow<List<FishingReport>>(emptyList())
    private val _favoriteReports = MutableStateFlow<List<FishingReport>>(emptyList())
    private val _favoriteIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _mapMarkers = MutableStateFlow<List<MarkerDomain>>(emptyList())
    private val _reportDetails = MutableStateFlow<FishingReport?>(null)
    private val _likeStates = MutableStateFlow<Map<UUID, ReportLikeState>>(emptyMap())

    var homeReportsValue: List<FishingReport>
        get() = _homeReports.value
        set(value) { _homeReports.value = value }

    var favoriteReportsValue: List<FishingReport>
        get() = _favoriteReports.value
        set(value) {
            _favoriteReports.value = value
            _favoriteIds.value = value.map { it.id }.toSet()
        }

    var mapMarkersValue: List<MarkerDomain>
        get() = _mapMarkers.value
        set(value) { _mapMarkers.value = value }

    var reportDetailsValue: FishingReport?
        get() = _reportDetails.value
        set(value) { _reportDetails.value = value }

    var likeStatesValue: Map<UUID, ReportLikeState>
        get() = _likeStates.value
        set(value) { _likeStates.value = value }

    var refreshHomeReportsCallCount = 0
    var refreshHomeReportsException: Exception? = null
    var refreshHomeReportsGate: CompletableDeferred<Unit>? = null
    var refreshHomeReportsHasMore = false
    var refreshHomeReportsLoadedCount = 0

    var loadMoreHomeReportsCallCount = 0
    var loadMoreHomeReportsException: Exception? = null
    var loadMoreHomeReportsGate: CompletableDeferred<Unit>? = null
    var loadMoreHomeReportsHasMore = false

    var refreshMapMarkersCallCount = 0
    var refreshMapMarkersException: Exception? = null
    var refreshMapMarkersGate: CompletableDeferred<Unit>? = null
    var refreshMapMarkersResult: List<MarkerDomain>? = null

    var addFavoriteCallCount = 0
    var addFavoriteException: Exception? = null
    var addFavoriteGate: CompletableDeferred<Unit>? = null

    var removeFavoriteCallCount = 0
    var removeFavoriteException: Exception? = null
    var removeFavoriteGate: CompletableDeferred<Unit>? = null

    var loadFavoriteReportsCallCount = 0
    var loadFavoriteReportsException: Exception? = null

    var refreshFavoriteIdsCallCount = 0
    var refreshFavoriteIdsException: Exception? = null

    var deleteReportCallCount = 0
    var deleteReportException: Exception? = null
    var deleteReportGate: CompletableDeferred<Unit>? = null

    var updateReportVisibilityCallCount = 0
    var updateReportVisibilityException: Exception? = null
    var updateReportVisibilityGate: CompletableDeferred<Unit>? = null
    var lastVisibilityReportId: UUID? = null
    var lastVisibilityIsPublic: Boolean? = null

    var addLikeCallCount = 0
    var addLikeException: Exception? = null
    var addLikeGate: CompletableDeferred<Unit>? = null

    var removeLikeCallCount = 0
    var removeLikeException: Exception? = null
    var removeLikeGate: CompletableDeferred<Unit>? = null

    var refreshLikesCallCount = 0
    var refreshLikesException: Exception? = null

    var saveReportCallCount = 0
    var saveReportException: Exception? = null
    /** Simulates a server that committed the report before the timeout. */
    var saveReportPersistsBeforeFailure = false
    /** Simulates a successful call that the server did not actually persist. */
    var saveReportReturnsSuccessWithoutPersistence = false
    /** Server-side report row, independent from its attachments. */
    var serverHasReport = false
    /** Server-side photo objects/rows for the report above. */
    var serverHasAllPhotos = false
    var isReportSyncedCallCount = 0
    val savedReports = mutableListOf<FishingReport>()
    val lastSavedReportId: UUID? get() = savedReports.lastOrNull()?.id

    var getPhotoSignedUrlsCallCount = 0
    var getPhotoSignedUrlsException: Exception? = null
    var getPhotoSignedUrlsGate: CompletableDeferred<Unit>? = null
    var signedUrlResults: Map<String, String> = emptyMap()
    val getPhotoSignedUrlsRequests = mutableListOf<List<String>>()

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> = _homeReports.asStateFlow()

    var loadUserProfileCallCount = 0
    var userProfileResult: Result<UserProfileData>? = null
    var loadUserProfileGate: CompletableDeferred<Unit>? = null

    override suspend fun loadUserProfile(userId: UUID): Result<UserProfileData> {
        loadUserProfileCallCount++
        loadUserProfileGate?.await()
        return userProfileResult
            ?: Result.success(UserProfileData(user = null, reports = emptyList()))
    }

    override suspend fun loadFavoriteReports(userId: UUID): List<FishingReport> {
        loadFavoriteReportsCallCount++
        loadFavoriteReportsException?.let { throw it }
        _favoriteIds.value = _favoriteReports.value.map { it.id }.toSet()
        return _favoriteReports.value
    }

    override fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>> = _favoriteIds.asStateFlow()

    override suspend fun refreshFavoriteIds(userId: UUID) {
        refreshFavoriteIdsCallCount++
        refreshFavoriteIdsException?.let { throw it }
        _favoriteIds.value = _favoriteReports.value.map { it.id }.toSet()
    }

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = _mapMarkers.asStateFlow()

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = _reportDetails.asStateFlow()

    override suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult {
        refreshHomeReportsCallCount++
        refreshHomeReportsGate?.await()
        refreshHomeReportsException?.let { throw it }
        return HomeRefreshResult(
            hasMore = refreshHomeReportsHasMore,
            loadedCount = refreshHomeReportsLoadedCount
        )
    }

    override suspend fun loadMoreHomeReports(userId: UUID): Boolean {
        loadMoreHomeReportsCallCount++
        loadMoreHomeReportsGate?.await()
        loadMoreHomeReportsException?.let { throw it }
        return loadMoreHomeReportsHasMore
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        refreshMapMarkersCallCount++
        refreshMapMarkersGate?.await()
        refreshMapMarkersException?.let { return Result.failure(it) }
        return Result.success(refreshMapMarkersResult ?: mapMarkersValue)
    }

    override suspend fun addFavorite(report: FishingReport): Result<Unit> {
        addFavoriteCallCount++
        addFavoriteGate?.await()
        addFavoriteException?.let { return Result.failure(it) }
        _favoriteReports.value = (_favoriteReports.value + report).distinctBy { it.id }
        _favoriteIds.value = _favoriteReports.value.map { it.id }.toSet()
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> {
        removeFavoriteCallCount++
        removeFavoriteGate?.await()
        removeFavoriteException?.let { return Result.failure(it) }
        _favoriteReports.value = _favoriteReports.value.filterNot { it.id == reportId }
        _favoriteIds.value = _favoriteReports.value.map { it.id }.toSet()
        return Result.success(Unit)
    }

    override suspend fun saveReport(
        report: FishingReport,
        onPhotoUploaded: (suspend (uploaded: Int, total: Int) -> Unit)?
    ): Result<Unit> {
        saveReportCallCount++
        savedReports.add(report)
        saveReportException?.let { failure ->
            if (saveReportPersistsBeforeFailure) {
                serverHasReport = true
                serverHasAllPhotos = true
            }
            return Result.failure(failure)
        }
        onPhotoUploaded?.invoke(report.photos.size, report.photos.size)
        _homeReports.value = listOf(report) + _homeReports.value
        if (!saveReportReturnsSuccessWithoutPersistence) {
            serverHasReport = true
            serverHasAllPhotos = true
        }
        return Result.success(Unit)
    }

    override suspend fun isReportSynced(report: FishingReport): Boolean {
        isReportSyncedCallCount++
        return serverHasReport && serverHasAllPhotos
    }

    override suspend fun deleteReport(id: UUID): Result<Unit> {
        deleteReportCallCount++
        deleteReportGate?.await()
        deleteReportException?.let { return Result.failure(it) }
        _homeReports.value = _homeReports.value.filterNot { it.id == id }
        _favoriteReports.value = _favoriteReports.value.filterNot { it.id == id }
        _favoriteIds.value = _favoriteReports.value.map { it.id }.toSet()
        return Result.success(Unit)
    }

    override suspend fun updateReportVisibility(id: UUID, isPublic: Boolean): Result<Unit> {
        updateReportVisibilityCallCount++
        lastVisibilityReportId = id
        lastVisibilityIsPublic = isPublic
        updateReportVisibilityGate?.await()
        updateReportVisibilityException?.let { return Result.failure(it) }

        _homeReports.value = _homeReports.value.map {
            if (it.id == id) it.copy(isPublic = isPublic) else it
        }
        _favoriteReports.value = _favoriteReports.value.map {
            if (it.id == id) it.copy(isPublic = isPublic) else it
        }
        _reportDetails.value = _reportDetails.value?.let {
            if (it.id == id) it.copy(isPublic = isPublic) else it
        }
        _mapMarkers.value = _mapMarkers.value.map {
            if (it.id == id) it.copy(isPublic = isPublic) else it
        }
        return Result.success(Unit)
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? = storagePath

    override suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String> {
        getPhotoSignedUrlsCallCount++
        getPhotoSignedUrlsRequests += storagePaths
        getPhotoSignedUrlsGate?.await()
        getPhotoSignedUrlsException?.let { throw it }
        return storagePaths.associateWith { path ->
            signedUrlResults[path] ?: "signed:$path"
        }
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
    }

    override suspend fun addLike(reportId: UUID): Result<Unit> {
        addLikeCallCount++
        addLikeGate?.await()
        addLikeException?.let { return Result.failure(it) }
        val prev = _likeStates.value[reportId] ?: ReportLikeState(reportId)
        if (!prev.isLiked) {
            _likeStates.value = _likeStates.value + (reportId to prev.copy(
                isLiked = true,
                likesCount = prev.likesCount + 1
            ))
        }
        return Result.success(Unit)
    }

    override suspend fun removeLike(reportId: UUID): Result<Unit> {
        removeLikeCallCount++
        removeLikeGate?.await()
        removeLikeException?.let { return Result.failure(it) }
        val prev = _likeStates.value[reportId] ?: ReportLikeState(reportId, isLiked = true, likesCount = 1)
        if (prev.isLiked) {
            _likeStates.value = _likeStates.value + (reportId to prev.copy(
                isLiked = false,
                likesCount = (prev.likesCount - 1).coerceAtLeast(0)
            ))
        }
        return Result.success(Unit)
    }

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> = _likeStates.asStateFlow()

    override suspend fun refreshLikes(userId: UUID) {
        refreshLikesCallCount++
        refreshLikesException?.let { throw it }
    }
}

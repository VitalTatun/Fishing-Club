package com.example.fishing.viewmodel.detail

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.SupabaseFishingRepository
import com.example.fishing.viewmodel.ReportDetailUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Report details scenario: cache-first rendering, authoritative existence
 * check, signed photo URLs and deletion state.
 */
class ReportDetailCoordinator(
    private val repository: FishingRepository,
    private val photoUrls: PhotoUrlResolver,
    private val scope: CoroutineScope
) {
    private val _reportDetailUiState =
        MutableStateFlow<ReportDetailUiState>(ReportDetailUiState.Loading)
    val reportDetailUiState: StateFlow<ReportDetailUiState> =
        _reportDetailUiState.asStateFlow()

    private val _isDeletingReport = MutableStateFlow(false)
    val isDeletingReport: StateFlow<Boolean> = _isDeletingReport.asStateFlow()

    private val _deleteReportError = MutableStateFlow<String?>(null)
    val deleteReportError: StateFlow<String?> = _deleteReportError.asStateFlow()

    private val _deletedReportId = MutableStateFlow<UUID?>(null)
    val deletedReportId: StateFlow<UUID?> = _deletedReportId.asStateFlow()

    private val _isUpdatingVisibility = MutableStateFlow(false)
    val isUpdatingVisibility: StateFlow<Boolean> = _isUpdatingVisibility.asStateFlow()

    private val _visibilityError = MutableStateFlow<String?>(null)
    val visibilityError: StateFlow<String?> = _visibilityError.asStateFlow()

    private val _visibilityResult = MutableStateFlow<String?>(null)
    val visibilityResult: StateFlow<String?> = _visibilityResult.asStateFlow()

    private var reportDetailsJob: Job? = null
    private var deleteReportJob: Job? = null
    private var visibilityJob: Job? = null
    private var detailPhotosVersion = 0

    fun loadReportDetails(id: UUID) {
        reportDetailsJob?.cancel()
        _deleteReportError.value = null
        // A result for a previous report must not leak into the next one if the
        // user left the screen before the snackbar finished.
        _visibilityError.value = null
        _visibilityResult.value = null
        _reportDetailUiState.value = ReportDetailUiState.Loading

        reportDetailsJob = scope.launch {
            // Observe Room cache and publish Success the moment the report is available.
            launch {
                try {
                    repository.getReportDetails(id).collect { report ->
                        if (report != null) {
                            // Render immediately with raw paths, then swap in signed
                            // URLs. Network work never blocks the Room collector.
                            _reportDetailUiState.value = ReportDetailUiState.Success(
                                photoUrls.applyCachedPhotoUrls(listOf(report)).single()
                            )
                            val version = ++detailPhotosVersion
                            launch {
                                val resolved = report.copy(
                                    photos = photoUrls.resolvePhotoUrls(report.photos)
                                )
                                if (version == detailPhotosVersion) {
                                    _reportDetailUiState.value =
                                        ReportDetailUiState.Success(resolved)
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (_reportDetailUiState.value !is ReportDetailUiState.Success) {
                        _reportDetailUiState.value = reportDetailError(id, e)
                    }
                }
            }
            // Authoritative network refresh: it knows whether the report actually exists,
            // so Empty can be decided without racing Room's async emission.
            try {
                val found = repository is SupabaseFishingRepository &&
                    repository.refreshReportDetails(id)
                if (!found && _reportDetailUiState.value == ReportDetailUiState.Loading) {
                    _reportDetailUiState.value = ReportDetailUiState.Empty(id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_reportDetailUiState.value !is ReportDetailUiState.Success) {
                    _reportDetailUiState.value = reportDetailError(id, e)
                }
            }
        }
    }

    fun deleteReport(id: UUID) {
        if (_isDeletingReport.value) return

        _isDeletingReport.value = true
        _deleteReportError.value = null
        _deletedReportId.value = null

        deleteReportJob = scope.launch {
            repository.deleteReport(id)
                .onSuccess {
                    _deletedReportId.value = id
                }
                .onFailure { e ->
                    _deleteReportError.value =
                        "Не удалось удалить отчет: ${ErrorMapper.toUserFriendlyMessage(e)}"
                }
            _isDeletingReport.value = false
        }
    }

    fun clearDeleteReportError() {
        _deleteReportError.value = null
    }

    /**
     * Flips the publish flag on the server. The room flow updates the rendered
     * report afterwards, so the menu label and the draft badge follow the
     * authoritative state instead of an optimistic local guess.
     */
    fun updateVisibility(id: UUID, isPublic: Boolean) {
        if (_isUpdatingVisibility.value) return

        _isUpdatingVisibility.value = true
        _visibilityError.value = null
        _visibilityResult.value = null

        visibilityJob = scope.launch {
            repository.updateReportVisibility(id, isPublic)
                .onSuccess {
                    _visibilityResult.value = if (isPublic) {
                        "Отчёт опубликован"
                    } else {
                        "Отчёт теперь виден только вам"
                    }
                }
                .onFailure { e ->
                    _visibilityError.value =
                        "Не удалось изменить видимость отчета: " +
                            ErrorMapper.toUserFriendlyMessage(e)
                }
            _isUpdatingVisibility.value = false
        }
    }

    fun clearVisibilityError() {
        _visibilityError.value = null
    }

    fun clearVisibilityResult() {
        _visibilityResult.value = null
    }

    fun clearDeletedReport() {
        _deletedReportId.value = null
    }

    fun clear() {
        reportDetailsJob?.cancel()
        deleteReportJob?.cancel()
        visibilityJob?.cancel()
        _reportDetailUiState.value = ReportDetailUiState.Loading
        _isDeletingReport.value = false
        _deleteReportError.value = null
        _deletedReportId.value = null
        _isUpdatingVisibility.value = false
        _visibilityError.value = null
        _visibilityResult.value = null
        detailPhotosVersion++
    }

    private fun reportDetailError(id: UUID, e: Exception): ReportDetailUiState.Error {
        return ReportDetailUiState.Error(
            reportId = id,
            message = "Ошибка загрузки отчета: ${ErrorMapper.toUserFriendlyMessage(e)}"
        )
    }
}

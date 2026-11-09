package com.example.fishing.viewmodel

import com.example.fishing.model.FishingReport
import java.util.UUID

sealed interface ReportDetailUiState {
    data object Loading : ReportDetailUiState

    data class Success(val report: FishingReport) : ReportDetailUiState

    // Report is confirmed absent in the backend, not merely not-yet-loaded.
    data class Empty(val reportId: UUID) : ReportDetailUiState

    data class Error(val reportId: UUID, val message: String) : ReportDetailUiState
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val reports: List<FishingReport>) : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

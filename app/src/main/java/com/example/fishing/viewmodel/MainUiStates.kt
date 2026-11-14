package com.example.fishing.viewmodel

import com.example.fishing.model.FishingReport
import com.example.fishing.model.User
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

/**
 * Public page of another user. [user] can be null when the profile row is gone,
 * in which case the screen falls back to the name passed by the caller.
 */
sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState

    data class Success(
        val user: User?,
        val reports: List<FishingReport>,
        val caughtCount: Int,
        val reportsCount: Int
    ) : UserProfileUiState

    data class Error(val message: String) : UserProfileUiState
}

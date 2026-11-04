package com.example.fishing.ui.screens.main

import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportUploadNotice
import com.example.fishing.viewmodel.HomeUiState
import org.osmdroid.util.GeoPoint
import java.util.UUID

data class MainUiState(
    val selectedTab: Int = 0,
    val homeUiState: HomeUiState = HomeUiState.Loading,
    val mapMarkers: List<MarkerDomain> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreReports: Boolean = false,
    val mapIsLoading: Boolean = false,
    val mapIsRefreshing: Boolean = false,
    val mapErrorMessage: String? = null,
    val highlightedPolygon: List<GeoPoint>? = null,
    val currentSortOrder: ReportSortOrder = ReportSortOrder.BY_FISHING_TIME,
    val userEmail: String? = null,
    val userName: String? = null,
    val userImage: String? = null,
    val currentUserId: UUID? = null,
    val errorText: String? = null,
    val deleteErrorText: String? = null,
    val favoriteErrorText: String? = null,
    val likeErrorText: String? = null,
    val pendingUploads: List<PendingReportUpload> = emptyList(),
    val uploadNotice: ReportUploadNotice? = null
)

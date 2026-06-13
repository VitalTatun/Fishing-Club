package com.example.fishing.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import java.util.Date
import com.example.fishing.model.*
import com.example.fishing.data.RoomFishingRepository
import java.util.UUID

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FishingRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<FishingReport>>(emptyList())
    val reports: StateFlow<List<FishingReport>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _mapRequestedLocation = MutableStateFlow<GeoPoint?>(null)
    val mapRequestedLocation: StateFlow<GeoPoint?> = _mapRequestedLocation.asStateFlow()

    // Create report form state (survives navigation — ViewModel scoped to Activity)
    var formTitle by mutableStateOf("")
    var formReportType by mutableStateOf("Отчет")
    var formWaterName by mutableStateOf("")
    var formSelectedPhotoUris by mutableStateOf<List<Uri>>(emptyList())
    var formFishingDate by mutableStateOf("")
    var formFishingStartTime by mutableStateOf("")
    var formFishingFromShore by mutableStateOf(true)
    var formIsPublic by mutableStateOf(true)
    var formIsPaidWater by mutableStateOf(false)
    var formWeight by mutableFloatStateOf(0f)
    var formSelectedMethod by mutableStateOf(FishingMethod.NONE)
    var formSelectedBaits by mutableStateOf<List<Bait>>(emptyList())
    var formSelectedFish by mutableStateOf<List<Fish>>(emptyList())
    var formComment by mutableStateOf("")
    var formLocation by mutableStateOf<GeoPoint?>(null)
    init {
        loadReports()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun requestMapLocation(point: GeoPoint?) {
        _mapRequestedLocation.value = point
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllReports().collect {
                _reports.value = it
                _isLoading.value = false
            }
        }
    }

    fun saveNewReport(
        title: String,
        type: String,
        waterName: String,
        location: GeoPoint?,
        fishingTime: Date,
        weight: Double,
        fish: List<Fish>,
        method: FishingMethod,
        baits: List<Bait>,
        comment: String,
        shore: Boolean,
        isPublic: Boolean,
        photos: List<String>
    ) {
        viewModelScope.launch {
            val report = FishingReport(
                userId = RoomFishingRepository.LOCAL_USER_ID,
                type = if (type == "Отчет") FishingType.FISHING_LOG else FishingType.HAUL,
                name = title,
                water = Water(
                    waterName = waterName,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0
                ),
                spotLat = location?.latitude,
                spotLng = location?.longitude,
                photo = photos,
                fishingTime = fishingTime,
                weight = weight,
                fish = fish,
                fishingMethod = method,
                bait = baits,
                comment = comment,
                user = RoomFishingRepository.LOCAL_USER,
                fishingFromTheShore = shore,
                isPublic = isPublic
            )
            repository.saveReport(report)
        }
    }

    fun deleteReport(id: UUID) {
        viewModelScope.launch {
            repository.deleteReport(id)
            loadReports()
        }
    }

    fun resetFormState() {
        formTitle = ""
        formReportType = "Отчет"
        formWaterName = ""
        formSelectedPhotoUris = emptyList()
        formFishingDate = ""
        formFishingStartTime = ""
        formFishingFromShore = true
        formIsPublic = true
        formIsPaidWater = false
        formWeight = 0f
        formSelectedMethod = FishingMethod.NONE
        formSelectedBaits = emptyList()
        formSelectedFish = emptyList()
        formComment = ""
        formLocation = null
    }
}

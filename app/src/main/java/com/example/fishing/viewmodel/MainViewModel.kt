package com.example.fishing.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
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
import java.util.UUID

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<FishingReport>>(emptyList())
    val reports: StateFlow<List<FishingReport>> = _reports.asStateFlow()

    private val _allReports = MutableStateFlow<List<FishingReport>>(emptyList())
    val allReports: StateFlow<List<FishingReport>> = _allReports.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _mapRequestedLocation = MutableStateFlow<GeoPoint?>(null)
    val mapRequestedLocation: StateFlow<GeoPoint?> = _mapRequestedLocation.asStateFlow()

    var mapLastCenterLat: Double? = null
    var mapLastCenterLon: Double? = null
    var mapLastZoom: Double = 6.0

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
    var formMood by mutableIntStateOf(3)
    var formComment by mutableStateOf("")
    var formLocation by mutableStateOf<GeoPoint?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        loadReports()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        if (index == 1) loadAllReports()
    }

    fun requestMapLocation(point: GeoPoint?) {
        _mapRequestedLocation.value = point
    }

    fun loadReports() {
        val currentUserId = authRepository.currentUser()?.id
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllReports(userId = currentUserId).collect {
                    _reports.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Ошибка загрузки: ${e.message ?: "неизвестная"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllReports() {
        viewModelScope.launch {
            try {
                repository.getAllReports().collect {
                    _allReports.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        isPaidWater: Boolean,
        photos: List<String>
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser()
            val report = FishingReport(
                userId = currentUser?.id ?: UUID.randomUUID(),
                type = if (type == "Отчет") FishingType.FISHING_LOG else FishingType.HAUL,
                name = title,
                water = Water(
                    waterName = waterName,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    isPaid = isPaidWater
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
                user = currentUser ?: User(name = "", email = "", image = ""),
                fishingFromTheShore = shore,
                isPublic = isPublic
            )
            repository.saveReport(report)
            loadReports()
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
        formMood = 3
        formComment = ""
        formLocation = null
    }
}

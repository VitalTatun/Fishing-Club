package com.example.fishing.viewmodel

import android.content.Context
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
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.utils.PhotoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Form state
    var formTitle by mutableStateOf("")
    var formReportType by mutableStateOf(FishingType.FISHING_LOG)
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

    val isTrophy: Boolean
        get() = formReportType == FishingType.HAUL

    val isSaveEnabled: Boolean
        get() = (formWaterName.isNotBlank() &&
                formLocation != null &&
                formSelectedMethod != FishingMethod.NONE &&
                formSelectedBaits.isNotEmpty() &&
                formSelectedFish.isNotEmpty() &&
                formFishingDate.isNotBlank()).let { baseValid ->
            if (isTrophy) {
                baseValid &&
                        formSelectedPhotoUris.isNotEmpty() &&
                        formSelectedFish.size == 1
            } else {
                baseValid
            }
        }

    val formHasData: Boolean
        get() = formWaterName.isNotBlank() ||
                formLocation != null ||
                formSelectedMethod != FishingMethod.NONE ||
                formSelectedFish.isNotEmpty() ||
                formSelectedBaits.isNotEmpty() ||
                formSelectedPhotoUris.isNotEmpty() ||
                formComment.isNotBlank() ||
                formWeight > 0f

    fun saveReport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser()
            
            // Photo processing
            val internalPhotos = formSelectedPhotoUris.mapNotNull { uri ->
                PhotoUtils.copyPhotoToInternalStorage(context.contentResolver, context.filesDir, uri)
            }

            val combinedDateTime = combineDateAndTime(
                formFishingDate,
                formFishingStartTime
            )

            val report = FishingReport(
                userId = currentUser?.id ?: UUID.randomUUID(),
                type = formReportType,
                name = formTitle,
                water = Water(
                    waterName = formWaterName,
                    latitude = formLocation?.latitude ?: 0.0,
                    longitude = formLocation?.longitude ?: 0.0,
                    isPaid = formIsPaidWater
                ),
                spotLat = formLocation?.latitude,
                spotLng = formLocation?.longitude,
                photo = internalPhotos,
                fishingTime = combinedDateTime,
                createdAt = Date(),
                weight = formWeight.toDouble(),
                fish = formSelectedFish,
                fishingMethod = formSelectedMethod,
                bait = formSelectedBaits,
                comment = formComment,
                user = currentUser ?: User(name = "", email = "", image = ""),
                fishingFromTheShore = formFishingFromShore,
                isPublic = formIsPublic
            )
            repository.saveReport(report)
            resetFormState()
            onSuccess()
        }
    }

    fun resetFormState() {
        formTitle = ""
        formReportType = FishingType.FISHING_LOG
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

    private fun combineDateAndTime(
        dateString: String,
        timeString: String
    ): Date {
        val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        val calendar = Calendar.getInstance()
        try {
            calendar.time = dateFormatter.parse(dateString) ?: Date()
        } catch (_: Exception) {
            // fallback
        }
        val timeParts = timeString.split(":")
        if (timeParts.size == 2) {
            calendar[Calendar.HOUR_OF_DAY] = timeParts[0].toInt()
            calendar[Calendar.MINUTE] = timeParts[1].toInt()
        }
        return calendar.time
    }
}

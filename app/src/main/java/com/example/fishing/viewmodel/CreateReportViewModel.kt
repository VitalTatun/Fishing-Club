package com.example.fishing.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.R
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.*
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
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    // Form state
    var formTitle by mutableStateOf("")
    var formReportType by mutableStateOf(FishingType.FISHING_LOG)
    var formWaterName by mutableStateOf("")
    var formSelectedPhotoUris by mutableStateOf<List<Uri>>(emptyList())
    var formFishingDate by mutableStateOf("")
    var formFishingStartTime by mutableStateOf("")
    var formFishingFromShore by mutableStateOf(value = true)
    var formIsPublic by mutableStateOf(true)
    var formIsPaidWater by mutableStateOf(false)
    var formWeight by mutableFloatStateOf(0f)
    var formSelectedMethod by mutableStateOf(FishingMethod.NONE)
    var formSelectedBaits by mutableStateOf<List<Bait>>(emptyList())
    var formSelectedFish by mutableStateOf<List<Fish>>(emptyList())
    var formMood by mutableIntStateOf(3)
    var formComment by mutableStateOf("")
    var formLocation by mutableStateOf<GeoPoint?>(null)

    val formConfig by derivedStateOf {
        val sections = mutableListOf<ReportFormSection>()

        // Type Section
        sections.add(
            ReportFormSection(
                id = "type",
                items = listOf(ReportField.CustomField("report_type"))
            )
        )

        // Date/Time Section
        sections.add(
            ReportFormSection(
                id = "date_time",
                items = listOf(
                    ReportField.ListItemField(
                        fieldId = "date_time",
                        title = formFishingDate,
                        leadingIcon = Icons.Default.Schedule,
                        trailingText = formFishingStartTime
                    ),
                    ReportField.ListItemField(
                        fieldId = "placeholder_date",
                        title = "Вс, 2 августа 2026",
                        trailingText = "4:00"
                    ),
                    ReportField.ToggleField(
                        fieldId = "is_public",
                        title = "Опубликовать",
                        supportingText = context.getString(R.string.publish_supporting),
                        leadingIcon = Icons.Default.PublishedWithChanges,
                        checked = formIsPublic,
                        onCheckedChange = { formIsPublic = it }
                    )
                )
            )
        )

        // Photos Section
        sections.add(
            ReportFormSection(
                id = "photos",
                items = listOf(ReportField.PhotoPicker(isRequired = isTrophy && formSelectedPhotoUris.isEmpty()))
            )
        )

        // Water Section
        val waterItems = mutableListOf<ReportField>()
        val hasLocation = formLocation != null
        waterItems.add(
            ReportField.ListItemField(
                fieldId = "water_body",
                title = context.getString(R.string.water_body),
                leadingIcon = Icons.Default.LocationOn,
                isRequired = !hasLocation
            )
        )
        if (hasLocation) {
            waterItems.add(ReportField.MapPreview)
            if (formWaterName.isEmpty()) {
                waterItems.add(
                    ReportField.ListItemField(
                        fieldId = "add_water_name",
                        title = context.getString(R.string.add_water_name_button),
                        leadingIcon = Icons.Default.Add
                    )
                )
            } else {
                waterItems.add(
                    ReportField.ListItemField(
                        fieldId = "water_name",
                        title = formWaterName,
                        supportingText = "Координаты: ${"%.5f".format(formLocation?.latitude)}, ${"%.5f".format(formLocation?.longitude)}"
                    )
                )
            }

            if (formWaterName.isNotEmpty()) {
                waterItems.add(ReportField.CustomField("water_details_header"))
                // WaterDetailsItems are handled separately or we can add them here
                // For simplicity, let's add them as custom or more fields
                waterItems.add(ReportField.CustomField("water_details"))
            }
        }
        sections.add(ReportFormSection(id = "water", items = waterItems))

        // Method Section
        val methodItems = mutableListOf<ReportField>()
        val hasMethod = formSelectedMethod != FishingMethod.NONE
        methodItems.add(
            ReportField.ListItemField(
                fieldId = "method",
                overline = if (hasMethod) context.getString(R.string.fishing_method) else null,
                title = if (hasMethod) context.getString(formSelectedMethod.labelRes) else context.getString(R.string.method_and_bait),
                leadingIcon = Icons.Default.Phishing,
                isRequired = !hasMethod
            )
        )
        if (formSelectedBaits.isNotEmpty()) {
            val baitsText = formSelectedBaits.joinToString(", ") { context.getString(it.labelRes) }
            methodItems.add(
                ReportField.ListItemField(
                    fieldId = "baits",
                    overline = context.getString(R.string.bait),
                    title = baitsText
                )
            )
        }
        sections.add(ReportFormSection(id = "method", items = methodItems))

        // Catch Section
        val catchItems = mutableListOf<ReportField>()
        val hasCatch = formSelectedFish.isNotEmpty()
        catchItems.add(ReportField.FishList(isRequired = !hasCatch))
        if (formWeight > 0f) {
            catchItems.add(
                ReportField.ListItemField(
                    fieldId = "weight",
                    overline = context.getString(R.string.total_weight),
                    title = "$formWeight ${context.getString(R.string.kg)}"
                )
            )
        }
        sections.add(ReportFormSection(id = "catch", items = catchItems))

        // Comment Section
        sections.add(
            ReportFormSection(
                id = "comment",
                items = listOf(
                    ReportField.ListItemField(
                        fieldId = "comment",
                        title = formComment.ifBlank { context.getString(R.string.comment) },
                        leadingIcon = Icons.AutoMirrored.Filled.Notes
                    )
                )
            )
        )

        sections
    }

    val isTrophy: Boolean
        get() = formReportType == FishingType.HAUL

    val isSaveEnabled: Boolean
        get() = (
            formWaterName.isNotBlank() &&
                formLocation != null &&
                formSelectedMethod != FishingMethod.NONE &&
                formSelectedBaits.isNotEmpty() &&
                formSelectedFish.isNotEmpty() &&
                formFishingDate.isNotBlank()
            ).let { baseValid ->
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

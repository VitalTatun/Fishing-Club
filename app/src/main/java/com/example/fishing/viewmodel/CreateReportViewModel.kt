package com.example.fishing.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
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
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class CreateReportSaveState {
    Idle, Saving, Success, Error
}

data class ReportPhoto(
    val id: UUID = UUID.randomUUID(),
    val uri: Uri
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private var currentReportId: UUID = UUID.randomUUID()

    // Form state
    var formReportType by mutableStateOf(FishingType.FISHING_LOG)
    var formWaterName by mutableStateOf("")
    var formPhotos by mutableStateOf<List<ReportPhoto>>(emptyList())
    var formStartDate by mutableStateOf("")
    var formStartTime by mutableStateOf("")
    var formEndDate by mutableStateOf("")
    var formEndTime by mutableStateOf("")
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

    var saveState by mutableStateOf(CreateReportSaveState.Idle)
    var saveErrorMessage by mutableStateOf<String?>(null)

    val isSaving: Boolean
        get() = saveState == CreateReportSaveState.Saving

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state == com.example.fishing.model.AuthState.Unauthenticated) {
                    resetFormState()
                    resetSaveState()
                }
            }
        }
    }

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
        val dateTimeItems = mutableListOf<ReportField>(
            ReportField.ListItemField(
                fieldId = "date_time",
                overline = context.getString(R.string.start),
                title = formStartDate.ifEmpty { context.getString(R.string.select_date) },
                leadingIcon = Icons.Default.Schedule,
                trailingText = formStartTime.ifEmpty { context.getString(R.string.select_time) }
            ),
            ReportField.ListItemField(
                fieldId = "date_time_end",
                overline = context.getString(R.string.end),
                title = formEndDate.ifEmpty { context.getString(R.string.select_date) },
                leadingIcon = Icons.Default.Schedule,
                trailingText = formEndTime.ifEmpty { context.getString(R.string.select_time) }
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
        dateTimeErrors().forEach { error ->
            dateTimeItems.add(
                ReportField.ErrorField(
                    text = context.getString(
                        when (error) {
                            FishingDateTimeError.START_MISSING -> R.string.error_start_missing
                            FishingDateTimeError.END_MISSING -> R.string.error_end_missing
                            FishingDateTimeError.END_NOT_AFTER_START -> R.string.error_end_not_after_start
                            FishingDateTimeError.START_IN_FUTURE -> R.string.error_start_in_future
                            FishingDateTimeError.END_IN_FUTURE -> R.string.error_end_in_future
                        }
                    )
                )
            )
        }
        sections.add(ReportFormSection(id = "date_time", items = dateTimeItems))

        // Photos Section
        sections.add(
            ReportFormSection(
                id = "photos",
                items = listOf(ReportField.PhotoPicker(isRequired = isTrophy && formPhotos.isEmpty()))
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
        get() {
            if (isSaving) return false
            val baseValid = formLocation != null &&
                formSelectedMethod != FishingMethod.NONE &&
                formSelectedBaits.isNotEmpty() &&
                formSelectedFish.isNotEmpty() &&
                FishingDateTimeValidator.isValid(fishingStartAt(), fishingEndAt(), Instant.now())
            return if (isTrophy) {
                baseValid &&
                    formPhotos.isNotEmpty() &&
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
                formPhotos.isNotEmpty() ||
                formComment.isNotBlank() ||
                formWeight > 0f ||
                formStartDate.isNotBlank() ||
                formStartTime.isNotBlank() ||
                formEndDate.isNotBlank() ||
                formEndTime.isNotBlank()

    fun fishingStartAt(): Instant? {
        return combineStart()
    }

    fun fishingEndAt(): Instant? {
        return combineEnd()
    }

    fun dateTimeErrors(): List<FishingDateTimeError> {
        return FishingDateTimeValidator.validate(fishingStartAt(), fishingEndAt(), Instant.now())
    }

    fun saveReport(onSuccess: () -> Unit) {
        if (saveState == CreateReportSaveState.Saving) {
            Log.w(TAG, "saveReport ignored: save already in progress")
            return
        }

        val currentUser = authRepository.currentUser()
        if (currentUser == null) {
            Log.e(TAG, "saveReport aborted: no authenticated user")
            saveState = CreateReportSaveState.Error
            saveErrorMessage = context.getString(R.string.error_no_auth_session)
            return
        }

        saveState = CreateReportSaveState.Saving
        saveErrorMessage = null

        viewModelScope.launch {
            val photoProcessingResults = formPhotos.map { reportPhoto ->
                val internalPath = PhotoUtils.copyPhotoToInternalStorage(
                    context.contentResolver,
                    context.filesDir,
                    reportPhoto.uri
                )
                reportPhoto.id to internalPath
            }

            if (formPhotos.isNotEmpty() && photoProcessingResults.all { it.second == null }) {
                Log.e(TAG, "saveReport failed: none of the selected photos could be processed")
                saveState = CreateReportSaveState.Error
                saveErrorMessage = context.getString(R.string.error_photo_processing_failed)
                return@launch
            }

            val startAt = combineStart()
            val endAt = combineEnd()

            val fishingPhotos = photoProcessingResults.mapNotNull { (id, path) ->
                path?.let { FishingPhoto(id = id, url = it) }
            }

            val report = FishingReport(
                id = currentReportId,
                userId = currentUser.id,
                type = formReportType,
                name = buildReportName(),
                water = Water(
                    waterName = formWaterName,
                    latitude = formLocation?.latitude ?: 0.0,
                    longitude = formLocation?.longitude ?: 0.0,
                    isPaid = formIsPaidWater
                ),
                spotLat = formLocation?.latitude,
                spotLng = formLocation?.longitude,
                photos = fishingPhotos,
                fishingStartAt = startAt,
                fishingEndAt = endAt,
                createdAt = Date(),
                weight = formWeight.toDouble(),
                fish = formSelectedFish,
                fishingMethod = formSelectedMethod,
                bait = formSelectedBaits,
                comment = formComment,
                user = currentUser,
                fishingFromTheShore = formFishingFromShore,
                isPublic = formIsPublic
            )

            val result = try {
                repository.saveReport(report)
            } catch (e: Exception) {
                Log.e(TAG, "saveReport failed", e)
                Result.failure(e)
            } finally {
                fishingPhotos.forEach { photo ->
                    runCatching { File(photo.url).delete() }
                }
            }

            result
                .onSuccess {
                    saveState = CreateReportSaveState.Success
                    onSuccess()
                    resetFormState()
                }
                .onFailure { e ->
                    Log.e(TAG, "saveReport failed: ${e.message}", e)
                    saveState = CreateReportSaveState.Error
                    saveErrorMessage = context.getString(R.string.error_save_report)
                }
        }
    }

    fun resetSaveState() {
        saveState = CreateReportSaveState.Idle
        saveErrorMessage = null
    }

    fun resetFormState() {
        currentReportId = UUID.randomUUID()
        formReportType = FishingType.FISHING_LOG
        formWaterName = ""
        formPhotos = emptyList()
        formStartDate = ""
        formStartTime = ""
        formEndDate = ""
        formEndTime = ""
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

    private fun buildReportName(): String {
        return listOfNotNull(
            context.getString(formSelectedMethod.labelRes).takeIf { it.isNotBlank() },
            formSelectedFish.firstOrNull()?.name?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
    }

    private fun combineStart(): Instant? {
        return combineDateAndTime(formStartDate, formStartTime)
    }

    private fun combineEnd(): Instant? {
        return combineDateAndTime(formEndDate, formEndTime)
    }

    private fun combineDateAndTime(dateString: String, timeString: String): Instant? {
        if (dateString.isBlank() || timeString.isBlank()) return null

        val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        return try {
            val datePart = dateFormatter.parse(dateString) ?: return null
            val calendar = Calendar.getInstance().apply {
                time = datePart
            }
            val timeParts = timeString.split(":")
            if (timeParts.size == 2) {
                calendar[Calendar.HOUR_OF_DAY] = timeParts[0].toInt()
                calendar[Calendar.MINUTE] = timeParts[1].toInt()
            }
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            calendar.time.toInstant()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "CreateReportVM"
    }
}

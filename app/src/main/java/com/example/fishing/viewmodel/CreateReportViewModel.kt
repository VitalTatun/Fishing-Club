package com.example.fishing.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.R
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.IoDispatcher
import com.example.fishing.data.upload.ReportUploadQueue
import com.example.fishing.model.*
import com.example.fishing.utils.PhotoUtils
import com.example.fishing.viewmodel.savedstate.baitsFromSaved
import com.example.fishing.viewmodel.savedstate.baitsToSaved
import com.example.fishing.viewmodel.savedstate.booleanProperty
import com.example.fishing.viewmodel.savedstate.enumProperty
import com.example.fishing.viewmodel.savedstate.fishFromSaved
import com.example.fishing.viewmodel.savedstate.fishToSaved
import com.example.fishing.viewmodel.savedstate.floatProperty
import com.example.fishing.viewmodel.savedstate.geoPointProperty
import com.example.fishing.viewmodel.savedstate.intProperty
import com.example.fishing.viewmodel.savedstate.listProperty
import com.example.fishing.viewmodel.savedstate.photosFromSaved
import com.example.fishing.viewmodel.savedstate.photosToSaved
import com.example.fishing.viewmodel.savedstate.stringProperty
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private data class ProcessedPhotos(
    val processed: List<Pair<UUID, String>>,
    val failed: List<ReportPhoto>
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository,
    private val reportUploadQueue: ReportUploadQueue,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    // The id is staged before the upload starts; persisting it means a retry
    // after process death updates the same queue row instead of duplicating it.
    private var formReportId by savedStateHandle.stringProperty(KEY_REPORT_ID) {
        UUID.randomUUID().toString()
    }

    /** Owner of the draft; null when no user was known when it was created. */
    private val formOwnerId: String?
        get() = savedStateHandle.get<String>(KEY_OWNER_ID)

    private var currentReportId: UUID
        get() {
            val parsed = runCatching { UUID.fromString(formReportId) }.getOrNull()
            if (parsed != null) return parsed
            // Corrupted/absent id: generate once and persist so save and cancel
            // keep addressing the same queue row.
            val generated = UUID.randomUUID()
            formReportId = generated.toString()
            return generated
        }
        set(value) {
            formReportId = value.toString()
        }

    private var saveJob: Job? = null
    private var pendingProcessedPaths: List<String> = emptyList()

    // Form state. Every field lives in SavedStateHandle so the draft survives
    // process death; names and types stay the same as before for callers.
    var formReportType by savedStateHandle.enumProperty(KEY_REPORT_TYPE) {
        FishingType.FISHING_LOG
    }
    var formWaterName by savedStateHandle.stringProperty(KEY_WATER_NAME)
    var formPhotos by savedStateHandle.listProperty(
        key = KEY_PHOTOS,
        defaultValue = { emptyList<ReportPhoto>() },
        save = { photosToSaved(it) },
        restore = { photosFromSaved(it) },
    )
    var formStartDate by savedStateHandle.stringProperty(KEY_START_DATE)
    var formStartTime by savedStateHandle.stringProperty(KEY_START_TIME)
    var formEndDate by savedStateHandle.stringProperty(KEY_END_DATE)
    var formEndTime by savedStateHandle.stringProperty(KEY_END_TIME)
    var formFishingFromShore by savedStateHandle.booleanProperty(KEY_FROM_SHORE) { true }
    var formIsPublic by savedStateHandle.booleanProperty(KEY_IS_PUBLIC) { true }
    var formIsPaidWater by savedStateHandle.booleanProperty(KEY_PAID_WATER) { false }
    var formWeight by savedStateHandle.floatProperty(KEY_WEIGHT) { 0f }
    var formSelectedMethod by savedStateHandle.enumProperty(KEY_METHOD) {
        FishingMethod.NONE
    }
    var formSelectedBaits by savedStateHandle.listProperty(
        key = KEY_BAITS,
        defaultValue = { emptyList<Bait>() },
        save = { baitsToSaved(it) },
        restore = { baitsFromSaved(it) },
    )
    var formSelectedFish by savedStateHandle.listProperty(
        key = KEY_FISH,
        defaultValue = { emptyList<Fish>() },
        save = { fishToSaved(it) },
        restore = { fishFromSaved(it) },
    )
    var formMood by savedStateHandle.intProperty(KEY_MOOD) { 3 }
    var formComment by savedStateHandle.stringProperty(KEY_COMMENT)
    var formLocation by savedStateHandle.geoPointProperty(KEY_GEO_LAT, KEY_GEO_LNG)

    var saveState by mutableStateOf(CreateReportSaveState.Idle)
    var saveErrorMessage by mutableStateOf<String?>(null)

    /** Null while photos are still being processed on-device. */
    var saveProgress by mutableStateOf<ReportSaveProgress?>(null)

    /**
     * Temp file handed to the camera app. Kept in the handle because the
     * ActivityResult callback is not replayed after process death.
     */
    var pendingCameraPath: String?
        get() = savedStateHandle.get<String>(KEY_PENDING_CAMERA)
        private set(value) {
            if (value == null) {
                savedStateHandle.remove<String>(KEY_PENDING_CAMERA)
            } else {
                savedStateHandle[KEY_PENDING_CAMERA] = value
            }
        }

    /** True while restored photos are being checked for readability. */
    var isRestoringPhotos by mutableStateOf(false)
        private set

    /** True while a captured photo is being persisted off the main thread. */
    var isPublishingCameraPhoto by mutableStateOf(false)
        private set

    /** Number of restored photos that could not be read, shown once as a snackbar. */
    var restoredPhotosLost by mutableStateOf(0)
        private set

    val isSaving: Boolean
        get() = saveState == CreateReportSaveState.Saving

    init {
        // Restored dates must win over freshly generated defaults.
        if (!savedStateHandle.contains(KEY_START_DATE)) {
            setDefaultDateTimeValues()
        }
        // Persist the id right away (and heal a corrupted one): a retry after
        // process death must update the same queued report instead of creating
        // a duplicate.
        savedStateHandle[KEY_REPORT_ID] = currentReportId.toString()
        bindDraftOwner(authRepository.currentUser()?.id?.toString())
        recoverInterruptedCameraCapture()
        reconcileRestoredPhotos()
        viewModelScope.launch {
            var lastAuthState: AuthState? = null
            authRepository.authState.collect { state ->
                bindDraftOwner((state as? AuthState.Authenticated)?.user?.id?.toString())
                // An authenticated session can become Unauthenticated on logout,
                // session expiry or a failed refresh; clear the draft on that
                // transition. The initial Unauthenticated reported on a cold
                // start must not wipe a restored draft.
                if (lastAuthState is AuthState.Authenticated &&
                    state is AuthState.Unauthenticated
                ) {
                    resetFormState()
                    resetSaveState()
                }
                lastAuthState = state
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
        val dtErrors = dateTimeErrors()
        val startError = dtErrors.firstOrNull {
            it == FishingDateTimeError.START_MISSING || it == FishingDateTimeError.START_IN_FUTURE
        }
        val endError = dtErrors.firstOrNull {
            it == FishingDateTimeError.END_MISSING ||
            it == FishingDateTimeError.END_IN_FUTURE ||
            it == FishingDateTimeError.END_NOT_AFTER_START
        }

        val startSupportingText = startError?.let { error ->
            context.getString(
                when (error) {
                    FishingDateTimeError.START_MISSING -> R.string.error_start_missing
                    FishingDateTimeError.START_IN_FUTURE -> R.string.error_start_in_future
                    else -> R.string.error_start_missing
                }
            )
        }

        val endSupportingText = endError?.let { error ->
            context.getString(
                when (error) {
                    FishingDateTimeError.END_MISSING -> R.string.error_end_missing
                    FishingDateTimeError.END_IN_FUTURE -> R.string.error_end_in_future
                    FishingDateTimeError.END_NOT_AFTER_START -> R.string.error_end_not_after_start
                    else -> R.string.error_end_missing
                }
            )
        }

        val dateTimeItems = mutableListOf<ReportField>(
            ReportField.ListItemField(
                fieldId = "date_time",
                title = formStartDate.ifEmpty { context.getString(R.string.start) },
                supportingText = startSupportingText,
                leadingIcon = if (startError != null) Icons.Default.Error else Icons.Default.Schedule,
                trailingText = formStartTime.ifEmpty { context.getString(R.string.select_time) },
                isError = startError != null
            ),
            ReportField.ListItemField(
                fieldId = "date_time_end",
                title = formEndDate.ifEmpty { context.getString(R.string.end) },
                supportingText = endSupportingText,
                leadingIcon = if (endError != null) Icons.Default.Error else null,
                trailingText = formEndTime.ifEmpty { context.getString(R.string.select_time) },
                isError = endError != null
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
        val hasBaits = formSelectedBaits.isNotEmpty()
        methodItems.add(
            ReportField.ListItemField(
                fieldId = "method",
                title = context.getString(R.string.method_and_bait),
                leadingIcon = Icons.Default.Phishing,
                isRequired = !hasMethod || !hasBaits
            )
        )
        if (hasMethod) {
            methodItems.add(
                ReportField.ListItemField(
                    fieldId = "fishing_method",
                    title = context.getString(R.string.fishing_method),
                    trailingText = context.getString(formSelectedMethod.labelRes)
                )
            )
        }
        if (hasBaits) {
            val baitsText = formSelectedBaits.joinToString(", ") { context.getString(it.labelRes) }
            methodItems.add(
                ReportField.ListItemField(
                    fieldId = "baits",
                    title = context.getString(R.string.bait),
                    trailingText = baitsText
                )
            )
        }
        sections.add(ReportFormSection(id = "method", items = methodItems))

        // Catch Section
        val catchItems = mutableListOf<ReportField>()
        val hasCatch = formSelectedFish.isNotEmpty()
        catchItems.add(
            ReportField.ListItemField(
                fieldId = "catch",
                title = context.getString(R.string.catch_label),
                leadingIcon = Icons.Default.SetMeal,
                isRequired = !hasCatch
            )
        )
        if (hasCatch && formWeight > 0f) {
            catchItems.add(
                ReportField.ListItemField(
                    fieldId = "weight",
                    title = context.getString(R.string.total_weight),
                    trailingText = "$formWeight ${context.getString(R.string.kg)}"
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
            if (isSaving || isRestoringPhotos || isPublishingCameraPhoto) return false
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
                formWeight > 0f

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
        saveProgress = null
        pendingProcessedPaths = emptyList()
        val photosSnapshot = formPhotos
        val reportId = currentReportId

        saveJob = viewModelScope.launch {
            try {
                // Bitmap decoding, compression and file writes are heavy: never do
                // them on the Main thread.
                val photoResult = withContext(ioDispatcher) {
                    val results = photosSnapshot.map { reportPhoto ->
                        reportPhoto to PhotoUtils.copyPhotoToInternalStorage(
                            context.contentResolver,
                            context.filesDir,
                            reportPhoto.uri
                        )
                    }
                    ProcessedPhotos(
                        processed = results.mapNotNull { (photo, path) ->
                            path?.let { photo.id to it }
                        },
                        failed = results.filter { it.second == null }.map { it.first }
                    )
                }

                if (photoResult.failed.isNotEmpty()) {
                    // Never save silently with fewer photos than the user selected.
                    Log.e(
                        TAG,
                        "saveReport aborted: ${photoResult.failed.size} photo(s) could not be processed"
                    )
                    withContext(ioDispatcher) {
                        cleanupTempPhotos(
                            photos = emptyList(),
                            extraFiles = photoResult.processed.map { it.second }
                        )
                    }
                    saveState = CreateReportSaveState.Error
                    saveErrorMessage = context.getString(
                        R.string.error_photos_partially_processed,
                        photoResult.failed.size
                    )
                    return@launch
                }

                val startAt = combineStart()
                val endAt = combineEnd()

                val fishingPhotos = photoResult.processed.map { (id, path) ->
                    FishingPhoto(id = id, url = path)
                }
                val localPaths = photoResult.processed.map { it.second }
                pendingProcessedPaths = localPaths

                val report = FishingReport(
                    id = reportId,
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

                // The queue row is the source of truth: WorkManager picks the report
                // up, survives process death and keeps retrying with backoff.
                val enqueued = reportUploadQueue.enqueue(
                    report = report,
                    localPhotoPaths = localPaths,
                    temporaryPhotoPaths = localPaths
                )
                val enqueueError = enqueued.exceptionOrNull()
                if (enqueueError != null) {
                    Log.e(TAG, "saveReport: could not enqueue upload", enqueueError)
                    saveState = CreateReportSaveState.Error
                    saveErrorMessage = ErrorMapper.toUserFriendlyMessage(enqueueError)
                    return@launch
                }

                // Progress and completion come from the queue, so the screen keeps
                // showing the right state even after the form itself is gone.
                val terminalRows = reportUploadQueue.observePending()
                    .onEach { rows ->
                        val row = rows.firstOrNull { it.reportId == reportId }
                        if (row != null) {
                            saveProgress = ReportSaveProgress(
                                uploadedPhotos = row.uploadedPhotos,
                                totalPhotos = row.totalPhotos
                            )
                        }
                    }
                    .first { rows ->
                        val row = rows.firstOrNull { it.reportId == reportId }
                        row == null || row.state == ReportUploadState.FAILED
                    }

                val failedRow = terminalRows.firstOrNull { it.reportId == reportId }
                if (failedRow == null) {
                    // The worker removed the row only after a successful publish.
                    withContext(ioDispatcher) {
                        cleanupTempPhotos(
                            photos = photosSnapshot,
                            extraFiles = pendingProcessedPaths
                        )
                    }
                    pendingProcessedPaths = emptyList()
                    saveProgress = null
                    saveState = CreateReportSaveState.Success
                    onSuccess()
                    resetFormState(cleanupPhotos = false)
                } else {
                    saveState = CreateReportSaveState.Error
                    saveErrorMessage = failedRow.lastError
                        ?: context.getString(R.string.error_save_report)
                }
            } catch (e: CancellationException) {
                // Cancellation is not an error; the form and photo files stay intact.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveReport failed", e)
                saveState = CreateReportSaveState.Error
                saveErrorMessage = ErrorMapper.toUserFriendlyMessage(e)
            } finally {
                if (saveJob === coroutineContext[Job]) {
                    saveJob = null
                }
            }
        }
    }

    fun cancelSave() {
        val job = saveJob
        val reportId = currentReportId
        saveJob = null
        saveProgress = null
        resetSaveState()

        viewModelScope.launch {
            // Wait for the cancelled job first so an enqueue that was already in
            // flight is visible to the queue cancellation.
            job?.cancelAndJoin()
            reportUploadQueue.cancel(reportId)
            // When the job was cancelled before the queue row was written there is
            // nothing for the queue to clean up, so remove its copies here.
            withContext(ioDispatcher) {
                pendingProcessedPaths.forEach { path ->
                    runCatching { File(path).delete() }
                }
            }
            pendingProcessedPaths = emptyList()
        }
    }

    fun resetSaveState() {
        saveState = CreateReportSaveState.Idle
        saveErrorMessage = null
        saveProgress = null
    }

    fun resetFormState(cleanupPhotos: Boolean = true) {
        val photosToDelete = formPhotos
        releasePersistedPhotoPermissions(photosToDelete)
        discardPendingCameraCapture()
        restoredPhotosLost = 0
        isRestoringPhotos = false
        isPublishingCameraPhoto = false
        currentReportId = UUID.randomUUID()
        formReportType = FishingType.FISHING_LOG
        formWaterName = ""
        formPhotos = emptyList()
        setDefaultDateTimeValues()
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
        updateDraftOwner(authRepository.currentUser()?.id?.toString())
        if (cleanupPhotos) {
            viewModelScope.launch(ioDispatcher) {
                cleanupTempPhotos(photos = photosToDelete)
            }
        }
    }

    /**
     * Removes a single photo from the draft, drops its gallery grant and deletes
     * a local draft file. Blocked while a save is in flight: the upload snapshot
     * may still reference the file.
     *
     * Only files inside `filesDir/photos` are deleted, and a path still
     * referenced by the upload queue (for example a FAILED row) is kept.
     */
    fun removePhoto(photo: ReportPhoto) {
        if (isSaving) return
        if (formPhotos.none { it.id == photo.id }) return
        formPhotos = formPhotos.filterNot { it.id == photo.id }
        releasePersistedPhotoPermissions(listOf(photo))
        if (photo.uri.scheme == "file") {
            val path = photo.uri.path ?: return
            viewModelScope.launch(ioDispatcher) {
                val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@launch
                val photosDir = runCatching {
                    File(context.filesDir, "photos").canonicalFile
                }.getOrNull() ?: return@launch
                if (!file.path.startsWith(photosDir.path + File.separator)) return@launch
                val referenced = runCatching {
                    reportUploadQueue.referencedLocalPaths()
                }.getOrDefault(emptySet())
                val keptByQueue = referenced.any { queued ->
                    runCatching { File(queued).canonicalPath }.getOrNull() == file.path
                }
                if (!keptByQueue) {
                    runCatching { file.delete() }
                }
            }
        }
    }

    /**
     * Keeps persisted read access to picked gallery URIs. Best effort: the
     * picker contract does not always offer a persistable grant. Runs off the
     * main thread because it is a binder call per URI.
     */
    fun retainPickedPhotoAccess(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(ioDispatcher) {
            uris.forEach { uri ->
                if (uri.scheme != "content") return@forEach
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
    }

    /**
     * Claims a temp file before launching the camera. A previous temp file that
     * was never returned by the camera app is deleted off the main thread.
     */
    fun beginCameraCapture(file: File) {
        val previous = pendingCameraPath
        pendingCameraPath = file.absolutePath
        if (previous != null && previous != file.absolutePath) {
            viewModelScope.launch(ioDispatcher) {
                runCatching { File(previous).delete() }
            }
        }
    }

    /**
     * Handles the camera result. The temp path is consumed first: whatever
     * happens next, a later restoration must not process the same file twice.
     */
    fun onCameraCaptureResult(success: Boolean) {
        val path = pendingCameraPath ?: return
        pendingCameraPath = null
        isPublishingCameraPhoto = true
        val reportId = currentReportId
        val file = File(path)
        viewModelScope.launch {
            try {
                val readable = withContext(ioDispatcher) { file.isFile && file.length() > 0 }
                if (success && readable) {
                    publishCameraPhoto(file, reportId)
                } else {
                    withContext(ioDispatcher) {
                        runCatching { file.delete() }
                    }
                }
            } finally {
                if (currentReportId == reportId) {
                    isPublishingCameraPhoto = false
                }
            }
        }
    }

    /** Resets the one-shot warning after the screen showed its snackbar. */
    fun consumeRestoredPhotosLost() {
        restoredPhotosLost = 0
    }

    /**
     * After process death the camera result callback is usually not delivered,
     * but the file the camera wrote is still in its temporary location. Treat a
     * non-empty file as a successful capture so the photo is not silently lost.
     */
    private fun recoverInterruptedCameraCapture() {
        val path = pendingCameraPath ?: return
        pendingCameraPath = null
        isPublishingCameraPhoto = true
        val reportId = currentReportId
        val file = File(path)
        viewModelScope.launch {
            try {
                val readable = withContext(ioDispatcher) { file.isFile && file.length() > 0 }
                if (readable) {
                    publishCameraPhoto(file, reportId)
                } else {
                    withContext(ioDispatcher) {
                        runCatching { file.delete() }
                    }
                }
            } finally {
                if (currentReportId == reportId) {
                    isPublishingCameraPhoto = false
                }
            }
        }
    }

    private fun discardPendingCameraCapture() {
        val path = pendingCameraPath
        pendingCameraPath = null
        if (path != null) {
            // Best effort: if the ViewModel scope is already cancelled the
            // delete may not run. NonCancellable only helps once the coroutine
            // has started.
            viewModelScope.launch(ioDispatcher) {
                withContext(NonCancellable) {
                    runCatching { File(path).delete() }
                }
            }
        }
    }

    private suspend fun publishCameraPhoto(file: File, reportId: UUID) {
        val persistedUri = withContext(ioDispatcher) {
            runCatching {
                PhotoUtils.persistCameraPhoto(
                    context.contentResolver,
                    context.filesDir,
                    Uri.fromFile(file)
                )
            }.getOrNull().also { persisted ->
                // Never delete the file we just published: persistCameraPhoto
                // short-circuits when the source already lives in photosDir.
                if (persisted?.path != file.absolutePath) {
                    runCatching { file.delete() }
                }
            }
        } ?: return
        if (currentReportId != reportId) {
            // The draft was reset (logout, account switch, successful save)
            // while the photo was being processed: drop the result.
            withContext(ioDispatcher) {
                persistedUri.path?.let { path -> runCatching { File(path).delete() } }
            }
            return
        }
        val current = formPhotos
        if (current.none { it.uri == persistedUri }) {
            formPhotos = current + ReportPhoto(uri = persistedUri)
        }
    }

    /**
     * Restored photo URIs may be gone (gallery grants expire, startup cleanup
     * removed internal files). Drop unreadable entries once and tell the user.
     */
    private fun reconcileRestoredPhotos() {
        val restored = formPhotos
        if (restored.isEmpty()) {
            isRestoringPhotos = false
            return
        }
        val reportId = currentReportId
        isRestoringPhotos = true
        viewModelScope.launch {
            try {
                val readableIds = withContext(ioDispatcher) {
                    restored.filter { isPhotoReadable(it.uri) }.map { it.id }.toSet()
                }
                // A reset while checking replaced the draft; do not touch it.
                if (currentReportId != reportId) return@launch
                val lost = restored.filterNot { it.id in readableIds }
                if (lost.isNotEmpty()) {
                    val lostIds = lost.map { it.id }.toSet()
                    formPhotos = formPhotos.filterNot { it.id in lostIds }
                    restoredPhotosLost += lost.size
                    // Give back the grants of photos that are no longer usable.
                    releasePersistedPhotoPermissions(lost)
                }
            } finally {
                // Never leave Save disabled if a provider check throws. A reset
                // already cleared the flag for an outdated check.
                if (currentReportId == reportId) {
                    isRestoringPhotos = false
                }
            }
        }
    }

    private fun isPhotoReadable(uri: Uri): Boolean = runCatching {
        when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.let { it.isFile && it.length() > 0 } ?: false
            else -> context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }
    }.getOrDefault(false)

    /**
     * A draft is not transferable between accounts: saved instance state from
     * the previous user must not surface after a different user signs in.
     * An as-yet unowned draft is claimed by the current user without a reset.
     *
     * Runs on inits and every auth emission. `Unauthenticated` can also arrive
     * from a failed session refresh, which keeps the draft (no user id to
     * compare against) instead of clearing it.
     */
    private fun bindDraftOwner(currentUserId: String?) {
        val storedOwnerId = formOwnerId
        if (storedOwnerId == null) {
            if (currentUserId != null) {
                updateDraftOwner(currentUserId)
            }
            return
        }
        if (currentUserId != null && storedOwnerId != currentUserId) {
            resetFormState()
            updateDraftOwner(currentUserId)
        }
    }

    private fun updateDraftOwner(ownerId: String?) {
        if (ownerId == null) {
            savedStateHandle.remove<String>(KEY_OWNER_ID)
        } else {
            savedStateHandle[KEY_OWNER_ID] = ownerId
        }
    }

    /**
     * Releases persisted read grants off the main thread. Called on every path
     * that forgets photos: reset, reset-on-logout, per-photo removal and
     * unreadable restored photos.
     */
    private fun releasePersistedPhotoPermissions(photos: List<ReportPhoto>) {
        if (photos.isEmpty()) return
        viewModelScope.launch(ioDispatcher) {
            photos.forEach { photo ->
                val uri = photo.uri
                if (uri.scheme != "content") return@forEach
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
    }

    private fun setDefaultDateTimeValues() {
        val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endCal = Calendar.getInstance()
        val startCal = (endCal.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, -1)
        }

        formStartDate = dateFormatter.format(startCal.time)
        formStartTime = timeFormatter.format(startCal.time)
        formEndDate = dateFormatter.format(endCal.time)
        formEndTime = timeFormatter.format(endCal.time)
    }

    private fun cleanupTempPhotos(
        photos: List<ReportPhoto> = formPhotos,
        extraFiles: List<String> = emptyList()
    ) {
        val paths = photos.mapNotNull { photo ->
            photo.uri.path?.takeIf { photo.uri.scheme == "file" }
        } + extraFiles
        paths.forEach { path ->
            runCatching { File(path).delete() }
        }
        // Remove the directory only when nothing else is using it.
        runCatching { File(context.filesDir, "photos").delete() }
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

        // `form_` is a namespace for the report draft. FishingNavHost writes its
        // sub-screen results to the destination's own SavedStateHandle, while
        // this ViewModel gets the activity-level handle, so the prefix is
        // hygiene rather than a collision fix.
        private const val KEY_REPORT_TYPE = "form_report_type"
        private const val KEY_WATER_NAME = "form_water_name"
        private const val KEY_PHOTOS = "form_photo_entries"
        private const val KEY_START_DATE = "form_start_date"
        private const val KEY_START_TIME = "form_start_time"
        private const val KEY_END_DATE = "form_end_date"
        private const val KEY_END_TIME = "form_end_time"
        private const val KEY_FROM_SHORE = "form_from_shore"
        private const val KEY_IS_PUBLIC = "form_is_public"
        private const val KEY_PAID_WATER = "form_paid_water"
        private const val KEY_WEIGHT = "form_weight"
        private const val KEY_METHOD = "form_method"
        private const val KEY_BAITS = "form_baits"
        private const val KEY_FISH = "form_fish"
        private const val KEY_MOOD = "form_mood"
        private const val KEY_COMMENT = "form_comment"
        private const val KEY_GEO_LAT = "form_geo_lat"
        private const val KEY_GEO_LNG = "form_geo_lng"
        private const val KEY_REPORT_ID = "form_report_id"
        private const val KEY_OWNER_ID = "form_owner_id"
        private const val KEY_PENDING_CAMERA = "form_pending_camera_path"
    }
}

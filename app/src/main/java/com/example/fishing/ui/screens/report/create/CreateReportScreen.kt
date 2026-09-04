package com.example.fishing.ui.screens.report.create

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.data.UserPreferencesRepository
import com.example.fishing.ui.theme.FishingTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.viewmodel.MainViewModel
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onSaveClick: (
        title: String,
        type: FishingType,
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
        photos: List<String>,
    ) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCatchEdit: () -> Unit = {},
    onNavigateToMethodAndBaitEdit: () -> Unit = {},
    onNavigateToCommentEdit: () -> Unit = {},
    onNavigateToWaterEdit: () -> Unit = {},
    onNavigateToWaterNameEdit: () -> Unit = {},
) {
    val calendar = remember { Calendar.getInstance() }
    val haptic = LocalHapticFeedback.current
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (viewModel.formFishingDate.isEmpty()) {
            viewModel.formFishingDate = dateFormatter.format(calendar.time)
        }
        if (viewModel.formFishingStartTime.isEmpty()) {
            viewModel.formFishingStartTime = timeFormatter.format(calendar.time)
        }
    }

    val isTrophy = viewModel.formReportType == FishingType.HAUL

    val isSaveEnabled by remember {
        derivedStateOf {
            val baseValid = (viewModel.formWaterName.isNotBlank() &&
                    viewModel.formLocation != null &&
                    viewModel.formSelectedMethod != FishingMethod.NONE &&
                    viewModel.formSelectedBaits.isNotEmpty() &&
                    viewModel.formSelectedFish.isNotEmpty() &&
                    viewModel.formFishingDate.isNotBlank())

            if (isTrophy) {
                baseValid &&
                        viewModel.formSelectedPhotoUris.isNotEmpty() &&
                        viewModel.formSelectedFish.size == 1
            } else {
                baseValid
            }
        }
    }

    val formHasData by remember {
        derivedStateOf {
            viewModel.formWaterName.isNotBlank() ||
                    viewModel.formLocation != null ||
                    viewModel.formSelectedMethod != FishingMethod.NONE ||
                    viewModel.formSelectedFish.isNotEmpty() ||
                    viewModel.formSelectedBaits.isNotEmpty() ||
                    viewModel.formSelectedPhotoUris.isNotEmpty() ||
                    viewModel.formComment.isNotBlank() ||
                    viewModel.formWeight > 0f
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isDetailsExpanded by remember { mutableStateOf(false) }

    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MaxPhotos)
    ) { uris ->
        val currentUris = viewModel.formSelectedPhotoUris
        val availableSlots = MaxPhotos - currentUris.size
        val newUris = uris.take(availableSlots).filter { it !in currentUris }
        viewModel.formSelectedPhotoUris = currentUris + newUris
    }

    val handleBack = {
        if (formHasData) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }

    BackHandler(enabled = formHasData) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.cancel)) },
            text = { Text(stringResource(R.string.discard_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.resetFormState()
                    onBackClick()
                }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.stay))
                }
            }
        )
    }

    if (showDatePicker) {
        FishingDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                millis?.let {
                    val d = Date(it)
                    viewModel.formFishingDate = dateFormatter.format(d)
                }
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    timePickerState.hour,
                    timePickerState.minute
                )
                viewModel.formFishingStartTime = formattedTime
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_report),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val combinedDateTime = combineDateAndTime(
                                viewModel.formFishingDate,
                                viewModel.formFishingStartTime,
                                dateFormatter
                            )

                            onSaveClick(
                                viewModel.formTitle, viewModel.formReportType, viewModel.formWaterName,
                                viewModel.formLocation, combinedDateTime,
                                viewModel.formWeight.toDouble(), viewModel.formSelectedFish,
                                viewModel.formSelectedMethod, viewModel.formSelectedBaits,
                                viewModel.formComment, viewModel.formFishingFromShore,
                                viewModel.formIsPublic, viewModel.formIsPaidWater,
                                viewModel.formSelectedPhotoUris.map { it.toString() }
                            )
                        },
                        enabled = isSaveEnabled
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                SectionGroup {
                    ReportTypeSelector(
                        reportType = viewModel.formReportType,
                        onReportTypeChange = { newType ->
                            viewModel.formReportType = newType
                            if (newType == FishingType.HAUL && viewModel.formSelectedFish.size > 1) {
                                viewModel.formSelectedFish = listOf(viewModel.formSelectedFish.first().copy(count = 1))
                            }
                        },
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

            }
            item {
                SectionGroup {
                    FishingListItem(
                        title = viewModel.formFishingDate,
                        onTitleClick = { showDatePicker = true },
                        leadingIcon = Icons.Default.Schedule,
                        trailingText = viewModel.formFishingStartTime,
                        onTrailingTextClick = { showTimePicker = true }
                    )
                    FishingListItem(
                        title = "Вс, 2 августа 2026",
                        trailingText = "4:00",
                    )
                    FishingListItem (
                        title = "Опубликовать",
                        supportingText = stringResource(R.string.publish_supporting),
                        leadingIcon = Icons.Default.PublishedWithChanges,
                        trailingContent = {
                            Switch(
                                checked = viewModel.formIsPublic,
                                onCheckedChange = {
                                    viewModel.formIsPublic = it
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                }
                            )
                        }
                    )
                }
            }

            item {
                SectionGroup {
                    FishingListItem(
                        title = stringResource(R.string.photos),
                        supportingText = stringResource(R.string.photos_subtitle),
                        leadingIcon = Icons.Default.AddPhotoAlternate,
                        onRowClick = {
                            if (viewModel.formSelectedPhotoUris.size < MaxPhotos) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    )

                    if (viewModel.formSelectedPhotoUris.isNotEmpty()) {
                        ReportPhotosList(
                            selectedPhotoUris = viewModel.formSelectedPhotoUris,
                            onRemoveClick = { uri ->
                                viewModel.formSelectedPhotoUris = viewModel.formSelectedPhotoUris - uri
                            }
                        )
                    }
                }
            }
            item {

                SectionGroup {
                    FishingListItem(
                        title = stringResource(R.string.water_body),
                        leadingIcon = Icons.Default.LocationOn,
                        onRowClick = onNavigateToWaterEdit,
                        isRequired = true
                    )
                    if (viewModel.formLocation != null) {
                        MapPreview(
                            location = viewModel.formLocation,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (viewModel.formWaterName.isEmpty()) {
                            FishingListItem(
                                title = stringResource(R.string.add_water_name_button),
                                leadingIcon = Icons.Default.Add,
                                onRowClick = onNavigateToWaterNameEdit
                            )
                        } else {
                            FishingListItem(
                                title = viewModel.formWaterName,
                                supportingText = viewModel.formLocation?.let {
                                    "Координаты: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}"
                                },
                                onRowClick = onNavigateToWaterNameEdit
                            )
                        }
                    }
                    if (viewModel.formWaterName.isNotEmpty()) {
                        val shoreText = stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat)
                        val paidText = if (viewModel.formIsPaidWater) " • ${stringResource(R.string.paid)}" else ""
                        FishingListItem(
                            title = "Детали",
                            supportingText = if (!isDetailsExpanded) "$shoreText$paidText" else null,
                            onRowClick = { isDetailsExpanded = !isDetailsExpanded }
                        )

                        if (isDetailsExpanded) {
                            WaterDetailsItems(viewModel, haptic)
                        }
                    }
                }
            }
            item {
                MethodAndBaitSection(
                    selectedMethod = viewModel.formSelectedMethod,
                    selectedBaits = viewModel.formSelectedBaits,
                    onArrowClick = onNavigateToMethodAndBaitEdit,
                    isRequired = true
                )
            }
            item {
                CatchSection(
                    selectedFish = viewModel.formSelectedFish,
                    onArrowClick = onNavigateToCatchEdit,
                    weight = viewModel.formWeight,
                    isRequired = true
                )
            }
            item {
                CommentSection(
                    comment = viewModel.formComment,
                    onArrowClick = onNavigateToCommentEdit
                )
            }
            item {
                Text(
                    text = stringResource(R.string.required_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = CreateReportColors.OnSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun combineDateAndTime(
    dateString: String,
    timeString: String,
    dateFormatter: SimpleDateFormat
): Date {
    val calendar = Calendar.getInstance()
    calendar.time = dateFormatter.parse(dateString) ?: Date()
    val timeParts = timeString.split(":")
    if (timeParts.size == 2) {
        calendar[Calendar.HOUR_OF_DAY] = timeParts[0].toInt()
        calendar[Calendar.MINUTE] = timeParts[1].toInt()
    }
    return calendar.time
}

@Composable
private fun WaterDetailsItems(
    viewModel: MainViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    FishingListItem(
        title = stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat),
        trailingContent = {
            Switch(
                checked = viewModel.formFishingFromShore,
                onCheckedChange = {
                    viewModel.formFishingFromShore = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        }
    )
    FishingListItem(
        title = stringResource(R.string.paid_water),
        trailingContent = {
            Switch(
                checked = viewModel.formIsPaidWater,
                onCheckedChange = {
                    viewModel.formIsPaidWater = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        }
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CreateReportScreenPreview() {
    val context = LocalContext.current
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val viewModel = remember {
            MainViewModel(
                repository = com.example.fishing.data.MockFishingRepository(),
                authRepository = object : com.example.fishing.data.AuthRepository {
                    override suspend fun login(email: String, password: String) = Result.failure<com.example.fishing.model.User>(Exception("mock"))
                    override suspend fun register(email: String, password: String, name: String) = Result.failure<com.example.fishing.model.User>(Exception("mock"))
                    override suspend fun logout() {}
                    override fun currentUser(): com.example.fishing.model.User? = null
                    override fun isLoggedIn() = false
                    override suspend fun loadSession() {}
                    override suspend fun updateProfile(name: String, imageUri: String?): Result<com.example.fishing.model.User> = Result.failure(Exception("mock"))
                    override val userStatus: kotlinx.coroutines.flow.Flow<com.example.fishing.model.User?> = kotlinx.coroutines.flow.flowOf(null)
                    override fun resolveImageUrl(path: String): String = ""
                },
                userPreferencesRepository = UserPreferencesRepository(context)
            )
        }
        CreateReportScreen(
            viewModel = viewModel,
            onBackClick = {},
            onSaveClick = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

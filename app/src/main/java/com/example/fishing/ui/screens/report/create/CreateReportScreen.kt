package com.example.fishing.ui.screens.report.create

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.example.fishing.viewmodel.CreateReportViewModel
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.MockFishingRepository
import com.example.fishing.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    viewModel: CreateReportViewModel,
    onBackClick: () -> Unit,
    onSaveComplete: () -> Unit,
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

    val isTrophy = viewModel.isTrophy
    val isSaveEnabled = viewModel.isSaveEnabled
    val formHasData = viewModel.formHasData

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
                            viewModel.saveReport(onSuccess = onSaveComplete)
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
                    ListItem(
                        headlineContent = {
                            Text(
                                text = viewModel.formFishingDate,
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Text(
                                text = viewModel.formFishingStartTime,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Вс, 2 августа 2026") },
                        trailingContent = {
                            Text(
                                text = "4:00",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.padding(start = 40.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Опубликовать") },
                        supportingContent = { Text(stringResource(R.string.publish_supporting)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.PublishedWithChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
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
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.photos)) },
                        supportingContent = { Text(stringResource(R.string.photos_subtitle)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
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
                    val hasLocation = viewModel.formLocation != null
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.water_body) + if (!hasLocation) " *" else ""
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onNavigateToWaterEdit() }
                    )

                    if (hasLocation) {
                        MapPreview(
                            location = viewModel.formLocation,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (viewModel.formWaterName.isEmpty()) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.add_water_name_button)) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.clickable { onNavigateToWaterNameEdit() }
                            )
                        } else {
                            ListItem(
                                headlineContent = { Text(viewModel.formWaterName) },
                                supportingContent = {
                                    Text(
                                        text = "Координаты: ${"%.5f".format(viewModel.formLocation?.latitude)}, ${"%.5f".format(viewModel.formLocation?.longitude)}"
                                    )
                                },
                                modifier = Modifier
                                    .padding(start = 40.dp)
                                    .clickable { onNavigateToWaterNameEdit() }
                            )
                        }

                        if (viewModel.formWaterName.isNotEmpty()) {
                            val shoreText = stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat)
                            val paidText = if (viewModel.formIsPaidWater) " • ${stringResource(R.string.paid)}" else ""
                            ListItem(
                                headlineContent = { Text("Детали") },
                                supportingContent = if (!isDetailsExpanded) {
                                    { Text("$shoreText$paidText") }
                                } else null,
                                modifier = Modifier
                                    .padding(start = 40.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isDetailsExpanded = !isDetailsExpanded }
                            )

                            if (isDetailsExpanded) {
                                WaterDetailsItems(viewModel, haptic)
                            }
                        }
                    }
                }
            }
            item {
                SectionGroup {
                    val hasMethod = viewModel.formSelectedMethod != FishingMethod.NONE
                    ListItem(
                        overlineContent = if (hasMethod) {
                            {
                                Text(stringResource(R.string.fishing_method),
                                style = MaterialTheme.typography.bodyMedium)
                            }
                        } else null,
                        headlineContent = {
                            Text(
                                if (hasMethod) stringResource(viewModel.formSelectedMethod.labelRes)
                                else stringResource(R.string.method_and_bait)
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Phishing,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onNavigateToMethodAndBaitEdit() }
                    )
                    if (viewModel.formSelectedBaits.isNotEmpty()) {
                        val baitsText = viewModel.formSelectedBaits.map { stringResource(it.labelRes) }.joinToString(", ")
                        ListItem(
                            overlineContent = {
                                Text(stringResource(R.string.bait),
                                style = MaterialTheme.typography.bodyMedium)
                                              },
                            headlineContent = { Text(baitsText) },
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }
            }
            item {
                SectionGroup {
                    val hasCatch = viewModel.formSelectedFish.isNotEmpty()
                    val firstFish = viewModel.formSelectedFish.firstOrNull()
                    ListItem(
                        headlineContent = {
                            Text(
                                if (hasCatch && firstFish != null) firstFish.name
                                else stringResource(R.string.catch_label)
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.SetMeal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = if (hasCatch && firstFish != null) {
                            {
                                Text(
                                    text = stringResource(R.string.fish_count_short, firstFish.count),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface

                                )
                            }
                        } else null,
                        modifier = Modifier.clickable { onNavigateToCatchEdit() }
                    )

                    if (viewModel.formSelectedFish.size > 1) {
                        viewModel.formSelectedFish.drop(1).forEach { fish ->
                            ListItem(
                                headlineContent = { Text(fish.name) },
                                trailingContent = {
                                    Text(
                                        text = stringResource(R.string.fish_count_short, fish.count),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface

                                    )
                                },
                                modifier = Modifier.padding(start = 40.dp)
                            )
                        }
                    }

                    if (viewModel.formWeight > 0f) {
                        ListItem(
                            overlineContent = { Text(stringResource(R.string.total_weight),
                                style = MaterialTheme.typography.bodyMedium,
                            )  },
                            headlineContent = { Text("${viewModel.formWeight} ${stringResource(R.string.kg)}") },
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }
            }
            item {
                SectionGroup {
                    val hasComment = viewModel.formComment.isNotBlank()
                    ListItem(

                        headlineContent = {
                            Text(
                                text = if (hasComment) viewModel.formComment else stringResource(R.string.comment),
                                maxLines = if (hasComment) 5 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onNavigateToCommentEdit() }
                    )
                }
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

@Composable
private fun WaterDetailsItems(
    viewModel: CreateReportViewModel,
    haptic: HapticFeedback
) {
    ListItem(
        headlineContent = {
            Text(stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat))
        },
        trailingContent = {
            Switch(
                checked = viewModel.formFishingFromShore,
                onCheckedChange = {
                    viewModel.formFishingFromShore = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        },
        modifier = Modifier.padding(start = 40.dp)
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.paid_water)) },
        trailingContent = {
            Switch(
                checked = viewModel.formIsPaidWater,
                onCheckedChange = {
                    viewModel.formIsPaidWater = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        },
        modifier = Modifier.padding(start = 40.dp)
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CreateReportScreenPreview() {
    val context = LocalContext.current
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val viewModel = remember {
            CreateReportViewModel(
                repository = MockFishingRepository(),
                authRepository = object : AuthRepository {
                    override suspend fun login(email: String, password: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun register(email: String, password: String, name: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun logout() {}
                    override fun currentUser(): User? = null
                    override fun isLoggedIn() = false
                    override suspend fun loadSession() {}
                    override suspend fun updateProfile(name: String, imageUri: String?): Result<User> = Result.failure(Exception("mock"))
                    override val userStatus: Flow<User?> = flowOf(null)
                    override fun resolveImageUrl(path: String): String = ""
                },
                context = context
            )
        }
        CreateReportScreen(
            viewModel = viewModel,
            onBackClick = {},
            onSaveComplete = {}
        )
    }
}

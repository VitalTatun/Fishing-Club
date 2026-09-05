package com.example.fishing.ui.screens.report.create

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.fishing.ui.theme.FishingTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.fishing.model.*
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.viewmodel.CreateReportViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.MockFishingRepository
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
                    Button(
                        onClick = {
                            viewModel.saveReport(onSuccess = onSaveComplete)
                        },
                        enabled = isSaveEnabled,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        val formConfig = viewModel.formConfig
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(formConfig) { section ->
                SectionGroup {
                    section.items.forEach { field ->
                        ReportFieldRenderer(
                            field = field,
                            viewModel = viewModel,
                            onNavigateToWaterEdit = onNavigateToWaterEdit,
                            onNavigateToWaterNameEdit = onNavigateToWaterNameEdit,
                            onNavigateToMethodAndBaitEdit = onNavigateToMethodAndBaitEdit,
                            onNavigateToCatchEdit = onNavigateToCatchEdit,
                            onNavigateToCommentEdit = onNavigateToCommentEdit,
                            onDatePickerClick = { showDatePicker = true },
                            onTimePickerClick = { showTimePicker = true },
                            onPhotoPickerClick = {
                                if (viewModel.formSelectedPhotoUris.size < MaxPhotos) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            isDetailsExpanded = isDetailsExpanded,
                            onDetailsExpandClick = { isDetailsExpanded = !isDetailsExpanded },
                            haptic = haptic
                        )
                    }
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

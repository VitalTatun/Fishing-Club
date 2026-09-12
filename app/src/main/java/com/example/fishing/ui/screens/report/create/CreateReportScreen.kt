package com.example.fishing.ui.screens.report.create

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fishing.R
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.MockFishingRepository
import com.example.fishing.model.*
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.utils.PhotoUtils
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.ReportPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val haptic = LocalHapticFeedback.current
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru")) }
    val savingText = stringResource(R.string.saving)

    val isSaveEnabled = viewModel.isSaveEnabled
    val isSaving = viewModel.isSaving
    val saveErrorMessage = viewModel.saveErrorMessage
    val formHasData = viewModel.formHasData
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveErrorMessage) {
        saveErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetSaveState()
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var pickerTarget by remember { mutableStateOf("start") }
    var isDetailsExpanded by remember { mutableStateOf(false) }

    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    val currentTime = Calendar.getInstance()
    val startTimePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MaxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val currentPhotos = viewModel.formPhotos
            val currentUris = currentPhotos.map { it.uri }
            val availableSlots = MaxPhotos - currentPhotos.size
            val newUris = uris.take(availableSlots).filter { it !in currentUris }
            viewModel.formPhotos = currentPhotos + newUris.map { ReportPhoto(uri = it) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                val uri = Uri.fromFile(file)
                viewModel.formPhotos = viewModel.formPhotos + ReportPhoto(uri = uri)
            }
        } else {
            tempCameraFile?.delete()
        }
        tempCameraFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = PhotoUtils.createTempPhotoFile(context)
            tempCameraFile = file
            cameraLauncher.launch(PhotoUtils.getUriForFile(context, file))
        }
    }

    val openCamera = {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            val file = PhotoUtils.createTempPhotoFile(context)
            tempCameraFile = file
            cameraLauncher.launch(PhotoUtils.getUriForFile(context, file))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                    viewModel.resetSaveState()
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
                    val formatted = dateFormatter.format(d)
                    if (pickerTarget == "start") {
                        viewModel.formStartDate = formatted
                    } else {
                        viewModel.formEndDate = formatted
                    }
                }
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val state = if (pickerTarget == "start") startTimePickerState else endTimePickerState
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    state.hour,
                    state.minute
                )
                if (pickerTarget == "start") {
                    viewModel.formStartTime = formattedTime
                } else {
                    viewModel.formEndTime = formattedTime
                }
                showTimePicker = false
            }
        ) {
            if (pickerTarget == "start") TimePicker(state = startTimePickerState)
            else TimePicker(state = endTimePickerState)
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text(stringResource(R.string.add_photo)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.camera)) },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPhotoSourceDialog = false
                            openCamera()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.gallery)) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPhotoSourceDialog = false
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        enabled = isSaveEnabled && !isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .semantics { contentDescription = savingText },
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
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
                            onDatePickerClick = { target ->
                                pickerTarget = target
                                showDatePicker = true
                            },
                            onTimePickerClick = { target ->
                                pickerTarget = target
                                showTimePicker = true
                            },
                            onPhotoPickerClick = {
                                if (viewModel.formPhotos.size < MaxPhotos) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showPhotoSourceDialog = true
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
                    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.Unauthenticated)
                    override suspend fun login(email: String, password: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun register(email: String, password: String, name: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun logout() {}
                    override fun currentUser(): User? = null
                    override suspend fun loadSession() {}
                    override suspend fun updateProfile(name: String, imageUri: String?): Result<User> = Result.failure(Exception("mock"))
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

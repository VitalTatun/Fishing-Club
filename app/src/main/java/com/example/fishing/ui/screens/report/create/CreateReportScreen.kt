package com.example.fishing.ui.screens.report.create

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.widthIn
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
import com.example.fishing.ui.components.FishingListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
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
import com.example.fishing.data.FishCatalogRepository
import com.example.fishing.data.MockFishingRepository
import com.example.fishing.data.upload.ReportUploadQueue
import com.example.fishing.data.upload.ReportUploadResult
import com.example.fishing.model.*
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.utils.PhotoUtils
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.ReportPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
    onNavigateToFishingDateTime: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
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
    var showSavingDialog by remember { mutableStateOf(false) }
    var isTakingTooLong by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) {
        showSavingDialog = isSaving
        isTakingTooLong = false
        if (isSaving) {
            delay(20_000)
            isTakingTooLong = true
        }
    }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var isDetailsExpanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val restoredPhotosLost = viewModel.restoredPhotosLost
    val restoredPhotosLostMessage = stringResource(R.string.restored_photos_lost, restoredPhotosLost)
    LaunchedEffect(restoredPhotosLost) {
        if (restoredPhotosLost > 0) {
            // Show before consuming: consuming resets the effect key and would
            // cancel the suspended showSnackbar call.
            snackbarHostState.showSnackbar(restoredPhotosLostMessage)
            viewModel.consumeRestoredPhotosLost()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MaxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // TODO(AUD-06, manual check): a re-delivered picker result must not
            // produce a duplicate snackbar on a later restore.
            val currentPhotos = viewModel.formPhotos
            val currentUris = currentPhotos.map { it.uri }
            val availableSlots = MaxPhotos - currentPhotos.size
            val newUris = uris.take(availableSlots).filter { it !in currentUris }
            // Best effort persistence of gallery grants happens off the main
            // thread inside the ViewModel.
            viewModel.retainPickedPhotoAccess(newUris)
            viewModel.formPhotos = currentPhotos + newUris.map { ReportPhoto(uri = it) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = PhotoUtils.createTempPhotoFile(context)
            viewModel.beginCameraCapture(file)
            cameraLauncher.launch(PhotoUtils.getUriForFile(context, file))
        }
    }

    val openCamera = {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            val file = PhotoUtils.createTempPhotoFile(context)
            viewModel.beginCameraCapture(file)
            cameraLauncher.launch(PhotoUtils.getUriForFile(context, file))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val handleBack = {
        when {
            isSaving -> showSavingDialog = true
            formHasData -> showDiscardDialog = true
            else -> onBackClick()
        }
    }

    BackHandler(enabled = isSaving || formHasData) {
        if (isSaving) showSavingDialog = true else showDiscardDialog = true
    }

    if (showSavingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.save_in_progress_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.save_in_progress_message))
                    }
                    if (isTakingTooLong) {
                        Text(
                            text = stringResource(R.string.save_taking_too_long),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSavingDialog = false
                    viewModel.cancelSave()
                }) {
                    Text(stringResource(R.string.cancel_saving))
                }
            }
        )
    }

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

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text(stringResource(R.string.add_photo)) },
            text = {
                Column {
                    FishingListItem(
                        title = stringResource(R.string.camera),
                        leadingIcon = Icons.Default.CameraAlt,
                        onRowClick = {
                            showPhotoSourceDialog = false
                            openCamera()
                        }
                    )
                    FishingListItem(
                        title = stringResource(R.string.gallery),
                        leadingIcon = Icons.Default.PhotoLibrary,
                        onRowClick = {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = ContentWidths.Form)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(formConfig) { section ->
                    SectionGroup(showDivider = section.id != "type") {
                        section.items.forEach { field ->
                            ReportFieldRenderer(
                                field = field,
                                viewModel = viewModel,
                                onNavigateToWaterEdit = onNavigateToWaterEdit,
                                onNavigateToWaterNameEdit = onNavigateToWaterNameEdit,
                                onNavigateToMethodAndBaitEdit = onNavigateToMethodAndBaitEdit,
                                onNavigateToCatchEdit = onNavigateToCatchEdit,
                                onNavigateToCommentEdit = onNavigateToCommentEdit,
                                onNavigateToFishingDateTime = onNavigateToFishingDateTime,
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

            if (isSaving) {
                // Swallow taps so the form cannot be edited while saving is in flight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
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
                fishCatalogRepository = PreviewFishCatalogRepository(),
                authRepository = object : AuthRepository {
                    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.Unauthenticated)
                    override suspend fun login(email: String, password: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun register(email: String, password: String, name: String) = Result.failure<User>(Exception("mock"))
                    override suspend fun logout() {}
                    override fun currentUser(): User? = null
                    override suspend fun loadSession() {}
                    override suspend fun updateProfile(name: String, imageUri: String?): Result<User> = Result.failure(Exception("mock"))
                    override suspend fun changePassword(newPassword: String, currentPassword: String): Result<Unit> = Result.failure(Exception("mock"))
                    override suspend fun deleteAccount(): Result<Unit> = Result.failure(Exception("mock"))
                    override fun resolveImageUrl(path: String): String = ""
                },
                reportUploadQueue = PreviewReportUploadQueue(),
                context = context,
                ioDispatcher = Dispatchers.IO
            )
        }
        CreateReportScreen(
            viewModel = viewModel,
            onBackClick = {},
            onSaveComplete = {}
        )
    }
}

/** No-op queue so the preview can build the ViewModel without Room/WorkManager. */
private class PreviewReportUploadQueue : ReportUploadQueue {
    override fun observePending(): Flow<List<PendingReportUpload>> = flowOf(emptyList())
    override fun observeResults(): SharedFlow<ReportUploadResult> = MutableSharedFlow()
    override suspend fun enqueue(
        report: FishingReport,
        localPhotoPaths: List<String>,
        temporaryPhotoPaths: List<String>
    ): Result<Unit> = Result.success(Unit)
    override suspend fun cancel(reportId: UUID) {}
    override suspend fun retry(reportId: UUID) {}
    override suspend fun resumePending() {}
    override suspend fun cleanupStale(nowMillis: Long) {}
    override suspend fun clearAll() {}
    override suspend fun referencedLocalPaths(): Set<String> = emptySet()
}

/** Offline catalog for the preview; the real one is seeded from assets and Room. */
private class PreviewFishCatalogRepository : FishCatalogRepository {
    private val species = MutableStateFlow(
        listOf(
            FishSpecies("rutilus_rutilus", "Плотва", emptyList(), isPopular = true, sortOrder = 1),
            FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3)
        )
    )

    override fun observeAll(): Flow<List<FishSpecies>> = species

    override fun observePopular(): Flow<List<FishSpecies>> = species

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}

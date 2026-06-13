package com.example.fishing.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onSaveClick: (
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
        photos: List<String>
    ) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCatchEdit: () -> Unit = {},
    onNavigateToMethodAndBaitEdit: () -> Unit = {},
    onNavigateToCommentEdit: () -> Unit = {},
    onNavigateToWaterEdit: () -> Unit = {},
) {
    val calendar = remember { Calendar.getInstance() }
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

    val isSaveEnabled by remember {
        derivedStateOf {
            viewModel.formTitle.isNotBlank() &&
                    viewModel.formWaterName.isNotBlank() &&
                    viewModel.formLocation != null &&
                    viewModel.formSelectedMethod != FishingMethod.NONE &&
                    viewModel.formSelectedFish.isNotEmpty()
        }
    }

    val formHasData by remember {
        derivedStateOf {
            viewModel.formTitle.isNotBlank() ||
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
            title = { Text("Отмена") },
            text = { Text("Введённые данные не сохранятся. Продолжить?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.resetFormState()
                    onBackClick()
                }) {
                    Text("Закрыть")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Остаться")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CreateReportColors.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Новый отчет",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val combinedDateTime = Calendar.getInstance().apply {
                                time = dateFormatter.parse(viewModel.formFishingDate) ?: Date()
                                val timeParts = viewModel.formFishingStartTime.split(":")
                                if (timeParts.size == 2) {
                                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                                    set(Calendar.MINUTE, timeParts[1].toInt())
                                }
                            }.time

                            onSaveClick(
                                viewModel.formTitle, viewModel.formReportType, viewModel.formWaterName,
                                viewModel.formLocation, combinedDateTime,
                                viewModel.formWeight.toDouble(), viewModel.formSelectedFish,
                                viewModel.formSelectedMethod, viewModel.formSelectedBaits,
                                viewModel.formComment, viewModel.formFishingFromShore,
                                viewModel.formIsPublic, viewModel.formSelectedPhotoUris.map { it.toString() }
                            )
                        },
                        enabled = isSaveEnabled
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreateReportColors.Surface,
                    titleContentColor = CreateReportColors.OnSurface,
                    navigationIconContentColor = CreateReportColors.OnSurface,
                    actionIconContentColor = CreateReportColors.OnSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ReportHeaderSection(
                    title = viewModel.formTitle,
                    onTitleChange = { viewModel.formTitle = it },
                    reportType = viewModel.formReportType,
                    onReportTypeChange = { viewModel.formReportType = it },
                )
            }
            item {
                PhotosSection(
                    selectedPhotoUris = viewModel.formSelectedPhotoUris,
                    onPhotosChange = { viewModel.formSelectedPhotoUris = it }
                )
            }
            item {
                WaterSection(
                    waterName = viewModel.formWaterName,
                    onWaterNameChange = { viewModel.formWaterName = it },
                    onArrowClick = onNavigateToWaterEdit,
                    location = viewModel.formLocation
                )
            }
            item {
                MethodAndBaitSection(
                    selectedMethod = viewModel.formSelectedMethod,
                    selectedBaits = viewModel.formSelectedBaits,
                    onArrowClick = onNavigateToMethodAndBaitEdit
                )
            }
            item {
                GeneralInfoSection(
                    fishingDate = viewModel.formFishingDate,
                    onFishingDateChange = { viewModel.formFishingDate = it },
                    fishingStartTime = viewModel.formFishingStartTime,
                    onFishingStartTimeChange = { viewModel.formFishingStartTime = it },
                    fishingFromShore = viewModel.formFishingFromShore,
                    onFishingFromShoreChange = { viewModel.formFishingFromShore = it },
                    isPublic = viewModel.formIsPublic,
                    onPublicChange = { viewModel.formIsPublic = it },
                    isPaidWater = viewModel.formIsPaidWater,
                    onPaidWaterChange = { viewModel.formIsPaidWater = it },
                    weight = viewModel.formWeight,
                    onWeightChange = { viewModel.formWeight = it }
                )
            }
            item {
                CatchSection(
                    selectedFish = viewModel.formSelectedFish,
                    onArrowClick = onNavigateToCatchEdit
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
                    text = "*Обязательные поля",
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
    FishingTheme(darkTheme = false, dynamicColor = false) {
        CreateReportScreen(
            viewModel = MainViewModel(
                repository = com.example.fishing.data.MockFishingRepository()
            ),
            onBackClick = {},
            onSaveClick = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Bait
import com.example.fishing.model.FishingMethod
import com.example.fishing.ui.theme.FishingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialMethod: FishingMethod = FishingMethod.NONE,
    initialBaits: List<Bait> = emptyList(),
    onNavigateToCatchEdit: () -> Unit = {},
    onNavigateToMethodAndBaitEdit: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("Отчет") }
    var waterName by remember { mutableStateOf("Озеро в деревне Вулька 2") }
    var fishingDate by remember { mutableStateOf("6 июн. 2026") }
    var fishingStartTime by remember { mutableStateOf("5:00") }
    var fishingFromShore by remember { mutableStateOf(true) }
    var isPublic by remember { mutableStateOf(true) }
    var isPaidWater by remember { mutableStateOf(false) }
    var weight by remember { mutableFloatStateOf(0f) }
    var selectedMethod by remember(initialMethod) { mutableStateOf(initialMethod) }
    var selectedBaits by remember(initialBaits) { mutableStateOf(initialBaits) }

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
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onSaveClick, enabled = title.isNotBlank()) {
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
                    title = title,
                    onTitleChange = { title = it },
                    reportType = reportType,
                    onReportTypeChange = { reportType = it }
                )
            }
            item { PhotosSection() }
            item {
                WaterSection(
                    waterName = waterName,
                    onWaterNameChange = { waterName = it }
                )
            }
            item {
                MethodAndBaitSection(
                    selectedMethod = selectedMethod,
                    selectedBaits = selectedBaits,
                    onArrowClick = onNavigateToMethodAndBaitEdit
                )
            }
            item {
                GeneralInfoSection(
                    fishingDate = fishingDate,
                    onFishingDateChange = { fishingDate = it },
                    fishingStartTime = fishingStartTime,
                    onFishingStartTimeChange = { fishingStartTime = it },
                    fishingFromShore = fishingFromShore,
                    onFishingFromShoreChange = { fishingFromShore = it },
                    isPublic = isPublic,
                    onPublicChange = { isPublic = it },
                    isPaidWater = isPaidWater,
                    onPaidWaterChange = { isPaidWater = it },
                    weight = weight,
                    onWeightChange = { weight = it }
                )
            }
            item { CatchSection(onArrowClick = onNavigateToCatchEdit) }
            item { CommentSection() }
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
            onBackClick = {},
            onSaveClick = {}
        )
    }
}

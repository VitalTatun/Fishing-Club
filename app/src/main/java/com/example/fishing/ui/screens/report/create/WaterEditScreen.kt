package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.R
import com.example.fishing.ui.theme.FishingTheme

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterEditScreen(
    viewModel: CreateReportViewModel,
    reportsViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var waterName by remember { mutableStateOf(viewModel.formWaterName) }
    var isPaidWater by remember { mutableStateOf(viewModel.formIsPaidWater) }
    var isFishingFromShore by remember { mutableStateOf(viewModel.formFishingFromShore) }
    
    val reports by reportsViewModel.reports.collectAsState()
    val previousPlaces = remember(reports) {
        reports.asSequence()
            .map { it.water.waterName }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase().replace("\\s+".toRegex(), "") } // Максимально жесткая проверка на уникальность
            .toList()
            .sortedBy { it }
    }

    LaunchedEffect(Unit) {
        reportsViewModel.loadReportsIfNeeded()
    }

    var showDiscardDialog by remember { mutableStateOf(value = false) }

    val hasChanges = ((waterName != viewModel.formWaterName) || 
                     (isPaidWater != viewModel.formIsPaidWater) || 
                     (isFishingFromShore != viewModel.formFishingFromShore))

    val haptic = LocalHapticFeedback.current

    BackHandler {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_water_data_confirm)) },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.stay))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.water_body)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) showDiscardDialog = true else onBackClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.formWaterName = waterName
                            viewModel.formIsPaidWater = isPaidWater
                            viewModel.formFishingFromShore = isFishingFromShore
                            onSaveClick()
                        },
                        enabled = waterName.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Блок 1: Поле ввода названия и подсказка
            Column(modifier = Modifier.fillMaxWidth()) {
                ReportTextField(
                    value = waterName,
                    onValueChange = { waterName = it },
                    label = stringResource(R.string.water_name_placeholder),
                    supportingText = stringResource(R.string.water_name_supporting_text)
                )
            }

            // Блок 2: Переключатели (Платный водоем и Ловля с лодки)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                WaterSwitchRow(
                    title = stringResource(R.string.paid_water),
                    checked = isPaidWater,
                ) { isPaidWater = it }
                WaterSwitchRow(
                    title = stringResource(R.string.fishing_from_shore),
                    checked = isFishingFromShore,
                ) { isFishingFromShore = it }
            }

            // Блок 3: Список предыдущих мест
            if (previousPlaces.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.previously_fished_places),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Column(Modifier.selectableGroup()) {
                        previousPlaces.forEach { place ->
                            val isSelected = waterName == place
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        waterName = place
                                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    }
                                )
                                Text(
                                    text = place,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterEditScreenPreview() {
    // Невозможно легко создать ViewModel для превью без Hilt/DI в превью, 
    // поэтому в реальном приложении здесь был бы мок или пустая заглушка.
}

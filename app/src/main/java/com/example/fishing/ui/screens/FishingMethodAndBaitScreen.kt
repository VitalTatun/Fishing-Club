package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Bait
import com.example.fishing.model.FishingMethod
import com.example.fishing.ui.theme.FishingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingMethodAndBaitScreen(
    onBackClick: () -> Unit,
    onSaveClick: (FishingMethod, List<Bait>) -> Unit,
    modifier: Modifier = Modifier,
    initialMethod: FishingMethod = FishingMethod.NONE,
    initialBaits: List<Bait> = emptyList()
) {
    var selectedMethod by remember { mutableStateOf(initialMethod) }
    val selectedBaits = remember { mutableStateListOf<Bait>().apply { addAll(initialBaits) } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Способ ловли и наживка",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isSaveEnabled = selectedMethod != FishingMethod.NONE && selectedBaits.isNotEmpty()
                    IconButton(
                        onClick = { onSaveClick(selectedMethod, selectedBaits.toList()) },
                        enabled = isSaveEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Сохранить",
                            tint = if (isSaveEnabled) Color.Black else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportDropdownField(
                value = selectedMethod.russianName,
                onValueChange = { name ->
                    val newMethod = FishingMethod.entries.find { it.russianName == name } ?: FishingMethod.NONE
                    if (newMethod != selectedMethod) {
                        selectedMethod = newMethod
                        selectedBaits.clear()
                    }
                },
                label = "Способ ловли",
                options = FishingMethod.entries.map { it.russianName }
            )

            if (selectedMethod == FishingMethod.NONE) {
                Text(
                    text = "Чтобы появился выбор наживок, сначала выберите способ ловли",
                    style = MaterialTheme.typography.bodySmall,
                    color = CreateReportColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    val availableBaits = FishingMethod.methodsAndBaits[selectedMethod] ?: emptyList()
                    availableBaits.forEach { bait ->
                        val isSelected = selectedBaits.contains(bait)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedBaits.add(bait)
                                    else selectedBaits.remove(bait)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Text(
                                text = bait.russianName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FishingMethodAndBaitScreenPreview() {
    FishingTheme {
        FishingMethodAndBaitScreen(
            onBackClick = {},
            onSaveClick = { _, _ -> },
            initialMethod = FishingMethod.SPINNING,
            initialBaits = listOf(Bait.SPOONBAIT, Bait.WOBBLER)
        )
    }
}

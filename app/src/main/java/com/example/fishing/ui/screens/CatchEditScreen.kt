package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.model.Fish
import com.example.fishing.ui.theme.FishingTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatchEditScreen(
    fishList: List<Fish>,
    onBackClick: () -> Unit,
    onSaveClick: (List<Fish>) -> Unit,
    modifier: Modifier = Modifier
) {
    var fishNameInput by remember { mutableStateOf("") }
    var isDuplicateError by remember { mutableStateOf(false) }
    val editableFish = remember(fishList) {
        mutableStateListOf<Fish>().also { it.addAll(fishList) }
    }

    fun toggleFishSelection(name: String) {
        val existingIndex = editableFish.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (existingIndex != -1) {
            editableFish.removeAt(existingIndex)
        } else {
            editableFish.add(0, Fish(name = name, count = 1))
        }
    }

    fun addFishFromInput() {
        if (fishNameInput.isNotBlank()) {
            val exists = editableFish.any { it.name.equals(fishNameInput, ignoreCase = true) }
            if (!exists) {
                editableFish.add(0, Fish(name = fishNameInput, count = 1))
                fishNameInput = ""
                isDuplicateError = false
            } else {
                isDuplicateError = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Пойманная рыба",
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
                    IconButton(
                        onClick = { onSaveClick(editableFish.toList()) },
                        enabled = editableFish.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fishNameInput,
                onValueChange = {
                    fishNameInput = it
                    isDuplicateError = false
                },
                label = { Text("Название рыбы") },
                singleLine = true,
                isError = isDuplicateError,
                supportingText = if (isDuplicateError) {
                    { Text("Уже есть в списке") }
                } else null,
                trailingIcon = {
                    IconButton(
                        onClick = { addFishFromInput() },
                        enabled = fishNameInput.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить",
                            tint = if (fishNameInput.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Рыба, которую вы уже указывали в отчетах",
                    color = CreateReportColors.OnSurface,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val chipLabels = listOf("Плотва", "Карась", "Щука", "Лещ", "Окунь", "Подлещик", "Язь")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    chipLabels.forEach { label ->
                        val isSelected = editableFish.any { it.name.equals(label, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { toggleFishSelection(label) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                editableFish.forEachIndexed { index, fish ->
                    FishQuantityRow(
                        name = fish.name,
                        count = fish.count,
                        onDecrement = {
                            if (fish.count > 1) {
                                editableFish[index] = fish.copy(count = fish.count - 1)
                            } else {
                                editableFish.removeAt(index)
                            }
                        },
                        onIncrement = {
                            editableFish[index] = fish.copy(count = fish.count + 1)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FishQuantityRow(
    name: String,
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = CreateReportColors.OnSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = count.toString(),
            color = CreateReportColors.OnSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(42.dp)
        )

        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val isMinCount = count <= 1
            FilledTonalIconButton(
                onClick = onDecrement,
                modifier = Modifier.size(52.dp, 40.dp),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isMinCount) Color(0xFFFFDAD6) else CreateReportColors.SecondaryContainer,
                    contentColor = if (isMinCount) Color(0xFF93000A) else CreateReportColors.OnSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isMinCount) Icons.Default.Delete else Icons.Default.Remove,
                    contentDescription = if (isMinCount) "Удалить" else "Уменьшить"
                )
            }

            FilledTonalIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(52.dp, 40.dp),
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = CreateReportColors.SecondaryContainer,
                    contentColor = CreateReportColors.OnSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Увеличить"
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CatchEditScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        CatchEditScreen(
            fishList = listOf(
                Fish(name = "Карась", count = 5),
                Fish(name = "Окунь", count = 1),
                Fish(name = "Плотва", count = 4)
            ),
            onBackClick = {},
            onSaveClick = { }
        )
    }
}

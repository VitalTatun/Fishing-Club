package com.example.fishing.ui.screens.report.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.R
import com.example.fishing.model.Fish
import com.example.fishing.model.FishSuggestion
import com.example.fishing.model.FishSuggestionSource
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.QuantityStepper
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CatchSearchState
import java.util.UUID

private const val MAX_TOTAL_WEIGHT_KG = 70f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatchEditScreen(
    fishList: List<Fish>,
    searchState: CatchSearchState,
    isAlreadyAdded: (name: String, speciesId: String?) -> Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddFish: (name: String, speciesId: String?) -> Unit,
    onAddCustomFish: (name: String) -> Unit,
    onToggleSuggestion: (name: String, speciesId: String?) -> Unit,
    onIncrementFish: (UUID) -> Unit,
    onDecrementFish: (UUID) -> Unit,
    onRemoveFish: (UUID) -> Unit,
    onWeightChange: (Float) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: (List<Fish>, Float) -> Unit,
    modifier: Modifier = Modifier,
    initialWeight: Float = 0f,
    isTrophy: Boolean = false
) {
    var weightInput by rememberSaveable {
        mutableStateOf(
            initialWeight
                .coerceAtMost(MAX_TOTAL_WEIGHT_KG)
                .takeIf { initialWeight > 0f }
                ?.toString()
                ?: ""
        )
    }

    val enteredWeight = weightInput.toFloatOrNull()
    val isWeightTooHigh = enteredWeight != null && enteredWeight > MAX_TOTAL_WEIGHT_KG

    if (searchState.isOpen) {
        BackHandler(onBack = onCloseSearch)
        FishSearchView(
            query = searchState.query,
            onQueryChange = onSearchQueryChange,
            recent = searchState.recent,
            popular = searchState.popular,
            results = searchState.results,
            isAlreadyAdded = isAlreadyAdded,
            onBack = onCloseSearch,
            onSelect = onAddFish,
            onAddCustom = { onAddCustomFish(searchState.query) },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    FishSearchBar(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSaveClick(fishList, enteredWeight ?: 0f) },
                        enabled = fishList.isNotEmpty() && !isWeightTooHigh
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = ContentWidths.Form)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchState.chips.isNotEmpty()) {
                    SectionGroup(title = stringResource(R.string.fish_suggestion_title), showDivider = false) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            searchState.chips.forEach { suggestion ->
                                val selected = isAlreadyAdded(suggestion.name, suggestion.speciesId)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onToggleSuggestion(suggestion.name, suggestion.speciesId)
                                    },
                                    label = {
                                        Text(
                                            text = suggestion.name,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("fish_chip_${suggestion.name}")
                                )
                            }
                        }
                    }
                }

                if (isTrophy) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.trophy_one_fish_info),
                            color = CreateReportColors.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,1}$"))) {
                            if ((newValue.toFloatOrNull() ?: 0f) <= MAX_TOTAL_WEIGHT_KG) {
                                weightInput = newValue
                                onWeightChange(newValue.toFloatOrNull() ?: 0f)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.total_weight)) },
                    suffix = { Text(stringResource(R.string.kg)) },
                    singleLine = true,
                    isError = isWeightTooHigh,
                    supportingText = if (isWeightTooHigh) {
                        {
                            Text(
                                stringResource(
                                    R.string.weight_limit_error,
                                    MAX_TOTAL_WEIGHT_KG.formatMaxWeight(),
                                    stringResource(R.string.kg)
                                )
                            )
                        }
                    } else {
                        {
                            Text(
                                stringResource(
                                    R.string.weight_limit_hint,
                                    MAX_TOTAL_WEIGHT_KG.formatMaxWeight(),
                                    stringResource(R.string.kg)
                                )
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                if (fishList.isNotEmpty()) {
                    SectionGroup(title = stringResource(R.string.fish_added_section), showDivider = false) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            fishList.forEach { fish ->
                                FishQuantityRow(
                                    name = fish.name,
                                    count = fish.count,
                                    onDecrement = { onDecrementFish(fish.id) },
                                    onIncrement = { onIncrementFish(fish.id) },
                                    onRemove = { onRemoveFish(fish.id) },
                                    isMaxCount = isTrophy && fish.count >= 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Float.formatMaxWeight(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

@Composable
private fun FishQuantityRow(
    name: String,
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit,
    isMaxCount: Boolean = false
) {
    FishingListItem(
        title = name,
        trailingText = stringResource(R.string.fish_count_short, count),
        trailingContent = {
            QuantityStepper(
                count = count,
                onDecrement = onDecrement,
                onIncrement = onIncrement,
                onRemove = onRemove,
                isIncrementEnabled = !isMaxCount
            )
        },
        contentPaddingEnd = 8.dp
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CatchEditScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        CatchEditScreen(
            fishList = listOf(
                Fish(id = UUID.randomUUID(), name = "Карась", count = 5, speciesId = "carassius_gibelio"),
                Fish(id = UUID.randomUUID(), name = "Окунь", count = 1, speciesId = "perca_fluviatilis"),
                Fish(id = UUID.randomUUID(), name = "Плотва", count = 4, speciesId = "rutilus_rutilus")
            ),
            searchState = CatchSearchState(
                chips = listOf(
                    FishSuggestion("Плотва", "rutilus_rutilus", FishSuggestionSource.HISTORY),
                    FishSuggestion("Карась", "carassius_gibelio", FishSuggestionSource.HISTORY),
                    FishSuggestion("Щука", "esox_lucius", FishSuggestionSource.HISTORY),
                    FishSuggestion("Лещ", "abramis_brama", FishSuggestionSource.HISTORY),
                    FishSuggestion("Окунь", "perca_fluviatilis", FishSuggestionSource.HISTORY)
                )
            ),
            isAlreadyAdded = { name, _ -> name == "Карась" || name == "Окунь" },
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchQueryChange = {},
            onAddFish = { _, _ -> },
            onAddCustomFish = {},
            onToggleSuggestion = { _, _ -> },
            onIncrementFish = {},
            onDecrementFish = {},
            onRemoveFish = {},
            onWeightChange = {},
            onBackClick = {},
            onSaveClick = { _, _ -> },
            initialWeight = 2.5f
        )
    }
}

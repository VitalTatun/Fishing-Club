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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.Fish
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishSuggestion
import com.example.fishing.model.FishSuggestionSource
import com.example.fishing.model.normalizeFishName
import com.example.fishing.model.resolveFishSpecies
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.QuantityStepper
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CatchSearchState
import java.util.UUID

private const val MAX_TOTAL_WEIGHT_KG = 70f

/**
 * Catch form with a local draft: fish and weight are only committed through
 * the checkmark. Back (app bar or system) leaves the screen and discards the
 * edits, matching the rest of the create-report sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatchEditScreen(
    fishList: List<Fish>,
    catalog: List<FishSpecies>,
    searchState: CatchSearchState,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: (List<Fish>, Float) -> Unit,
    modifier: Modifier = Modifier,
    initialWeight: Float = 0f,
    isTrophy: Boolean = false
) {
    val editableFish = rememberSaveable(fishList, saver = FishListSaver) {
        mutableStateListOf<Fish>().also { it.addAll(fishList) }
    }
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

    fun fishIdentity(name: String, speciesId: String?): String {
        val trimmedName = name.trim()
        val resolvedId = speciesId ?: resolveFishSpecies(trimmedName, catalog)?.id
        return resolvedId?.let { "id:$it" } ?: "name:${normalizeFishName(trimmedName)}"
    }

    fun isAlreadyAdded(name: String, speciesId: String?): Boolean {
        val identity = fishIdentity(name, speciesId)
        return editableFish.any { fishIdentity(it.name, it.speciesId) == identity }
    }

    fun addFish(name: String, speciesId: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val resolved = resolveFishSpecies(trimmedName, catalog)
        val resolvedId = speciesId ?: resolved?.id
        val displayName = resolved?.canonicalName ?: trimmedName
        if (isAlreadyAdded(displayName, resolvedId)) return

        val fish = Fish(
            id = UUID.randomUUID(),
            name = displayName,
            count = 1,
            speciesId = resolvedId
        )
        if (isTrophy) {
            editableFish.clear()
        }
        editableFish.add(0, fish)
        onCloseSearch()
    }

    fun toggleFish(name: String, speciesId: String?) {
        val identity = fishIdentity(name, speciesId)
        val existing = editableFish.firstOrNull {
            fishIdentity(it.name, it.speciesId) == identity
        }
        if (existing != null) {
            editableFish.remove(existing)
        } else {
            addFish(name, speciesId)
        }
    }

    if (searchState.isOpen) {
        BackHandler(onBack = onCloseSearch)
        FishSearchView(
            query = searchState.query,
            onQueryChange = onSearchQueryChange,
            recent = searchState.recent,
            popular = searchState.popular,
            results = searchState.results,
            isAlreadyAdded = { name, speciesId -> isAlreadyAdded(name, speciesId) },
            onBack = onCloseSearch,
            onSelect = { name, speciesId -> addFish(name, speciesId) },
            onAddCustom = { addFish(searchState.query, speciesId = null) },
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
                        onClick = { onSaveClick(editableFish.toList(), enteredWeight ?: 0f) },
                        enabled = editableFish.isNotEmpty() && !isWeightTooHigh
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
                    SectionGroup(
                        title = stringResource(R.string.fish_suggestion_title),
                        showDivider = false
                    ) {
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
                                        toggleFish(suggestion.name, suggestion.speciesId)
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

                if (editableFish.isNotEmpty()) {
                    SectionGroup(
                        title = stringResource(R.string.fish_added_section),
                        showDivider = false
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                        if (!isTrophy) {
                                            editableFish[index] = fish.copy(count = fish.count + 1)
                                        }
                                    },
                                    onRemove = { editableFish.removeAt(index) },
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
            catalog = listOf(
                FishSpecies("rutilus_rutilus", "Плотва", emptyList(), isPopular = true, sortOrder = 1),
                FishSpecies("carassius_gibelio", "Карась", emptyList(), isPopular = true, sortOrder = 2),
                FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3),
                FishSpecies("abramis_brama", "Лещ", listOf("подлещик"), isPopular = true, sortOrder = 4),
                FishSpecies("perca_fluviatilis", "Окунь", emptyList(), isPopular = true, sortOrder = 5)
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
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchQueryChange = {},
            onBackClick = {},
            onSaveClick = { _, _ -> },
            initialWeight = 2.5f
        )
    }
}

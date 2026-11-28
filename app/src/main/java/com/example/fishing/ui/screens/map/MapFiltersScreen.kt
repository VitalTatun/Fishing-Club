package com.example.fishing.ui.screens.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fishing.R
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.SelectedFish
import com.example.fishing.model.searchFishSpecies
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.filter.ReportFilters

/**
 * Map filter screen: a draft is edited locally and only reaches the map when
 * the check action is pressed. Back discards the draft.
 */
@Composable
fun MapFiltersScreen(
    initialFilters: ReportFilters,
    onApply: (ReportFilters) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapFiltersViewModel = hiltViewModel()
) {
    val species by viewModel.species.collectAsStateWithLifecycle()
    MapFiltersContent(
        initialFilters = initialFilters,
        species = species,
        onApply = onApply,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapFiltersContent(
    initialFilters: ReportFilters,
    species: List<FishSpecies>,
    onApply: (ReportFilters) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(initialFilters) { mutableStateOf(initialFilters) }
    val hasChanges = draft != initialFilters

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_filters_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onApply(draft) },
                        enabled = hasChanges,
                        modifier = Modifier.testTag("map_filters_apply")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.map_filters_apply)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidths.Form)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                SectionGroup(showDivider = true) {
                    FishFilterField(
                        species = species,
                        selectedFish = draft.selectedFish,
                        onToggle = { fish ->
                            val exists = draft.selectedFish.any { it.sameAs(fish) }
                            val next = when {
                                exists -> draft.selectedFish.filterNot { it.sameAs(fish) }
                                draft.selectedFish.size >= MAX_SELECTED_FISH ->
                                    draft.selectedFish
                                else -> draft.selectedFish + fish
                            }
                            draft = draft.copy(selectedFish = next)
                        },
                        onClear = { draft = draft.copy(selectedFish = emptyList()) }
                    )
                }

                SectionGroup(
                    title = stringResource(R.string.map_filters_section_misc),
                    showDivider = false
                ) {
                    FilterChipRow {
                        FilterChip(
                            selected = draft.isOwnOnly,
                            onClick = { draft = draft.copy(isOwnOnly = !draft.isOwnOnly) },
                            label = { Text(stringResource(R.string.map_filter_own)) }
                        )
                        FilterChip(
                            selected = draft.isTrophySelected,
                            onClick = {
                                draft = draft.copy(isTrophySelected = !draft.isTrophySelected)
                            },
                            label = { Text(stringResource(R.string.trophy)) }
                        )
                        FilterChip(
                            selected = draft.isPaidSelected,
                            onClick = {
                                draft = draft.copy(isPaidSelected = !draft.isPaidSelected)
                            },
                            label = { Text(stringResource(R.string.paid_water)) }
                        )
                    }
                }

                SectionGroup(
                    title = stringResource(R.string.fishing_method),
                    showDivider = false
                ) {
                    FilterChipRow {
                        METHOD_ORDER.forEach { method ->
                            FilterChip(
                                selected = draft.selectedMethod == method,
                                onClick = {
                                    draft = draft.copy(
                                        selectedMethod = if (draft.selectedMethod == method) {
                                            null
                                        } else {
                                            method
                                        }
                                    )
                                },
                                label = { Text(stringResource(method.labelRes)) }
                            )
                        }
                    }
                }

                if (draft.isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { draft = ReportFilters() },
                            modifier = Modifier.testTag("map_filters_reset")
                        ) {
                            Text(stringResource(R.string.map_filters_reset))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

}

private const val MAX_SELECTED_FISH = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishFilterField(
    species: List<FishSpecies>,
    selectedFish: List<SelectedFish>,
    onToggle: (SelectedFish) -> Unit,
    onClear: () -> Unit
) {
    val searchLabel = stringResource(R.string.search)
    val limitReached = selectedFish.size >= MAX_SELECTED_FISH
    val fieldShape = RoundedCornerShape(8.dp)
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val results = remember(query, species) {
        if (query.isBlank()) species else searchFishSpecies(query, species)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = { text ->
                query = text
                expanded = true
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        query.isEmpty() &&
                        selectedFish.isNotEmpty()
                    ) {
                        onToggle(selectedFish.last())
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(fieldShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = fieldShape
                        )
                        .horizontalScroll(scrollState)
                        .testTag("map_filter_fish_field")
                        .clickable { focusRequester.requestFocus() }
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Поиск" is always visible in both states, as in the mock.
                    Text(
                        text = searchLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selectedFish.forEach { fish ->
                        InputChip(
                            selected = true,
                            onClick = {
                                onToggle(fish)
                                // The field's menu anchor also sees this tap;
                                // keep the removal from opening the menu.
                                expanded = false
                            },
                            label = { Text(fish.name) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(
                                        R.string.map_filter_fish_remove,
                                        fish.name
                                    ),
                                    modifier = Modifier.size(InputChipDefaults.IconSize)
                                )
                            },
                            modifier = Modifier.testTag(
                                "fish_chip_" + (fish.speciesId ?: fish.name)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier.widthIn(min = 72.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupStandardContainerColor,
            modifier = Modifier.exposedDropdownSize(),
            shape = MenuDefaults.standaloneGroupShape
        ) {
            val optionCount = results.size + 1
            SelectableDropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, optionCount),
                text = { Text(stringResource(R.string.map_filter_fish_all)) },
                selected = selectedFish.isEmpty() && query.isBlank(),
                onClick = {
                    onClear()
                    query = ""
                },
                selectedLeadingIcon = { SelectedCheckIcon() },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
            results.forEachIndexed { index, item ->
                val candidate = SelectedFish(item.canonicalName, item.id)
                SelectableDropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index + 1, optionCount),
                    text = {
                        Text(
                            text = item.canonicalName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    selected = selectedFish.any { it.sameAs(candidate) },
                    enabled = selectedFish.any { it.sameAs(candidate) } || !limitReached,
                    onClick = {
                        onToggle(candidate)
                        query = ""
                    },
                    selectedLeadingIcon = { SelectedCheckIcon() },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
            if (results.isEmpty()) {
                Text(
                    text = stringResource(R.string.map_filter_fish_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (limitReached) {
                Text(
                    text = stringResource(R.string.map_filter_fish_limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedCheckIcon() {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(MenuDefaults.LeadingIconSize)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

private val METHOD_ORDER = listOf(
    FishingMethod.BOBBER,
    FishingMethod.FEEDER,
    FishingMethod.SPINNING,
    FishingMethod.FLY_FISHING
)

@Preview(showBackground = true, widthDp = 412, heightDp = 860)
@Composable
private fun MapFiltersContentWithFishPreview() {
    FishingTheme(dynamicColor = false) {
        MapFiltersContent(
            initialFilters = ReportFilters(
                selectedFish = listOf(
                    SelectedFish("Щука", "esox_lucius"),
                    SelectedFish("Окунь", "perca_fluviatilis")
                )
            ),
            species = listOf(
                FishSpecies("esox_lucius", "Щука", isPopular = true, sortOrder = 1),
                FishSpecies("perca_fluviatilis", "Окунь", isPopular = true, sortOrder = 2),
                FishSpecies("rutilus_rutilus", "Плотва", isPopular = true, sortOrder = 3)
            ),
            onApply = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 860)
@Composable
private fun MapFiltersContentPreview() {
    FishingTheme(dynamicColor = false) {
        MapFiltersContent(
            initialFilters = ReportFilters(),
            species = listOf(
                FishSpecies("esox_lucius", "Щука", isPopular = true, sortOrder = 1),
                FishSpecies("perca_fluviatilis", "Окунь", isPopular = true, sortOrder = 2),
                FishSpecies("rutilus_rutilus", "Плотва", isPopular = true, sortOrder = 3)
            ),
            onApply = {},
            onBack = {}
        )
    }
}

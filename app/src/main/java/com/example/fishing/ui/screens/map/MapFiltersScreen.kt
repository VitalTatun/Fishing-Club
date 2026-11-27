package com.example.fishing.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fishing.R
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishingMethod
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
                        selectedCatch = draft.selectedCatch,
                        selectedSpeciesId = draft.selectedSpeciesId,
                        onSelect = { fish ->
                            draft = draft.copy(
                                selectedCatch = fish?.canonicalName,
                                selectedSpeciesId = fish?.id
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishFilterField(
    species: List<FishSpecies>,
    selectedCatch: String?,
    selectedSpeciesId: String?,
    onSelect: (FishSpecies?) -> Unit
) {
    val label = stringResource(R.string.map_filter_fish_label)
    val emptyValue = stringResource(R.string.map_filter_fish_none)
    var expanded by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(TextFieldValue(selectedCatch ?: emptyValue)) }
    var searchQuery by remember { mutableStateOf("") }

    // Keep the field in sync with the applied value when the menu is closed;
    // while typing the editable text is independent from the selection.
    LaunchedEffect(selectedCatch, selectedSpeciesId) {
        if (!expanded) {
            fieldValue = TextFieldValue(selectedCatch ?: emptyValue)
            searchQuery = ""
        }
    }

    val results = remember(searchQuery, species) {
        if (searchQuery.isBlank()) species else searchFishSpecies(searchQuery, species)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open ->
            val text = selectedCatch ?: emptyValue
            // Select the current value so typing replaces it instead of
            // appending to an already chosen fish name.
            fieldValue = TextFieldValue(
                text = text,
                selection = TextRange(0, text.length)
            )
            searchQuery = ""
            expanded = open
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                if (newValue.text != emptyValue || selectedCatch != null) {
                    fieldValue = newValue
                    searchQuery = newValue.text
                    expanded = true
                    // Editing after a selection starts a new search; the old
                    // species id must not keep filtering the map.
                    if (selectedCatch != null || selectedSpeciesId != null) {
                        onSelect(null)
                    }
                }
            },
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth()
                .testTag("map_filter_fish_field"),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(4.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                fieldValue = TextFieldValue(selectedCatch ?: emptyValue)
                searchQuery = ""
            },
            containerColor = MenuDefaults.groupStandardContainerColor,
            modifier = Modifier.exposedDropdownSize(),
            shape = MenuDefaults.standaloneGroupShape
        ) {
            val optionCount = results.size + 1
            SelectableDropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, optionCount),
                text = { Text(stringResource(R.string.map_filter_fish_all)) },
                selected = searchQuery.isBlank() && selectedCatch == null,
                onClick = {
                    fieldValue = TextFieldValue(emptyValue)
                    searchQuery = ""
                    onSelect(null)
                    expanded = false
                },
                selectedLeadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                    )
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
            results.forEachIndexed { index, item ->
                val selected = item.id == selectedSpeciesId ||
                    (selectedSpeciesId == null && item.canonicalName == selectedCatch)
                SelectableDropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index + 1, optionCount),
                    text = {
                        Text(
                            text = item.canonicalName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    selected = selected,
                    onClick = {
                        fieldValue = TextFieldValue(item.canonicalName)
                        searchQuery = ""
                        onSelect(item)
                        expanded = false
                    },
                    selectedLeadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                        )
                    },
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
        }
    }
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

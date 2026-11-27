package com.example.fishing.ui.screens.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fishing.R
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.popularFrom
import com.example.fishing.model.searchFishSpecies
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.SearchInputField
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
    var showFishPicker by rememberSaveable { mutableStateOf(false) }
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
                FishFilterField(
                    selectedCatch = draft.selectedCatch,
                    onClick = { showFishPicker = true }
                )

                HorizontalDivider()

                FilterSection(
                    title = stringResource(R.string.map_filters_section_misc)
                ) {
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
                        onClick = { draft = draft.copy(isPaidSelected = !draft.isPaidSelected) },
                        label = { Text(stringResource(R.string.paid_water)) }
                    )
                }

                FilterSection(
                    title = stringResource(R.string.fishing_method)
                ) {
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

    if (showFishPicker) {
        FishFilterDialog(
            species = species,
            selectedSpeciesId = draft.selectedSpeciesId,
            selectedCatch = draft.selectedCatch,
            onDismiss = { showFishPicker = false },
            onClear = {
                draft = draft.copy(selectedCatch = null, selectedSpeciesId = null)
                showFishPicker = false
            },
            onSelect = { fish ->
                draft = draft.copy(
                    selectedCatch = fish.canonicalName,
                    selectedSpeciesId = fish.id
                )
                showFishPicker = false
            }
        )
    }
}

@Composable
private fun FishFilterField(
    selectedCatch: String?,
    onClick: () -> Unit
) {
    val value = selectedCatch ?: stringResource(R.string.map_filter_fish_none)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.map_filter_fish_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // The overlay makes the whole field open the picker without turning the
        // read-only field into an editable one.
        Box(
            modifier = Modifier
                .matchParentSize()
                .testTag("map_filter_fish_field")
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.map_filter_fish_label),
                    onClick = onClick
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FishFilterDialog(
    species: List<FishSpecies>,
    selectedSpeciesId: String?,
    selectedCatch: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSelect: (FishSpecies) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    val options = remember(query, species) {
        if (query.isBlank()) {
            val popular = popularFrom(species)
            if (popular.isNotEmpty()) {
                popular.mapNotNull { suggestion ->
                    suggestion.speciesId?.let { id ->
                        FishFilterOption(id = id, name = suggestion.name)
                    }
                }
            } else {
                species
                    .sortedBy { it.canonicalName }
                    .map { FishFilterOption(id = it.id, name = it.canonicalName) }
            }
        } else {
            searchFishSpecies(query, species)
                .map { FishFilterOption(id = it.id, name = it.canonicalName) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ContentWidths.Form)
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.map_filter_fish_label),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }

                SearchInputField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.map_filter_fish_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("map_filter_fish_query"),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "all") {
                        FishingListItem(
                            title = stringResource(R.string.map_filter_fish_all),
                            leadingIcon = Icons.Default.Clear,
                            onRowClick = onClear
                        )
                    }
                    items(options, key = { it.id }) { option ->
                        val selected = option.id == selectedSpeciesId ||
                            (selectedSpeciesId == null && option.name == selectedCatch)
                        FishingListItem(
                            title = option.name,
                            leadingIcon = if (selected) Icons.Default.Check else Icons.Default.Search,
                            fontWeight = if (selected) FontWeight.Medium else null,
                            onRowClick = {
                                species.firstOrNull { it.id == option.id }?.let(onSelect)
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class FishFilterOption(
    val id: String,
    val name: String
)

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

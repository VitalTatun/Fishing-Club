package com.example.fishing.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingFilterChips
import com.example.fishing.ui.components.FishingReportCardItem
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.SearchInputField
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.filter.ReportSearchState
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSearchScreen(
    reports: List<FishingReport>,
    favoriteReports: List<FishingReport> = emptyList(),
    state: ReportSearchState,
    onStateChange: ((ReportSearchState) -> ReportSearchState) -> Unit,
    onReportClick: (FishingReport) -> Unit,
    onBack: () -> Unit,
    favoriteError: String? = null,
    onFavoriteErrorDismiss: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(favoriteError) {
        if (favoriteError != null) {
            snackbarHostState.showSnackbar(favoriteError)
            onFavoriteErrorDismiss()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.selectedDateMillis
    )

    val uniqueFish = remember(reports) {
        reports.flatMap { it.fish }.map { it.name }.distinct().sorted()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    onStateChange { it.copy(selectedDateMillis = selectedDateMillis) }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val favoriteIds = remember(favoriteReports) { favoriteReports.map { it.id }.toSet() }

    val baseReports = remember(reports, favoriteReports, state.filters.isFavoritesSelected) {
        if (state.filters.isFavoritesSelected) favoriteReports else reports
    }

    val filteredReports = remember(baseReports, state, favoriteIds) {
        baseReports.filter { report -> state.matches(report, favoriteIds) }
    }

    val showEmptyPlaceholder = state.isEmpty
    val filters = state.filters
    val density = LocalDensity.current
    val navigationBarBottom = if (WindowInsets.ime.getBottom(density) == 0) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ContentWidths.Search)
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    ReportSearchHeader(
                        query = state.query,
                        onQueryChange = { query ->
                            onStateChange { current -> current.copy(query = query) }
                        },
                        onBack = onBack,
                        focusRequester = focusRequester,
                    )
                    FishingFilterChips(
                    isFavoritesSelected = filters.isFavoritesSelected,
                    onFavoritesClick = {
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(
                                    isFavoritesSelected = !current.filters.isFavoritesSelected
                                )
                            )
                        }
                    },
                    isTrophySelected = filters.isTrophySelected,
                    onTrophyClick = {
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(
                                    isTrophySelected = !current.filters.isTrophySelected
                                )
                            )
                        }
                    },
                    isPaidSelected = filters.isPaidSelected,
                    onPaidClick = {
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(
                                    isPaidSelected = !current.filters.isPaidSelected
                                )
                            )
                        }
                    },
                    selectedCatch = filters.selectedFish.firstOrNull()?.name,
                    onCatchSelected = { catch ->
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(
                                    selectedFish = listOf(SelectedFish(catch))
                                )
                            )
                        }
                    },
                    onClearCatch = {
                        onStateChange { current ->
                            current.copy(filters = current.filters.copy(selectedFish = emptyList()))
                        }
                    },
                    selectedMethod = filters.selectedMethods.firstOrNull(),
                    onMethodSelected = { method ->
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(
                                    selectedMethods = setOf(method)
                                )
                            )
                        }
                    },
                    onClearMethod = {
                        onStateChange { current ->
                            current.copy(
                                filters = current.filters.copy(selectedMethods = emptySet())
                            )
                        }
                    },
                    uniqueFish = uniqueFish,
                    selectedDate = state.selectedDateMillis,
                    onDateClick = { showDatePicker = true },
                    onClearDate = {
                        onStateChange { current ->
                            current.copy(selectedDateMillis = null)
                        }
                    }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                showEmptyPlaceholder -> EmptyReportSearchPlaceholder()
                filteredReports.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        modifier = Modifier.padding(top = 120.dp),
                        text = stringResource(R.string.nothing_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = ContentWidths.Search)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .testTag("report_search_results")
                            .imePadding(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 30.dp + navigationBarBottom
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            FishingReportCardItem(
                                report = report,
                                onClick = { onReportClick(report) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReportSearchPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.search_reports),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.enter_report_query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReportSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SearchInputField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.search_placeholder),
            modifier = Modifier.fillMaxWidth(),
            focusRequester = focusRequester,
            leadingContent = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            trailingContent = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                        )
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportSearchScreenPreview() {
    val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
    val calendar = Calendar.getInstance()

    val sampleReports = listOf(
        FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Смеркалось...",
            water = Water(waterName = "Водохранилище Крылово", latitude = 0.0, longitude = 0.0),
            photos = emptyList(),
            fishingStartAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant(),
            fishingEndAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant().plusSeconds(3600 * 3),
            weight = 1.2,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Окунь", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Ловил на джиг, глубина 5 метров.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        ),
        FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Отчет без фото",
            water = Water(waterName = "Чистый пруд", latitude = 0.0, longitude = 0.0),
            photos = emptyList(),
            fishingStartAt = calendar.apply { set(2024, Calendar.MAY, 1) }.time.toInstant(),
            fishingEndAt = calendar.apply { set(2024, Calendar.MAY, 1) }.time.toInstant().plusSeconds(3600 * 3),
            weight = 0.5,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Карась", count = 2)),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BREAD),
            comment = "Забыл телефон дома, фоток нет.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        )
    )
    FishingTheme {
        ReportSearchScreen(
            reports = sampleReports,
            favoriteReports = listOf(sampleReports.first()),
            state = ReportSearchState(),
            onStateChange = {},
            onReportClick = {},
            onBack = {}
        )
    }
}

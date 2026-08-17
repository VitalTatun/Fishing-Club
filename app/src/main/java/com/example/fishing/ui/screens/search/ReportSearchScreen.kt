package com.example.fishing.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingReportItem
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSearchScreen(
    reports: List<FishingReport>,
    favoriteReports: List<FishingReport> = emptyList(),
    query: String,
    onQueryChange: (String) -> Unit,
    selectedDate: Long?,
    onDateChange: (Long?) -> Unit,
    isFavoritesSelected: Boolean,
    onFavoritesChange: (Boolean) -> Unit,
    isTrophySelected: Boolean,
    onTrophyChange: (Boolean) -> Unit,
    isPaidSelected: Boolean,
    onPaidChange: (Boolean) -> Unit,
    selectedCatch: String?,
    onCatchChange: (String?) -> Unit,
    selectedMethod: FishingMethod?,
    onMethodChange: (FishingMethod?) -> Unit,
    onReportClick: (FishingReport) -> Unit,
    onBack: () -> Unit,
    currentUserId: UUID? = null,
) {
    val focusRequester = remember { FocusRequester() }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    var showCatchMenu by remember { mutableStateOf(false) }
    var showMethodMenu by remember { mutableStateOf(false) }

    val uniqueFish = remember(reports) {
        reports.flatMap { it.fish }.map { it.name }.distinct().sorted()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(datePickerState.selectedDateMillis)
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

    val filteredReports = remember(
        query,
        reports,
        selectedDate,
        isFavoritesSelected,
        isTrophySelected,
        isPaidSelected,
        selectedCatch,
        selectedMethod
    ) {
        reports.filter { report ->
            // Text query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                report.name.contains(query, ignoreCase = true) ||
                        report.water.waterName.contains(query, ignoreCase = true) ||
                        report.comment.contains(query, ignoreCase = true) ||
                        report.fish.any { it.name.contains(query, ignoreCase = true) }
            }

            // Date filter
            val matchesDate = if (selectedDate == null) {
                true
            } else {
                val calendar = Calendar.getInstance().apply { time = report.fishingTime }
                val reportDay = calendar.get(Calendar.DAY_OF_YEAR)
                val reportYear = calendar.get(Calendar.YEAR)

                val filterCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                val filterDay = filterCalendar.get(Calendar.DAY_OF_YEAR)
                val filterYear = filterCalendar.get(Calendar.YEAR)

                reportDay == filterDay && reportYear == filterYear
            }

            // Favorites filter
            val isFavorite = favoriteReports.any { it.id == report.id }
            val matchesFavorites = if (!isFavoritesSelected) true else isFavorite

            // Trophy filter
            val matchesTrophy = if (!isTrophySelected) true else report.type == FishingType.HAUL

            // Paid water filter
            val matchesPaid = if (!isPaidSelected) true else report.water.isPaid

            // Catch (fish species) filter
            val matchesCatch = if (selectedCatch == null) {
                true
            } else {
                report.fish.any { it.name == selectedCatch }
            }

            // Fishing method filter
            val matchesMethod = if (selectedMethod == null) {
                true
            } else {
                report.fishingMethod == selectedMethod
            }

            matchesQuery && matchesDate && matchesFavorites && matchesTrophy && matchesPaid && matchesCatch && matchesMethod
        }
    }

    val showEmptyPlaceholder = remember(query, selectedDate, isFavoritesSelected, isTrophySelected, isPaidSelected, selectedCatch, selectedMethod) {
        query.isBlank() && selectedDate == null && !isFavoritesSelected && !isTrophySelected && !isPaidSelected && selectedCatch == null && selectedMethod == null
    }



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                ReportSearchHeader(
                    query = query,
                    onQueryChange = onQueryChange,
                    onBack = onBack,
                    focusRequester = focusRequester,
                )
                ReportSearchFilters(
                    selectedDate = selectedDate,
                    onDateClick = { showDatePicker = true },
                    onClearDate = { onDateChange(null) },
                    isFavoritesSelected = isFavoritesSelected,
                    onFavoritesClick = { onFavoritesChange(!isFavoritesSelected) },
                    isTrophySelected = isTrophySelected,
                    onTrophyClick = { onTrophyChange(!isTrophySelected) },
                    isPaidSelected = isPaidSelected,
                    onPaidClick = { onPaidChange(!isPaidSelected) },
                    selectedCatch = selectedCatch,
                    onCatchClick = { showCatchMenu = true },
                    onClearCatch = { onCatchChange(null) },
                    selectedMethod = selectedMethod,
                    onMethodClick = { showMethodMenu = true },
                    onClearMethod = { onMethodChange(null) },
                    uniqueFish = uniqueFish,
                    showCatchMenu = showCatchMenu,
                    onDismissCatchMenu = { showCatchMenu = false },
                    onCatchSelected = { fish ->
                        onCatchChange(fish)
                        showCatchMenu = false
                    },
                    showMethodMenu = showMethodMenu,
                    onDismissMethodMenu = { showMethodMenu = false },
                    onMethodSelected = { method ->
                        onMethodChange(method)
                        showMethodMenu = false
                    }
                )
//                Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier.fillMaxSize().imePadding(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            FishingReportItem(
                                report = report,
                                onClick = { onReportClick(report) },
                                isFavorite = favoriteReports.any { it.id == report.id },
                                currentUserId = currentUserId
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
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            trailingIcon = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSearchFilters(
    selectedDate: Long?,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    isFavoritesSelected: Boolean,
    onFavoritesClick: () -> Unit,
    isTrophySelected: Boolean,
    onTrophyClick: () -> Unit,
    isPaidSelected: Boolean,
    onPaidClick: () -> Unit,
    selectedCatch: String?,
    onCatchClick: () -> Unit,
    onClearCatch: () -> Unit,
    selectedMethod: FishingMethod?,
    onMethodClick: () -> Unit,
    onClearMethod: () -> Unit,
    uniqueFish: List<String>,
    showCatchMenu: Boolean,
    onDismissCatchMenu: () -> Unit,
    onCatchSelected: (String) -> Unit,
    showMethodMenu: Boolean,
    onDismissMethodMenu: () -> Unit,
    onMethodSelected: (FishingMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(selectedDate) {
        if (selectedDate != null) {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(selectedDate))
        } else {
            null
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedDate != null,
            onClick = onDateClick,
            label = { Text(dateText ?: stringResource(R.string.date)) },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (selectedDate != null) {
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClearDate() }
                    )
                }
            } else null
        )

        Box {
            FilterChip(
                selected = selectedCatch != null,
                onClick = onCatchClick,
                label = { Text(selectedCatch ?: stringResource(R.string.catch_label)) },
                trailingIcon = {
                    if (selectedCatch != null) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClearCatch() }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = showCatchMenu,
                onDismissRequest = onDismissCatchMenu
            ) {
                if (uniqueFish.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_reports)) },
                        onClick = onDismissCatchMenu,
                        enabled = false
                    )
                } else {
                    uniqueFish.forEach { fish ->
                        DropdownMenuItem(
                            text = { Text(fish) },
                            onClick = { onCatchSelected(fish) }
                        )
                    }
                }
            }
        }

        Box {
            FilterChip(
                selected = selectedMethod != null,
                onClick = onMethodClick,
                label = { Text(selectedMethod?.let { stringResource(it.labelRes) } ?: stringResource(R.string.fishing_method)) },
                trailingIcon = {
                    if (selectedMethod != null) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClearMethod() }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = showMethodMenu,
                onDismissRequest = onDismissMethodMenu
            ) {
                FishingMethod.entries.filter { it != FishingMethod.NONE }.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(stringResource(method.labelRes)) },
                        onClick = { onMethodSelected(method) }
                    )
                }
            }
        }

        FilterChip(
            selected = isFavoritesSelected,
            onClick = onFavoritesClick,
            label = { Text(stringResource(R.string.favorites)) },
            leadingIcon = if (isFavoritesSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = isTrophySelected,
            onClick = onTrophyClick,
            label = { Text(stringResource(R.string.trophy)) },
            leadingIcon = if (isTrophySelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = isPaidSelected,
            onClick = onPaidClick,
            label = { Text(stringResource(R.string.paid)) },
            leadingIcon = if (isPaidSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
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
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Смеркалось...",
            water = Water(waterName = "Водохранилище Крылово", latitude = 0.0, longitude = 0.0),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time,
            weight = 1.2,
            fish = listOf(Fish(name = "Окунь", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Ловил на джиг, глубина 5 метров.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        ),
        FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Отчет без фото",
            water = Water(waterName = "Чистый пруд", latitude = 0.0, longitude = 0.0),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2024, Calendar.MAY, 1) }.time,
            weight = 0.5,
            fish = listOf(Fish(name = "Карась", count = 2)),
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
            query = "",
            onQueryChange = {},
            selectedDate = null,
            onDateChange = {},
            isFavoritesSelected = false,
            onFavoritesChange = {},
            isTrophySelected = false,
            onTrophyChange = {},
            isPaidSelected = false,
            onPaidChange = {},
            selectedCatch = null,
            onCatchChange = {},
            selectedMethod = null,
            onMethodChange = {},
            onReportClick = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportSearchFiltersPreview() {
    FishingTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportSearchFilters(
                selectedDate = null,
                onDateClick = {},
                onClearDate = {},
                isFavoritesSelected = false,
                onFavoritesClick = {},
                isTrophySelected = false,
                onTrophyClick = {},
                isPaidSelected = false,
                onPaidClick = {},
                selectedCatch = null,
                onCatchClick = {},
                onClearCatch = {},
                selectedMethod = null,
                onMethodClick = {},
                onClearMethod = {},
                uniqueFish = emptyList(),
                showCatchMenu = false,
                onDismissCatchMenu = {},
                onCatchSelected = {},
                showMethodMenu = false,
                onDismissMethodMenu = {},
                onMethodSelected = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            ReportSearchFilters(
                selectedDate = System.currentTimeMillis(),
                onDateClick = {},
                onClearDate = {},
                isFavoritesSelected = true,
                onFavoritesClick = {},
                isTrophySelected = true,
                onTrophyClick = {},
                isPaidSelected = true,
                onPaidClick = {},
                selectedCatch = "Окунь",
                onCatchClick = {},
                onClearCatch = {},
                selectedMethod = FishingMethod.SPINNING,
                onMethodClick = {},
                onClearMethod = {},
                uniqueFish = listOf("Окунь", "Щука"),
                showCatchMenu = false,
                onDismissCatchMenu = {},
                onCatchSelected = {},
                showMethodMenu = false,
                onDismissMethodMenu = {},
                onMethodSelected = {}
            )
        }
    }
}

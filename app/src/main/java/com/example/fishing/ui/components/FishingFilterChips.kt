package com.example.fishing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingFilterChips(
    isFavoritesSelected: Boolean,
    onFavoritesClick: () -> Unit,
    isTrophySelected: Boolean,
    onTrophyClick: () -> Unit,
    isPaidSelected: Boolean,
    onPaidClick: () -> Unit,
    selectedCatch: String?,
    onCatchSelected: (String) -> Unit,
    onClearCatch: () -> Unit,
    selectedMethod: FishingMethod?,
    onMethodSelected: (FishingMethod) -> Unit,
    onClearMethod: () -> Unit,
    uniqueFish: List<String>,
    modifier: Modifier = Modifier,
    selectedDate: Long? = null,
    onDateClick: (() -> Unit)? = null,
    onClearDate: (() -> Unit)? = null
) {
    var showCatchMenu by remember { mutableStateOf(false) }
    var showMethodMenu by remember { mutableStateOf(false) }

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
        if (onDateClick != null) {
            FilterChip(
                selected = selectedDate != null,
                onClick = onDateClick,
                label = { Text(dateText ?: stringResource(R.string.date)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (selectedDate != null && onClearDate != null) {
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
        }

        Box {
            FilterChip(
                selected = selectedCatch != null,
                onClick = { showCatchMenu = true },
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
                onDismissRequest = { showCatchMenu = false }
            ) {
                if (uniqueFish.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_reports)) },
                        onClick = { showCatchMenu = false },
                        enabled = false
                    )
                } else {
                    uniqueFish.forEach { fish ->
                        DropdownMenuItem(
                            text = { Text(fish) },
                            onClick = {
                                onCatchSelected(fish)
                                showCatchMenu = false
                            }
                        )
                    }
                }
            }
        }

        Box {
            FilterChip(
                selected = selectedMethod != null,
                onClick = { showMethodMenu = true },
                label = {
                    Text(
                        selectedMethod?.let { stringResource(it.labelRes) }
                            ?: stringResource(R.string.fishing_method)
                    )
                },
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
                onDismissRequest = { showMethodMenu = false }
            ) {
                FishingMethod.entries.filter { it != FishingMethod.NONE }.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(stringResource(method.labelRes)) },
                        onClick = {
                            onMethodSelected(method)
                            showMethodMenu = false
                        }
                    )
                }
            }
        }

        FilterChip(
            selected = isFavoritesSelected,
            onClick = onFavoritesClick,
            label = { Text(stringResource(R.string.favorites)) },
            leadingIcon = if (isFavoritesSelected) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )
        FilterChip(
            selected = isTrophySelected,
            onClick = onTrophyClick,
            label = { Text(stringResource(R.string.trophy)) },
            leadingIcon = if (isTrophySelected) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )
        FilterChip(
            selected = isPaidSelected,
            onClick = onPaidClick,
            label = { Text(stringResource(R.string.paid)) },
            leadingIcon = if (isPaidSelected) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )
    }
}

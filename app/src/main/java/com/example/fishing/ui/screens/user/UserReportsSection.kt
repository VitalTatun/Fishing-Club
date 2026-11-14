package com.example.fishing.ui.screens.user

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingReport
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.ui.components.HomeReportListItem

/**
 * Third block of the user page: the sort affordance and the author's reports.
 *
 * Exposed as a [LazyListScope] extension so the items stay part of the screen's
 * single `LazyColumn` instead of nesting a scrollable inside a scrollable.
 */
fun LazyListScope.userReportsSection(
    reports: List<FishingReport>,
    sortOrder: ReportSortOrder,
    canChangeSortOrder: Boolean,
    onSetSortOrder: (ReportSortOrder) -> Unit,
    onReportClick: (FishingReport) -> Unit
) {
    item(key = "reports-header", contentType = "reports_header") {
        UserReportsHeader(
            sortOrder = sortOrder,
            canChangeSortOrder = canChangeSortOrder,
            onSetSortOrder = onSetSortOrder
        )
    }

    if (reports.isEmpty()) {
        item(key = "reports-empty", contentType = "reports_empty") {
            Text(
                text = stringResource(R.string.no_reports),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
        return
    }

    items(
        items = reports,
        key = { report -> report.id },
        contentType = { "user_report_item" }
    ) { report ->
        // The home feed card is the same element in the design; reuse it so both
        // lists stay visually identical.
        HomeReportListItem(
            report = report,
            onClick = { onReportClick(report) },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun UserReportsHeader(
    sortOrder: ReportSortOrder,
    canChangeSortOrder: Boolean,
    onSetSortOrder: (ReportSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Photo urls may still be signing; a re-sort then would render raw paths.
    val sortTint = if (canChangeSortOrder) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.all_reports),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = canChangeSortOrder) { menuExpanded = true }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(sortOrder.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = sortTint,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.sort),
                    tint = sortTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Same two orders and the same check-mark affordance as the home feed.
            DropdownMenu(
                expanded = menuExpanded && canChangeSortOrder,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_by_publish_date)) },
                    onClick = {
                        onSetSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        if (sortOrder == ReportSortOrder.BY_PUBLISH_DATE) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_by_fishing_time)) },
                    onClick = {
                        onSetSortOrder(ReportSortOrder.BY_FISHING_TIME)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        if (sortOrder == ReportSortOrder.BY_FISHING_TIME) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@StringRes
private fun ReportSortOrder.labelRes(): Int = when (this) {
    ReportSortOrder.BY_PUBLISH_DATE -> R.string.sort_by_publish_date
    ReportSortOrder.BY_FISHING_TIME -> R.string.sort_by_fishing_time
}

/** Material's standard alpha for disabled content. */
private const val DisabledContentAlpha = 0.38f

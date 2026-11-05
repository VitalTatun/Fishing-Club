package com.example.fishing.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingReport
import com.example.fishing.ui.components.HomeReportListItem
import com.example.fishing.viewmodel.HomeUiState
import kotlinx.coroutines.delay
import java.util.UUID

private const val PREFETCH_THRESHOLD = 3

/**
 * Cold starts usually hit the Room cache within a few dozen milliseconds.
 * Holding the spinner back avoids a one-frame flash of the loading state.
 */
private const val LOADING_INDICATOR_DELAY_MILLIS = 200L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    homeUiState: HomeUiState,
    currentUserId: UUID?,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMoreReports: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onReportClick: (FishingReport) -> Unit,
    modifier: Modifier = Modifier,
    loadMoreError: String? = null,
    onRetryLoadMore: () -> Unit = onLoadMore
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (homeUiState) {
            is HomeUiState.Loading -> {
                HomeLoadingState()
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = homeUiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRefresh) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            is HomeUiState.Empty -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // A scrollable child is required: PullToRefreshBox receives the
                    // gesture through nested scroll and a plain Box produces none.
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_reports_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(onClick = onRefresh) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                val myReports = remember(homeUiState.reports, currentUserId) {
                    homeUiState.reports.filter { currentUserId == null || it.userId == currentUserId }
                }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = myReports,
                            key = { _, report -> report.id },
                            contentType = { _, _ -> "home_report_item" }
                        ) { index, report ->
                            if (hasMoreReports && index >= myReports.size - PREFETCH_THRESHOLD) {
                                LaunchedEffect(report.id) { onLoadMore() }
                            }
                            HomeReportListItem(
                                report = report,
                                onClick = { onReportClick(report) }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "loading_more", contentType = "loading_more") {
                                val loadingDescription = stringResource(R.string.loading)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .semantics { contentDescription = loadingDescription },
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        } else if (loadMoreError != null) {
                            item(key = "load_more_error", contentType = "load_more_error") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = loadMoreError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    TextButton(onClick = onRetryLoadMore) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLoadingState(modifier: Modifier = Modifier) {
    var indicatorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // If the cache arrives earlier the composable leaves the tree and this
        // coroutine is cancelled, so the spinner never flickers.
        delay(LOADING_INDICATOR_DELAY_MILLIS)
        indicatorVisible = true
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = indicatorVisible, enter = fadeIn()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.home_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

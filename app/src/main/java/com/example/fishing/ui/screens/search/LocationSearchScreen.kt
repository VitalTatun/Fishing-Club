package com.example.fishing.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.ui.components.ContentWidths
import com.example.fishing.ui.components.SearchInputField
import com.example.fishing.data.network.NominatimResponse
import com.example.fishing.data.network.getPolygonPoints
import com.example.fishing.viewmodel.LocationSearchUiState
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    state: LocationSearchUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLocationSelected: (GeoPoint, String, List<GeoPoint>?) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val navigationBarBottom = if (WindowInsets.ime.getBottom(density) == 0) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = ContentWidths.Search)
                        .fillMaxWidth()
                ) {
                    SearchFieldHeader(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        onBack = onBack,
                        focusRequester = focusRequester,
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
                state.isSearching -> EmptySearchPlaceholder(isLoading = true)
                state.hasError -> SearchErrorPlaceholder(onRetry = onRetry)
                state.query.isBlank() -> EmptySearchPlaceholder(isLoading = false)
                state.results.isEmpty() -> Box(
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = ContentWidths.Search)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                top = 8.dp,
                                end = 8.dp,
                                bottom = 8.dp + navigationBarBottom
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.results, key = { it.placeId }) { result ->
                                LocationResultItem(
                                    result = result,
                                    onClick = {
                                        val lat = result.lat.toDoubleOrNull() ?: 0.0
                                        val lon = result.lon.toDoubleOrNull() ?: 0.0
                                        val polygon = result.getPolygonPoints()?.map { 
                                            GeoPoint(it[1], it[0]) 
                                        }
                                        onLocationSelected(GeoPoint(lat, lon), result.displayName, polygon)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchErrorPlaceholder(onRetry: () -> Unit) {
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
                text = stringResource(R.string.location_search_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun LocationResultItem(
    result: NominatimResponse,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.displayName.split(",").firstOrNull() ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptySearchPlaceholder(isLoading: Boolean = false) {
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.search_places),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.enter_place_name),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchFieldHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
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

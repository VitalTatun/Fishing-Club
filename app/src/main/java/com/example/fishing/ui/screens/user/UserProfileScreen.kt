package com.example.fishing.ui.screens.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.UserProfileUiState
import java.time.Instant
import java.util.UUID

/** Figma "Экран профиля пользователя" (4158:6583). */
private val ScreenBackground = Color(0xFFF9FAFE)

/**
 * Scroll distance after which the page header is considered hidden: the avatar,
 * the gap below it and a little of the first name line.
 */
private val AppBarTitleThreshold = 80.dp

/**
 * Public page of another user, opened from the author row of a report.
 *
 * Deliberately read-only: no editing, no settings — that lives in the current
 * user's own profile tab. The page is assembled from three blocks: header,
 * statistics and the reports list.
 *
 * While the header scrolls under the app bar, a compact avatar and the name fade
 * into the bar, so the author stays identifiable in the collapsed state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    uiState: UserProfileUiState,
    onBackClick: () -> Unit,
    onReportClick: (FishingReport) -> Unit = {},
    onSetSortOrder: (ReportSortOrder) -> Unit = {},
    onRetry: () -> Unit = {},
    sortOrder: ReportSortOrder = ReportSortOrder.BY_FISHING_TIME,
    canChangeSortOrder: Boolean = true,
    fallbackUserName: String = "",
    fallbackUserAvatar: String = "",
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // The profile request may still be in flight or return nothing; the values
    // handed over by the report screen keep the header populated meanwhile.
    val userName = (uiState as? UserProfileUiState.Success)
        ?.user?.name?.takeIf { it.isNotBlank() }
        ?: fallbackUserName.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.fisherman)
    val avatarUrl = (uiState as? UserProfileUiState.Success)
        ?.user?.image?.takeIf { it.isNotBlank() }
        ?: fallbackUserAvatar

    val threshold = with(LocalDensity.current) { AppBarTitleThreshold.toPx() }
    val showAppBarTitle by remember(listState, threshold) {
        derivedStateOf {
            // Index 0 is the page header item, so anything further means it has
            // scrolled away completely.
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > threshold
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBackground,
        topBar = {
            UserProfileTopBar(
                userName = userName,
                avatarUrl = avatarUrl,
                showTitle = uiState is UserProfileUiState.Success && showAppBarTitle,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        when (uiState) {
            UserProfileUiState.Loading -> UserProfileLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is UserProfileUiState.Error -> UserProfileErrorState(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is UserProfileUiState.Success -> UserProfileContent(
                state = uiState,
                userName = userName,
                avatarUrl = avatarUrl,
                sortOrder = sortOrder,
                canChangeSortOrder = canChangeSortOrder,
                onSetSortOrder = onSetSortOrder,
                onReportClick = onReportClick,
                listState = listState,
                contentPadding = paddingValues
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileTopBar(
    userName: String,
    avatarUrl: String,
    showTitle: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserAvatar(
                        avatarUrl = avatarUrl,
                        size = AppBarAvatarSize,
                        iconSize = 20.dp
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ScreenBackground
        )
    )
}

@Composable
private fun UserProfileContent(
    state: UserProfileUiState.Success,
    userName: String,
    avatarUrl: String,
    sortOrder: ReportSortOrder,
    canChangeSortOrder: Boolean,
    onSetSortOrder: (ReportSortOrder) -> Unit,
    onReportClick: (FishingReport) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
            UserProfileHeader(
                userName = userName,
                avatarUrl = avatarUrl,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "stats") {
            UserProfileStats(
                caughtCount = state.caughtCount,
                reportsCount = state.reportsCount,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp
                )
            )
        }

        userReportsSection(
            reports = state.reports,
            sortOrder = sortOrder,
            canChangeSortOrder = canChangeSortOrder,
            onSetSortOrder = onSetSortOrder,
            onReportClick = onReportClick
        )
    }
}

@Composable
private fun UserProfileLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UserProfileErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun UserProfileScreenPreview() {
    val water = Water(waterName = "водохранилище д.Межлесье", latitude = 54.0, longitude = 27.0)
    val reports = listOf("Окунь", "Карась", "Окунь").mapIndexed { index, fish ->
        FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Отчёт",
            water = water,
            photos = listOf(FishingPhoto(url = "https://picsum.photos/400/300?random=$index")),
            fishingStartAt = Instant.parse("2026-08-12T09:00:00Z").minusSeconds(index * 86_400L),
            weight = 1.5,
            fish = listOf(Fish(id = UUID.randomUUID(), name = fish, count = 2)),
            fishingMethod = FishingMethod.SPINNING,
            bait = emptyList(),
            comment = "",
            user = User(name = "Никита Белозерцев", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = true
        )
    }
    FishingTheme {
        UserProfileScreen(
            uiState = UserProfileUiState.Success(
                user = User(name = "Никита Белозерцев", image = "", email = ""),
                reports = reports,
                caughtCount = 98,
                reportsCount = 25
            ),
            onBackClick = {}
        )
    }
}

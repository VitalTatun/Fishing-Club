package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FishingReportItem(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteReport: (FishingReport) -> Unit = {},
    isFavorite: Boolean = false,
    currentUserId: UUID? = null,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_report)) },
            text = { Text(stringResource(R.string.delete_report_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteReport(report)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FishingReportHeader(
                user = report.user,
                date = report.publishedAt ?: report.fishingTime,
                onDeleteClick = { showDeleteDialog = true },
                showDeleteOption = report.userId == currentUserId
            )

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                FishingReportTitle(
                    report = report,
                    isFavorite = isFavorite
                )
                FishingReportDetails(report = report)
            }

            if (report.comment.isNotBlank()) {
                Text(
                    text = report.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (report.photo.isNotEmpty()) {
            FishingReportPhotos(photos = report.photo)
        }
    }
}

@Composable
private fun FishingReportHeader(
    user: User,
    date: Date,
    onDeleteClick: () -> Unit,
    showDeleteOption: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (user.image.isNotBlank()) {
                AsyncImage(
                    model = user.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifBlank { stringResource(R.string.fisherman) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormatter.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDeleteOption) {
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { showMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingReportTitle(
    report: FishingReport,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    val fishFallback = stringResource(R.string.fish_fallback)
    val methodName = stringResource(report.fishingMethod.labelRes)
    val fishName = report.fish.firstOrNull()?.name ?: fishFallback
    val title = "$methodName • $fishName"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (report.type == FishingType.HAUL) {
            Surface(
                color = FishingTheme.colors.trophyYellow,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.trophy),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = FishingTheme.colors.textOnTrophy
                )
            }
        }

        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = FishingTheme.colors.bookmarkRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FishingReportDetails(
    report: FishingReport,
    modifier: Modifier = Modifier
) {
    val details = buildString {
        append(report.water.waterName)
        if (report.water.isPaid) {
            append(" • ")
            append(stringResource(R.string.paid_water))
        }
        append(" • ")
        append(stringResource(if (report.fishingFromTheShore) R.string.fishing_from_shore else R.string.fishing_from_boat))
    }

    Text(
        text = details,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FishingReportPhotos(
    photos: List<String>,
) {
    val pagerState = rememberPagerState { photos.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 8.dp
        ) { index ->
            AsyncImage(
                model = photos[index],
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        if (photos.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FishingReportItemPreview() {
    val sampleUser = User(name = "Иван", image = "", email = "ivan@example.com")
    val sampleWater = Water(waterName = "Минское море", latitude = 55.0, longitude = 60.0, isPaid = true)
    val calendar = Calendar.getInstance().apply {
        set(2023, Calendar.AUGUST, 22)
    }
    val sampleUserId = UUID.randomUUID()
    val sampleReport = FishingReport(
        userId = sampleUserId,
        type = FishingType.HAUL,
        name = "Смеркалось...",
        water = sampleWater,
        photo = listOf(
            "https://picsum.photos/800/400?random=1",
            "https://picsum.photos/800/400?random=2"
        ),
        fishingTime = calendar.time,
        weight = 2.5,
        fish = listOf(Fish(name = "Окунь", count = 5)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "Прекрасное утро. В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб!",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = false
    )
    Box(modifier = Modifier
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)) {
        FishingTheme {
            FishingReportItem(
                report = sampleReport,
                isFavorite = true,
                currentUserId = sampleUserId
            )
        }
    }
}

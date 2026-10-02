package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.screens.report.detail.PhotoViewerOverlay
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FishingReportListItem(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPhotoClick: ((Int) -> Unit)? = null,
    likeState: ReportLikeState? = null,
    onToggleLike: (() -> Unit)? = null,
) {
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    val date = report.publishedAt ?: report.fishingStartAt?.let { Date.from(it) } ?: Date()

    val fishFallback = stringResource(R.string.fish_fallback)
    val methodName = stringResource(report.fishingMethod.labelRes)
    val fishName = report.fish.firstOrNull()?.name ?: fishFallback
    val title = "$methodName • $fishName"

    val details = listOfNotNull(
        report.water.waterName.takeIf { it.isNotBlank() },
        stringResource(R.string.paid_water).takeIf { report.water.isPaid },
        stringResource(if (report.fishingFromTheShore) R.string.fishing_from_shore else R.string.fishing_from_boat)
    ).joinToString(" • ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFFF9FAFE) // Figma background surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color(0xFFC5C6D0)
            )
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 2. REPORT DETAILS (Primary & Location Metadata)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (report.type == FishingType.HAUL) {
                        Surface(
                            modifier = Modifier.size(width = 60.dp, height = 18.dp),
                            color = Color(0xFFFFD71D), // semantic yellow
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.trophy),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF50250A)
                                    )
                                )
                            }
                        }
                    }
                }

                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. PHOTO DISPLAY (Single or Horizontal Scroll)
            if (report.photos.size == 1) {
                AsyncImage(
                    model = report.photos[0].url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFECE6F0))
                        .clickable {
                            if (onPhotoClick != null) {
                                onPhotoClick(0)
                            } else {
                                selectedPhotoIndex = 0
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            } else if (report.photos.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(report.photos) { index, photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFECE6F0))
                                .clickable {
                                    if (onPhotoClick != null) {
                                        onPhotoClick(index)
                                    } else {
                                        selectedPhotoIndex = index
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 4. REPORT DESCRIPTION
            if (report.comment.isNotBlank()) {
                Text(
                    text = report.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 5. AUTHOR AND ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5.1 & 5.2 AVATAR & AUTHOR DETAILS
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECE6F0))
                ) {
                    if (report.user.image.isNotBlank()) {
                        AsyncImage(
                            model = report.user.image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(20.dp),
                            tint = Color(0xFF44474F)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.user.name.ifBlank { stringResource(R.string.fisherman) },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 5.3 ENGAGEMENT ACTIONS
                if (likeState != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (onToggleLike != null) {
                                    Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                        onToggleLike()
                                    }
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (likeState.isLiked) R.drawable.thumb_up_20px_2 else R.drawable.thumb_up_20px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = likeState.likesCount.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedPhotoIndex != null) {
        PhotoViewerOverlay(
            photos = report.photos.map { it.url },
            initialIndex = selectedPhotoIndex!!,
            onDismiss = { selectedPhotoIndex = null }
        )
    }
}
}

@Preview(showBackground = true, widthDp = 412)
@Composable
fun FishingReportListItemPreview() {
    val sampleUser = User(name = "Никита Белозерцев", image = "", email = "vital@example.com")
    val sampleWater = Water(waterName = "Озеро у деревни Вулька 2", latitude = 55.0, longitude = 60.0, isPaid = true)
    val calendar = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 25)
    }
    val sampleReport = FishingReport(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        type = FishingType.HAUL,
        name = "Спиннинг • Окунь",
        water = sampleWater,
        photos = listOf(
            FishingPhoto(url = "https://picsum.photos/800/400?random=1"),
            FishingPhoto(url = "https://picsum.photos/800/400?random=2"),
            FishingPhoto(url = "https://picsum.photos/800/400?random=3")
        ),
        fishingStartAt = calendar.time.toInstant(),
        fishingEndAt = calendar.time.toInstant().plusSeconds(3600 * 3),
        weight = 2.5,
        fish = listOf(Fish(id = UUID.randomUUID(), name = "Окунь", count = 5)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб! Замешав вечерком плотву с орехом от Feeder.by с утра поехал на мелководную часть вдх посмотреть как там обстоят дела с рыбкой...",
        user = sampleUser,
        fishingFromTheShore = false,
        isPublic = false
    )
    FishingTheme {
        FishingReportListItem(
            report = sampleReport,
            likeState = ReportLikeState(reportId = sampleReport.id, likesCount = 1, isLiked = false)
        )
    }
}

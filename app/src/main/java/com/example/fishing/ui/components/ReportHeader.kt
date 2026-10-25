package com.example.fishing.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@Composable
fun ReportHeader(
    report: FishingReport,
    modifier: Modifier = Modifier,
    likesCount: Int? = null,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    onPhotoClick: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPhotoCarousel(
            photos = report.photos,
            showTrophyBadge = report.type == FishingType.HAUL,
            onPhotoClick = onPhotoClick
        )
        UserInfoBlock(
            user = report.user,
            date = report.publishedAt ?: report.fishingStartAt?.let { Date.from(it) } ?: Date(),
            modifier = Modifier.padding(horizontal = 16.dp),
            likesCount = likesCount,
            isLiked = isLiked,
            onToggleLike = onToggleLike
        )
    }
}

@Composable
fun ReportPhotoCarousel(
    photos: List<FishingPhoto>,
    modifier: Modifier = Modifier,
    showTrophyBadge: Boolean = false,
    onPhotoClick: (Int) -> Unit = {}
) {
    if (photos.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (photos.size == 1) {
            AsyncImage(
                model = photos[0].url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFECE6F0))
                    .clickable { onPhotoClick(0) },
                contentScale = ContentScale.Crop
            )
        } else if (photos.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(photos) { index, photo ->
                    AsyncImage(
                        model = photo.url,
                        contentDescription = null,
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFECE6F0))
                            .clickable { onPhotoClick(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        if (showTrophyBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp, start = 22.dp),
                color = FishingTheme.colors.trophyYellow,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = stringResource(R.string.trophy),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = FishingTheme.colors.textOnTrophy
                )
            }
        }
    }
}

@Composable
fun UserInfoBlock(
    user: User,
    date: Date,
    modifier: Modifier = Modifier,
    likesCount: Int? = null,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null
) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.name.ifBlank { stringResource(R.string.fisherman) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (likesCount != null) {
            ReportLikeCount(
                likesCount = likesCount,
                isLiked = isLiked,
                onToggleLike = onToggleLike
            )
        }
    }
}

@Composable
private fun ReportLikeCount(
    likesCount: Int,
    isLiked: Boolean,
    onToggleLike: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .then(
                if (onToggleLike != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onToggleLike()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                } else Modifier
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = if (isLiked) R.drawable.thumb_up_20px_2 else R.drawable.thumb_up_20px),
            contentDescription = null,
            tint = if (isLiked) Color(0xFFFF383C) else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        AnimatedContent(
            targetState = likesCount,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                } else {
                    (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                }
            },
            label = "likesCount"
        ) { count ->
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportHeaderPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photos = listOf(FishingPhoto(url = "")),
            fishingStartAt = Instant.now().minusSeconds(3600 * 3),
            fishingEndAt = Instant.now(),
            weight = 3.2,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Окунь", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(),
            comment = "",
            user = User(name = "Никита Белозерцев", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = false
        )
        ReportHeader(
            report = sampleReport,
            likesCount = 12,
            isLiked = true,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

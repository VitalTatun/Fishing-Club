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
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.time.Instant
import java.util.UUID

@Composable
fun ReportHeader(
    report: FishingReport,
    modifier: Modifier = Modifier,
    likesCount: Int? = null,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    onPhotoClick: (Int) -> Unit = {},
    onUserClick: (() -> Unit)? = null
) {
    val fishFallback = stringResource(R.string.fish_fallback)
    val methodName = stringResource(report.fishingMethod.labelRes)
    val fishName = report.fish.firstOrNull()?.name ?: fishFallback
    val title = "$fishName • $methodName"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header (Figma node 4200:7177 "Шапка профиля")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportAuthorRow(
                report = report,
                title = title,
                likesCount = likesCount,
                isLiked = isLiked,
                onToggleLike = onToggleLike,
                onUserClick = onUserClick
            )

            ReportBadges(report = report)
        }

        // Photo Carousel
        ReportPhotoCarousel(
            photos = report.photos,
            onPhotoClick = onPhotoClick
        )
    }
}

@Composable
private fun ReportAuthorRow(
    report: FishingReport,
    title: String,
    likesCount: Int?,
    isLiked: Boolean,
    onToggleLike: (() -> Unit)?,
    onUserClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar (48.dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (onUserClick != null) {
                        Modifier
                            .clickable(onClick = onUserClick)
                    } else {
                        Modifier
                    }
                ),
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Title and Username
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = report.user.name.ifBlank { stringResource(R.string.fisherman) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Like count
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
private fun ReportBadges(report: FishingReport) {
    // Shore fishing is the default, so only boat fishing gets a badge
    val isDraft = !report.isPublic
    val showBoatBadge = !report.fishingFromTheShore

    if (isDraft || showBoatBadge) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDraft) {
                ReportBadge(
                    text = stringResource(R.string.draft),
                    icon = Icons.Default.VisibilityOff,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }

            if (showBoatBadge) {
                ReportBadge(
                    text = stringResource(R.string.fishing_from_boat),
                    icon = Icons.Default.Anchor,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ReportPhotoCarousel(
    photos: List<FishingPhoto>,
    modifier: Modifier = Modifier,
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
                    .padding(horizontal = 16.dp)
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
                contentPadding = PaddingValues(horizontal = 16.dp),
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
    }
}

@Composable
private fun ReportBadge(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
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

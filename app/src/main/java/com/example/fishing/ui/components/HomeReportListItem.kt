package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeReportListItem(
    report: FishingReport,
    currentUserId: UUID?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isCurrentUser = currentUserId != null && report.userId == currentUserId
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    val date = report.publishedAt ?: report.fishingStartAt?.let { Date.from(it) } ?: Date()

    val fishFallback = stringResource(R.string.fish_fallback)
    val methodName = stringResource(report.fishingMethod.labelRes)
    val fishName = report.fish.firstOrNull()?.name ?: fishFallback
    val title = "$methodName • $fishName"
    val waterName = report.water.waterName.takeIf { it.isNotBlank() } ?: ""

    val topText = if (!isCurrentUser) {
        val authorName = report.user.name.ifBlank { stringResource(R.string.fisherman) }
        "$authorName • ${dateFormatter.format(date)}"
    } else {
        dateFormatter.format(date)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECE6F0)),
                    contentAlignment = Alignment.Center
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
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF44474F)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECE6F0)),
                    contentAlignment = Alignment.Center
                ) {
                    val firstPhoto = report.photos.firstOrNull()?.url
                    if (!firstPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = firstPhoto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF44474F)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = topText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (waterName.isNotBlank()) {
                    Text(
                        text = waterName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "Home Item - Own Report")
@Composable
fun HomeReportListItemOwnPreview() {
    val myId = UUID.randomUUID()
    val sampleUser = User(id = myId, name = "Я", image = "", email = "me@example.com")
    val sampleWater = Water(waterName = "Заславское водохранилище", latitude = 54.0, longitude = 27.0)
    val calendar = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 10)
    }
    val sampleReport = FishingReport(
        id = UUID.randomUUID(),
        userId = myId,
        type = FishingType.FISHING_LOG,
        name = "Поплавок • Плотва",
        water = sampleWater,
        photos = emptyList(),
        fishingStartAt = calendar.time.toInstant(),
        weight = 1.2,
        fish = listOf(Fish(id = UUID.randomUUID(), name = "Плотва", count = 3)),
        fishingMethod = FishingMethod.BOBBER,
        bait = listOf(Bait.CORN),
        comment = "",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = true
    )
    FishingTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HomeReportListItem(
                report = sampleReport,
                currentUserId = myId
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "Home Item - Bookmark (Other User)")
@Composable
fun HomeReportListItemBookmarkPreview() {
    val myId = UUID.randomUUID()
    val otherId = UUID.randomUUID()
    val sampleUser = User(id = otherId, name = "Никита Белозерцев", image = "", email = "nikita@example.com")
    val sampleWater = Water(waterName = "Заславское водохранилище", latitude = 54.0, longitude = 27.0)
    val calendar = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 18)
    }
    val sampleReport = FishingReport(
        id = UUID.randomUUID(),
        userId = otherId,
        type = FishingType.FISHING_LOG,
        name = "Поплавок • Карась",
        water = sampleWater,
        photos = emptyList(),
        fishingStartAt = calendar.time.toInstant(),
        weight = 2.0,
        fish = listOf(Fish(id = UUID.randomUUID(), name = "Карась", count = 2)),
        fishingMethod = FishingMethod.BOBBER,
        bait = listOf(Bait.WORM),
        comment = "",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = true
    )
    FishingTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HomeReportListItem(
                report = sampleReport,
                currentUserId = myId
            )
        }
    }
}

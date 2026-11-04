package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    val date = report.publishedAt ?: report.fishingStartAt?.let { Date.from(it) } ?: Date()

    val methodName = stringResource(report.fishingMethod.labelRes)
    val fishName = report.fish.firstOrNull()?.name ?: stringResource(R.string.fish_fallback)
    val title = "$methodName • $fishName"
    val waterName = report.water.waterName.trim()
    val firstPhoto = report.photos.firstOrNull()?.url

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = dateFormatter.format(date),
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
                if (waterName.isNotEmpty()) {
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
            if (!firstPhoto.isNullOrBlank()) {
                AsyncImage(
                    model = firstPhoto,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
                        .width(112.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFECE6F0)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun previewReport(
    date: Date,
    fishName: String,
    photos: List<FishingPhoto> = emptyList()
): FishingReport {
    val userId = UUID.randomUUID()
    return FishingReport(
        id = UUID.randomUUID(),
        userId = userId,
        type = FishingType.FISHING_LOG,
        name = fishName,
        water = Water(
            waterName = "Заславское водохранилище",
            latitude = 54.0,
            longitude = 27.0
        ),
        photos = photos,
        fishingStartAt = date.toInstant(),
        weight = 1.2,
        fish = listOf(Fish(id = UUID.randomUUID(), name = fishName, count = 1)),
        fishingMethod = FishingMethod.BOBBER,
        bait = listOf(Bait.CORN),
        comment = "",
        user = User(id = userId, name = "Я", image = "", email = "me@example.com"),
        fishingFromTheShore = true,
        isPublic = true
    )
}

@Preview(showBackground = true, widthDp = 412, name = "Home Item - With Photo")
@Composable
fun HomeReportListItemWithPhotoPreview() {
    FishingTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HomeReportListItem(
                report = previewReport(
                    date = Calendar.getInstance().apply { set(2026, Calendar.JUNE, 10) }.time,
                    fishName = "Плотва",
                    photos = listOf(FishingPhoto(url = "https://picsum.photos/300/200"))
                )
            )
        }
    }
}

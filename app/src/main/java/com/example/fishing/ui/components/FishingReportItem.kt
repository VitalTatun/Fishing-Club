package com.example.fishing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Verified
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),

    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            if (report.photo.isNotEmpty()) {
                FishingReportPhotos(photos = report.photo)
            }
            FishingReportHeader(report = report)
            FishingReportFooter(report = report, onDeleteReport = { onDeleteReport(report) })
        }
    }
}

@Composable
private fun FishingReportHeader(report: FishingReport) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!report.isPublic) {
            PrivateReportBadge()
            Spacer(modifier = Modifier.width(5.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ReportBadges(
                    report = report,
                    showDraftBadge = false
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (report.water.isPaid) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = "Платный водоем",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Bookmark",
                    tint = FishingTheme.colors.bookmarkRed,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormatter.format(report.fishingTime),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.water.waterName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun FishingReportPhotos(photos: List<String>) {
    val pagerState = rememberPagerState { photos.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
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
                    .padding(top = 8.dp, end = 24.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    )
                )
            }
        }
    }
}

@Composable
private fun FishingReportFooter(
    report: FishingReport,
    onDeleteReport: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить отчет") },
            text = { Text("Вы уверены, что хотите удалить этот отчет?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteReport()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (report.fish.isNotEmpty()) {
                TagChip(text = report.fish.first().name, icon = Icons.Default.SetMeal)
            }

            TagChip(
                text = when(report.fishingMethod) {
                    FishingMethod.SPINNING -> "Спиннинг"
                    FishingMethod.BOBBER -> "Поплавок"
                    FishingMethod.FEEDER -> "Фидер"
                    else -> "Метод"
                },
                icon = Icons.Default.Phishing
            )
        }
        Box {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showMenu = true }
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Удалить отчет") },
                    onClick = {
                        showMenu = false
                        showDeleteDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivateReportBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Private",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun ReportBadges(
    report: FishingReport,
    modifier: Modifier = Modifier,
    showDraftBadge: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!report.isPublic && showDraftBadge) {
            DraftBadge()
        }
        if (report.type == FishingType.HAUL) {
            TrophyBadge()
        }
    }
}

@Composable
fun TrophyBadge() {
    Surface(
        color = FishingTheme.colors.trophyYellow,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Трофей",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = FishingTheme.colors.textOnTrophy,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DraftBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Не опубликован",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TagChip(
    text: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FishingReportItemPreview() {
    val sampleUser = User(name = "Иван", image = "", email = "ivan@example.com")
    val sampleWater = Water(waterName = "Водохранилище Крылово", latitude = 55.0, longitude = 60.0)
    val calendar = Calendar.getInstance().apply { 
        set(2023, Calendar.AUGUST, 22) 
    }
    val sampleReport = FishingReport(
        userId = UUID.randomUUID(),
        type = FishingType.HAUL,
        name = "Смеркалось...",
        water = sampleWater,
        photo = emptyList(),
        fishingTime = calendar.time,
        weight = 2.5,
        fish = listOf(Fish(name = "Окунь", count = 5)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "Прекрасное утро.",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = false
    )
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        FishingTheme {
            FishingReportItem(report = sampleReport)
        }
    }
}

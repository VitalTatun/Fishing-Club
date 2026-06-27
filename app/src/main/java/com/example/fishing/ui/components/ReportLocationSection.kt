package com.example.fishing.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*

@Composable
fun MapCell(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    val trophyColor = FishingTheme.colors.trophyYellow.toArgb()
    val regularColor = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
    ) {
        if (isInPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Карта",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false)
                        isClickable = false
                        isFocusable = false

                        controller.setZoom(15.0)
                        val point = GeoPoint(report.water.latitude, report.water.longitude)
                        controller.setCenter(point)

                        overlays.add(Marker(this).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            val markerColor = if (report.type == FishingType.HAUL) trophyColor else regularColor
                            icon = MarkerDrawableUtils.getMarkerDrawable(context, MarkerShape.CIRCLE, markerColor, report.fishingMethod)

                            setOnMarkerClickListener { _, _ -> true }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { it.onDetach() }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = true, onClick = onMapClick)
        )
    }
}

@Composable
fun CoordinatesCell(
    report: FishingReport,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.water.waterName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${"%.5f".format(report.water.latitude)} - ${"%.5f".format(report.water.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Копировать координаты",
            tint = primaryColor.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    "Coordinates",
                                    "${report.water.latitude}, ${report.water.longitude}"
                                )
                            )
                        )
                    }
                }
        )
    }
}

@Composable
fun ReportLocationSection(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    SectionCard(
        title = "Водоем",
        items = listOf(
            SectionEntry.ComposableItem(
                padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                MapCell(report = report, onMapClick = onMapClick)
            },
            SectionEntry.ComposableItem() {
                CoordinatesCell(report = report)
            },
            SectionEntry.TextItem(
                label = "Ловля с берега",
                value = if (report.fishingFromTheShore) "Да" else "Нет"
            ),
            SectionEntry.TextItem(
                label = "Платный водоем",
                value = if (report.water.isPaid) "Да" else "Нет"
            )
        ),
        modifier = modifier.padding(horizontal = 16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun ReportLocationSectionPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Тестовый отчет",
            water = Water(
                waterName = "Озеро Нарочь",
                latitude = 54.8510,
                longitude = 26.7086
            ),
            photo = listOf(),
            fishingTime = Date(),
            weight = 0.0,
            fish = listOf(),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(),
            comment = "",
            user = User(name = "Иван Иванов", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = true
        )
        ReportLocationSection(
            report = sampleReport,
            modifier = Modifier.padding(16.dp)
        )
    }
}

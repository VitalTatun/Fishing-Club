package com.example.fishing.ui.components

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ReportLocationSection(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.padding(horizontal = 16.dp),

            ) {
        Text(
            text = "Водоем",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
        )

        // Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            val inPreview = LocalInspectionMode.current

            if (inPreview) {
                val ctx = LocalContext.current
                val previewTrophyColor = FishingTheme.colors.trophyYellow.toArgb()
                val markerDrawable = remember(previewTrophyColor, report.fishingMethod) {
                    MarkerDrawableUtils.getMarkerDrawable(
                        ctx,
                        MarkerShape.DROP,
                        previewTrophyColor,
                        report.fishingMethod,
                        android.graphics.Color.parseColor("#50250A")
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidDrawable(
                        drawable = markerDrawable,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val regularColorInt = MaterialTheme.colorScheme.primary.toArgb()
                val trophyColorInt = FishingTheme.colors.trophyYellow.toArgb()
                val trophyIconColorInt = android.graphics.Color.parseColor("#50250A")

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false)
                            setBuiltInZoomControls(false)
                            isClickable = false
                            isFocusable = false

                            controller.setZoom(15.0)
                            val point = GeoPoint(report.water.latitude, report.water.longitude)
                            controller.setCenter(point)

                            val shape = MarkerShape.DROP
                            val color = if (report.type == FishingType.HAUL) trophyColorInt else regularColorInt
                            val iconColor = if (report.type == FishingType.HAUL) trophyIconColorInt else android.graphics.Color.WHITE

                            overlays.add(Marker(this).apply {
                                position = point
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = MarkerDrawableUtils.getMarkerDrawable(ctx, shape, color, report.fishingMethod, iconColor)
                                setOnMarkerClickListener { _, _ -> true }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { it.onDetach() }
                )

                // Block all touch events on the map
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                )
            }

            // Zoom button
            FilledTonalIconButton(
                onClick = onMapClick,
                modifier = Modifier
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = "Открыть карту",
                    tint = Color.White
                )
            }
        }

        // Badges
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (report.water.isPaid) {
                LocationBadge(
                    icon = Icons.Default.AttachMoney,
                    text = "Платный"
                )
            }
            if (report.fishingFromTheShore) {
                LocationBadge(
                    icon = Icons.Default.Anchor,
                    text = "Ловля с берега"
                )
            }
            if (!report.fishingFromTheShore) {
                LocationBadge(
                    icon = Icons.Default.Anchor,
                    text = "Ловля с лодки"
                )
            }
        }

        // Water name and coordinates
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 0.dp, top = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = report.water.waterName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${"%.5f".format(report.water.latitude)} - ${"%.5f".format(report.water.longitude)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
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
                },
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Копировать координаты",
                    tint = primaryColor
                )
            }
        }
    }
}

@Composable
private fun AndroidDrawable(
    drawable: android.graphics.drawable.Drawable,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                setImageDrawable(drawable)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LocationBadge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportLocationSectionPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Тестовый отчет",
            water = Water(
                waterName = "Заславское водохранилище, Дамба",
                latitude = 54.32344,
                longitude = 54.23425,
                isPaid = true
            ),
            photo = listOf(),
            fishingTime = Date(),
            weight = 0.0,
            fish = listOf(),
            fishingMethod = FishingMethod.SPINNING,
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

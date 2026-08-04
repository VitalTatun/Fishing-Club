package com.example.fishing.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.R
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            val inPreview = LocalInspectionMode.current

            if (inPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.map_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Marker Preview
                    val markerColor = if (report.type == FishingType.HAUL) 
                        Color(0xFFFFD71D) else MaterialTheme.colorScheme.primary
                        
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(50.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(markerColor, shape = RoundedCornerShape(50.dp))
                                .padding(2.dp)
                                .background(Color.White, shape = RoundedCornerShape(50.dp))
                                .padding(2.dp)
                                .background(markerColor, shape = RoundedCornerShape(50.dp))
                        )
                    }
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

                            val shape = MarkerShape.DOT
                            val color = if (report.type == FishingType.HAUL) trophyColorInt else regularColorInt
                            val iconColor = if (report.type == FishingType.HAUL) trophyIconColorInt else android.graphics.Color.WHITE

                            overlays.add(Marker(this).apply {
                                position = point
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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
            OverlayIconButton(
                icon = Icons.Default.ZoomOutMap,
                onClick = onMapClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                contentDescription = stringResource(R.string.open_map)
            )
        }

        // Name and Coordinates Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.water.waterName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${"%.5f".format(report.water.latitude)} - ${"%.5f".format(report.water.longitude)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            
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
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_coordinates),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (report.water.isPaid) {
                FishingBadge(
                    text = stringResource(R.string.paid)
                )
            }
            FishingBadge(
                text = stringResource(
                    if (report.fishingFromTheShore) R.string.fishing_from_shore else R.string.fishing_from_boat
                )
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
        ReportLocationSection(report = sampleReport)
    }
}

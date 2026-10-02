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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
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

        // Water info as list item
        val shoreText = stringResource(
            if (report.fishingFromTheShore) R.string.fishing_from_shore else R.string.fishing_from_boat
        )
        val overlineText = if (report.water.isPaid) {
            "${stringResource(R.string.paid_water)} • $shoreText"
        } else {
            shoreText
        }

        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()

        FishingListItem(
            overlineText = overlineText,
            title = report.water.waterName.ifBlank { stringResource(R.string.not_specified) },
            supportingText = "${"%.5f".format(report.water.latitude)} - ${"%.5f".format(report.water.longitude)}",
            trailingContent = {
                FilledTonalIconButton(
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
                        contentDescription = stringResource(R.string.copy_coordinates)
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportLocationSectionPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Тестовый отчет",
            water = Water(
                waterName = "Заславское водохранилище, Дамба",
                latitude = 54.32344,
                longitude = 54.23425,
                isPaid = true
            ),
            photos = listOf(),
            fishingStartAt = java.time.Instant.now().minusSeconds(3600 * 3),
            fishingEndAt = java.time.Instant.now(),
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

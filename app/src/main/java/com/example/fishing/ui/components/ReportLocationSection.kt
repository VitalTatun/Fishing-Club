package com.example.fishing.ui.components

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
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
fun ReportLocationSection(
    report: FishingReport, 
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trophyColor = FishingTheme.colors.trophyYellow.toArgb()
    val regularColor = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Водоем",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )

        // Table structure
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Cell 1: Map
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                color = FishingTheme.colors.secondaryBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(
                            topStart = 12.dp, topEnd = 12.dp,
                            bottomStart = 2.dp, bottomEnd = 2.dp
                        ))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                // Полностью отключаем взаимодействие
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
                                    icon = BitmapDrawable(context.resources, createMarkerBitmap(markerColor))
                                    
                                    // Отключаем клик по маркеру
                                    setOnMarkerClickListener { _, _ -> true }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { it.onDetach() }
                    )
                    
                    // Прозрачный слой поверх карты, чтобы перехватывать любые нажатия
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = true, onClick = onMapClick)
                    )
                }
            }

            // Cell 2: Info (Name + Coordinates)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = FishingTheme.colors.secondaryBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = report.water.waterName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    LocationInfoRow(
                        report = report,
                        primaryColor = primaryColor
                    )
                }
            }

            // Cell 3: Ловля с берега
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = FishingTheme.colors.secondaryBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ловля с берега",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (report.fishingFromTheShore) "Да" else "Нет",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Cell 4: Платный водоем
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = FishingTheme.colors.secondaryBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Платный водоем",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (report.water.isPaid) "Да" else "Нет",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun LocationInfoRow(
    report: FishingReport,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "GPS координаты: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${"%.5f".format(report.water.latitude)} - ${"%.5f".format(report.water.longitude)}",
            style = MaterialTheme.typography.bodyMedium,
            color = primaryColor,
            fontWeight = FontWeight.Normal
        )

        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            tint = primaryColor.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(start = 4.dp)
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

private fun createMarkerBitmap(color: Int): Bitmap {
    val size = 60 // Чуть меньше для превью
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Рисуем каплю
    paint.color = color
    val path = Path()
    path.moveTo(size / 2f, size.toFloat())
    path.cubicTo(0f, size * 0.6f, 0f, 0f, size / 2f, 0f)
    path.cubicTo(size.toFloat(), 0f, size.toFloat(), size * 0.6f, size / 2f, size.toFloat())
    canvas.drawPath(path, paint)

    // Рисуем белый круг внутри
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 3f, size / 6f, paint)

    return bitmap
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

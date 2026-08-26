package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.R
import com.example.fishing.ui.components.FishingBadge
import com.example.fishing.ui.theme.FishingTheme
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
internal fun WaterSection(
    waterName: String,
    onArrowClick: () -> Unit,
    onEditClick: () -> Unit = {},
    location: GeoPoint? = null,
    fishingFromShore: Boolean = true,
    isPaidWater: Boolean = false,
    isRequired: Boolean = false,
) {
    val hasData = (location != null) || waterName.isNotBlank()
    
    SectionCard(contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 0.dp)) {

        Section(
            title = stringResource(R.string.water_body),
            hasData = hasData,
            isRequired = isRequired,
            onArrowClick = onArrowClick,
        )
        
        if (hasData) {
            MapPreview(location = location)
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name and Coordinates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = waterName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (location != null) {
                            Text(
                                text = "${"%.5f".format(location.latitude)} - ${"%.5f".format(location.longitude)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (location != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onEditClick) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPaidWater) {
                        FishingBadge(
                            text = stringResource(R.string.paid)
                        )
                    }
                    FishingBadge(
                        text = stringResource(
                            if (fishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun MapPreview(
    location: GeoPoint?,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(114.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFBFE3EA))
    ) {
        if (location != null && !isPreview) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false)
                        isClickable = false
                        isFocusable = false
                        setOnTouchListener { v, _ -> 
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            v.performClick()
                            true 
                        }
                        controller.setZoom(15.0)
                        controller.setCenter(location)
                    }
                },
                update = { mapView ->
                    mapView.controller.setCenter(location)
                },
                modifier = Modifier.matchParentSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFB8E4EC),
                                Color(0xFFEAF4D0),
                                Color(0xFFCDE8B8)
                            )
                        )
                    )
            )
        }

        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true, name = "Без данных")
@Composable
private fun WaterSectionEmptyPreview() {
    FishingTheme {
        WaterSection(
            waterName = "",
            onArrowClick = {}
        )
    }
}

@Preview(showBackground = true, name = "С данными")
@Composable
private fun WaterSectionWithDataPreview() {
    FishingTheme {
        WaterSection(
            waterName = "Озеро Байкал",
            onArrowClick = {},
            fishingFromShore = true,
            isPaidWater = false,
        )
    }
}

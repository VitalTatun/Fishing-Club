package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.ui.theme.FishingTheme
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

@Composable
internal fun WaterSection(
    waterName: String,
    onWaterNameChange: (String) -> Unit,
    onArrowClick: () -> Unit,
    location: GeoPoint? = null,
    fishingFromShore: Boolean = true,
    onFishingFromShoreChange: (Boolean) -> Unit = {},
    isPaidWater: Boolean = false,
    onPaidWaterChange: (Boolean) -> Unit = {},
) {
    val hasData = (location != null) || waterName.isNotBlank()
    
    SectionCard(contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 0.dp)) {

        Section(
            title = "Водоем*",
            hasData = hasData,
            onArrowClick = onArrowClick,
        )
        
        if (hasData) {
            MapPreview(location = location)
            Spacer(Modifier.height(16.dp))
            ReportTextField(
                value = waterName,
                onValueChange = onWaterNameChange,
                label = "Название водоема *"
            )
            SwitchRow(
                title = "Ловля с берега",
                checked = fishingFromShore,
                onCheckedChange = onFishingFromShoreChange
            )
            SwitchRow(
                title = "Платный водоем",
                checked = isPaidWater,
                onCheckedChange = onPaidWaterChange
            )
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(114.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFBFE3EA))
    ) {
        if (location != null) {
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
            onWaterNameChange = {},
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
            onWaterNameChange = {},
            onArrowClick = {},
            fishingFromShore = true,
            onFishingFromShoreChange = {},
            isPaidWater = false,
            onPaidWaterChange = {}
        )
    }
}

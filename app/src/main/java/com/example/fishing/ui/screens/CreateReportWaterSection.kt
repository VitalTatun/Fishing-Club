package com.example.fishing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
) {
    val hasData = (location != null) || waterName.isNotBlank()
    
    SectionCard(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (hasData) 20.dp else 0.dp)) {
        val subtitle = if (location != null) {
            String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
        } else {
            "Обязательное"
        }
        
        SectionHeader(
            title = "Водоем*",
            subtitle = subtitle,
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
        }
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

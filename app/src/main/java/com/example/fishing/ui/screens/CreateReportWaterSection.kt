package com.example.fishing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun WaterSection(
    waterName: String,
    onWaterNameChange: (String) -> Unit
) {
    SectionCard(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
        SectionHeader(title = "Водоем*", subtitle = "Координаты не указаны")
        MapPreview()
        Spacer(Modifier.height(16.dp))
        ReportTextField(
            value = waterName,
            onValueChange = onWaterNameChange,
            label = "Название водоема *"
        )
    }
}

@Composable
private fun MapPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(114.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFBFE3EA))
    ) {
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
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .fillMaxWidth(0.7f)
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 26.dp, bottom = 18.dp)
                .width(112.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.72f))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp),
            shape = CircleShape,
            color = Color(0xFF445E91),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.padding(9.dp)
            )
        }
    }
}

package com.example.fishing.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyBadge(
    isPublic: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPublic) {
        Surface(
            modifier = modifier.size(32.dp),
            shape = CircleShape,
            color = Color(0xFF3E5481)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Private",
                tint = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

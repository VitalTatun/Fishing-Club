package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.ui.theme.FishingTheme

@Composable
fun FishingBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrophyBadge(modifier: Modifier = Modifier) {
    FishingBadge(
        text = "Трофей",
        containerColor = Color(0xFFFFD71D), // FFD71D
        contentColor = Color(0xFF50250A),   // 50250A
        modifier = modifier
    )
}

@Composable
fun DraftBadge(modifier: Modifier = Modifier) {
    FishingBadge(
        text = "Не опубликован",
        containerColor = Color(0xFFD8E2FF), // D8E2FF
        contentColor = Color(0xFF2C4678),   // 2C4678
        modifier = modifier
    )
}

@Composable
fun PublishedBadge(modifier: Modifier = Modifier) {
    FishingBadge(
        text = "Опубликован",
        containerColor = Color(0xFFDCEDC8),
        contentColor = Color(0xFF689F38),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun BadgesPreview() {
    FishingTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrophyBadge()
            DraftBadge()
            FishingBadge(text = "Ловля с берега")
            FishingBadge(text = "Платный")
            PublishedBadge()
        }
    }
}

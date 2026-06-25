package com.example.fishing.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val moodIcons = listOf(
    Icons.Default.SentimentVeryDissatisfied,
    Icons.Default.SentimentDissatisfied,
    Icons.Default.SentimentNeutral,
    Icons.Default.SentimentSatisfied,
    Icons.Default.SentimentVerySatisfied,
)

@Composable
internal fun MoodSection(
    selectedMood: Int,
    onMoodChange: (Int) -> Unit,
) {
    SectionCard(contentPadding = PaddingValues(0.dp)) {
        SectionHeader(
            title = "Эмоциональная оценка",
            showArrow = false,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodIcons.forEachIndexed { index, icon ->
                    MoodIcon(
                        icon = icon,
                        isSelected = selectedMood == index + 1,
                        onClick = { onMoodChange(index + 1) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Очень плохая",
                    color = Color(0xFF3E5481),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Очень хорошая",
                    color = Color(0xFF3E5481),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MoodIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Color(0xFF3E5481) else CreateReportColors.OnSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    )
}

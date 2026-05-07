package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MoodSection(selectedMood: Int, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Эмоциональная оценка",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3E5481)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MoodIcon(Icons.Default.SentimentVeryDissatisfied, selectedMood == 1)
                MoodIcon(Icons.Default.SentimentDissatisfied, selectedMood == 2)
                MoodIcon(Icons.Default.SentimentNeutral, selectedMood == 3)
                MoodIcon(Icons.Default.SentimentSatisfied, selectedMood == 4)
                MoodIcon(Icons.Default.SentimentVerySatisfied, selectedMood == 5)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Очень плохая",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3E5481)
                )
                Text(
                    text = "Очень хорошая",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3E5481)
                )
            }
        }
    }
}

@Composable
private fun MoodIcon(icon: ImageVector, isSelected: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Color(0xFFFF5722) else Color(0xFF8E99BA).copy(alpha = 0.7f),
        modifier = Modifier.size(44.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun MoodSectionPreview() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Выбрано: Очень плохо (1)")
        MoodSection(selectedMood = 1)
        
        Text("Выбрано: Нейтрально (3)")
        MoodSection(selectedMood = 3)
        
        Text("Выбрано: Отлично (5)")
        MoodSection(selectedMood = 5)
    }
}

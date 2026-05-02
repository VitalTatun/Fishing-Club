package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.util.*

@Composable
fun ReportHeader(report: FishingReport, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Теги (Верхняя строка)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                text = report.type.displayName,
                containerColor = Color(0xFFE8EAF6),
                contentColor = Color(0xFF3F51B5)
            )
            if (report.isPublic) {
                StatusChip(
                    text = "Опубликован",
                    containerColor = Color(0xFFDCEDC8),
                    contentColor = Color(0xFF689F38)
                )
            } else {
                StatusChip(
                    text = "Черновик",
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = Color(0xFF757575)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Инфо об отчете и авторе (Аватар + Вертикальный стек текстов)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Аватар пользователя
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }


            // Вертикальный стек (Название + Имя)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.user.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            if (!report.isPublic) {
                IconButton(onClick = { } ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Опубликовать",
                    )
                }
            }
            IconButton(onClick = { } ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Опубликовать",
                )
            }
        }

        // 3. Баннер публикации (если черновик)
        if (!report.isPublic) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF3E5481)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отчет не опубликован",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Об этой рыбалке знаете только вы и рыба",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportHeaderPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = listOf(),
            fishingTime = Date(),
            weight = 3.2,
            fish = listOf(),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(),
            comment = "",
            user = User(name = "Никита Белозерцев", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = false
        )
        Column(modifier = Modifier.padding(16.dp)) {
            ReportHeader(report = sampleReport)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ReportHeader(report = sampleReport.copy(isPublic = true))
        }
    }
}

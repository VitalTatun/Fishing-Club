package com.example.fishing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapReportSheetContent(
    report: FishingReport,
    repository: FishingRepository,
    onPhotoClick: (Int) -> Unit = {},
    onMapClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Шапка отчёта (Фото карусель + Заголовок, Дата, Статус)
        ReportHeader(report = report, repository = repository)

        MoodSection(selectedMood = 5)

        // Баннер публикации (если черновик)
        if (!report.isPublic) {
            PublishBanner()
        }

        // 4. Секция местоположения
        ReportLocationSection(
            report = report,
            onMapClick = onMapClick
        )

        // Общая информация (Mood, Details, Catch)
        ReportGeneralInfo(report = report)
    }
}

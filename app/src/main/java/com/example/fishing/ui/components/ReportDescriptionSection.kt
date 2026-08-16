package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDescriptionSection(
    report: FishingReport,
    modifier: Modifier = Modifier,
    forceShowExpand: Boolean = false
) {
    if (report.comment.isBlank()) return

    var showSheet by remember { mutableStateOf(false) }
    var isOverflowed by remember { mutableStateOf(forceShowExpand) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = report.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { 
                    if (!forceShowExpand) {
                        isOverflowed = it.hasVisualOverflow 
                    }
                }
            )
            if (isOverflowed) {
                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF)
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .clickable { showSheet = true }
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = report.comment,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportDescriptionSectionPreview() {
    val sampleReport = FishingReport(
        userId = UUID.randomUUID(),
        type = FishingType.FISHING_LOG,
        name = "Тестовый отчет",
        water = Water(waterName = "Озеро", latitude = 0.0, longitude = 0.0),
        photo = listOf(),
        fishingTime = Date(),
        weight = 5.4,
        fish = listOf(),
        fishingMethod = FishingMethod.FEEDER,
        bait = listOf(),
        comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб! Замешав вечерком плотву с орехом от Feeder.by с утра поехал на мелководную часть вдх посмотреть как там обстоят дела с рыбкой. Место выбрал перспективное, глубина около полутора метров, дно песчано-илистое. Первые поклевки начались уже через полчаса после закорма. Сначала подошла мелкая плотва, но ближе к полудню проклюнулся и подлещик. Погода радовала, штиль и теплое июльское солнце создавали идеальную атмосферу. В итоге удалось поймать около пяти килограмм разнорыбицы, среди которых было несколько действительно достойных экземпляров. Обязательно вернусь сюда снова, чтобы проверить еще несколько точек на этом участке.",
        user = User(name = "Виталий", image = "", email = ""),
        fishingFromTheShore = true,
        isPublic = true
    )
    Box(
        modifier = Modifier
            .width(360.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportDescriptionSection(report = sampleReport, forceShowExpand = true)
    }
}


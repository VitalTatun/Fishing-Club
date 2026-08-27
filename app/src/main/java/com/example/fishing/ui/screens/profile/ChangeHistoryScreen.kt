package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme

data class VersionChanges(
    val version: String,
    val date: String,
    val changes: List<String>
)

val history = listOf(
    VersionChanges(
        version = "1.0.1-alpha",
        date = "27 августа 2026",
        changes = listOf(
            "Добавлена автоматическая синхронизация версии приложения.",
            "Реализовано динамическое получение версии в настройках.",
            "Подготовлена инфраструктура для автоматического ведения истории изменений."
        )
    ),
    VersionChanges(
        version = "1.0.0-alpha",
        date = "27 августа 2026",
        changes = listOf(
            "Полностью обновлен экран настроек профиля.",
            "Добавлено подтверждение при выходе из профиля для предотвращения случайных нажатий.",
            "Улучшен процесс редактирования имени и фото профиля.",
            "Оптимизирован интерфейс заголовка профиля.",
            "Добавлен раздел «История изменений»."
        )
    ),
    VersionChanges(
        version = "0.9.0 Alpha",
        date = "15 августа 2026",
        changes = listOf(
            "Первый запуск тестовой версии приложения.",
            "Реализована карта с рыболовными местами.",
            "Добавлена возможность создания отчетов об уловах.",
            "Базовая авторизация и создание профиля."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeHistoryScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История изменений") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(history) { item ->
                VersionItem(item)
            }
        }
    }
}

@Composable
fun VersionItem(item: VersionChanges) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.version,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        item.changes.forEach { change ->
            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangeHistoryScreenPreview() {
    FishingTheme {
        ChangeHistoryScreen(onBackClick = {})
    }
}

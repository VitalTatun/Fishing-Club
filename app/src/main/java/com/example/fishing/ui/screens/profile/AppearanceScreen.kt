package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.ReportDisplayMode
import com.example.fishing.ui.theme.FishingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    currentMode: ReportDisplayMode,
    onModeSelected: (ReportDisplayMode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Оформление") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Отображение отчетов на главном экране",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Вариант "Список"
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (currentMode == ReportDisplayMode.LIST),
                        onClick = { onModeSelected(ReportDisplayMode.LIST) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (currentMode == ReportDisplayMode.LIST),
                    onClick = null // null рекомендован при использовании Modifier.selectable
                )
                Text(
                    text = "Список",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Вариант "Карточка"
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (currentMode == ReportDisplayMode.CARD),
                        onClick = { onModeSelected(ReportDisplayMode.CARD) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (currentMode == ReportDisplayMode.CARD),
                    onClick = null
                )
                Text(
                    text = "Карточка",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceScreenPreview() {
    FishingTheme {
        AppearanceScreen(
            currentMode = ReportDisplayMode.CARD,
            onModeSelected = {},
            onBackClick = {}
        )
    }
}

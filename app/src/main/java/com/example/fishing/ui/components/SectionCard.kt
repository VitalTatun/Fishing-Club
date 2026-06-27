package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme

sealed class SectionEntry {

    data class TextItem(
        val label: String,
        val value: String,
        val valueColor: Color? = null,
        val onClick: (() -> Unit)? = null,
        val padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) : SectionEntry()

    class ComposableItem(
        val padding: PaddingValues = PaddingValues(0.dp),
        val content: @Composable () -> Unit
    ) : SectionEntry()
}

@Composable
fun SectionCard(
    title: String,
    items: List<SectionEntry>,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
    containerColor: Color = FishingTheme.colors.secondaryBackground,
    cornerRadius: Dp = 16.dp,
    gap: Dp = 2.dp,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    onTitleClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = titleModifier
                .fillMaxWidth()
                .clickable(enabled = onTitleClick != null, onClick = onTitleClick ?: {})
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            items.forEach { entry ->
                when (entry) {
                    is SectionEntry.TextItem -> {
                        val resolvedColor = entry.valueColor
                            ?: MaterialTheme.colorScheme.onSurface

                        val rowModifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = gap / 2)
                            .clip(RoundedCornerShape(4.dp))
                            .background(containerColor)
                            .padding(entry.padding)

                        val clickableModifier = if (entry.onClick != null) {
                            rowModifier.clickable { entry.onClick.invoke() }
                        } else {
                            rowModifier
                        }

                        Row(
                            modifier = clickableModifier,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.label, style = labelStyle)
                            Text(
                                text = entry.value,
                                color = resolvedColor,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is SectionEntry.ComposableItem -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = gap / 2)
                                .background(containerColor)
                                .padding(entry.padding)
                        ) {
                            entry.content()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionCardPreview() {
    FishingTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionCard(
                title = "Общая информация",
                items = listOf(
                    SectionEntry.TextItem("Способ ловли", "Поплавок"),
                    SectionEntry.TextItem("Наживка", "Мотыль, Опарыш"),
                    SectionEntry.TextItem("Дата", "14 августа 2023 • 07:00")
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(
                title = "Улов",
                items = listOf(
                    SectionEntry.TextItem("Карась", "2 шт."),
                    SectionEntry.TextItem("Окунь", "2 шт."),
                    SectionEntry.TextItem("Общий вес", "3,2 кг")
                )
            )
        }
    }
}

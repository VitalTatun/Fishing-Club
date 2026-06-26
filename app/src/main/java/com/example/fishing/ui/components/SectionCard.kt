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

data class SectionItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
    val valueFontWeight: FontWeight? = null,
    val valueStyle: TextStyle? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SectionCard(
    title: String,
    items: List<SectionItem>,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
    containerColor: Color = FishingTheme.colors.secondaryBackground,
    cornerRadius: Dp = 16.dp,
    rowPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    rowSpacing: Dp = 2.dp,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
    onTitleClick: (() -> Unit)? = null,
    beforeItems: (@Composable ColumnScope.() -> Unit)? = null
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
                .clip(RoundedCornerShape(cornerRadius)),
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            if (beforeItems != null) {
                beforeItems()
            }

            items.forEachIndexed { index, item ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(
                        topStart = if (beforeItems != null) 4.dp else cornerRadius,
                        topEnd = if (beforeItems != null) 4.dp else cornerRadius,
                        bottomStart = 4.dp, bottomEnd = 4.dp
                    )
                    items.size - 1 -> RoundedCornerShape(
                        topStart = 4.dp, topEnd = 4.dp,
                        bottomStart = cornerRadius, bottomEnd = cornerRadius
                    )
                    else -> RoundedCornerShape(4.dp)
                }

                val rowModifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(containerColor)
                    .padding(rowPadding)

                val clickableModifier = if (item.onClick != null) {
                    rowModifier.clickable { item.onClick?.invoke() }
                } else {
                    rowModifier
                }

                val resolvedColor = item.valueColor ?: MaterialTheme.colorScheme.onSurface
                val resolvedWeight = item.valueFontWeight ?: FontWeight.Medium
                val resolvedStyle = item.valueStyle ?: MaterialTheme.typography.bodyMedium

                Row(
                    modifier = clickableModifier,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.label, style = labelStyle)
                    Text(
                        text = item.value,
                        color = resolvedColor,
                        fontWeight = resolvedWeight,
                        style = resolvedStyle
                    )
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
                    SectionItem("Способ ловли", "Поплавок"),
                    SectionItem("Наживка", "Мотыль, Опарыш"),
                    SectionItem("Дата", "14 августа 2023 • 07:00")
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(
                title = "Улов",
                items = listOf(
                    SectionItem("Карась", "2 шт."),
                    SectionItem("Окунь", "2 шт."),
                    SectionItem("Общий вес", "3,2 кг")
                )
            )
        }
    }
}

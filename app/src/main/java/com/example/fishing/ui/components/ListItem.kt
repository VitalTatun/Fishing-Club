package com.example.fishing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme

@Composable
fun FishingListItem(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    onRowClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onTrailingTextClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = 48.dp)
            .then(
                if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier
            )
            .padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Иконка слева
        Box(
            modifier = Modifier
                .width(24.dp)
                .heightIn(max = 68.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Текстовый блок
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.then(
                        if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier
                    )
                )
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.then(
                            if (onTrailingTextClick != null) Modifier.clickable(onClick = onTrailingTextClick) else Modifier
                        )
                    )
                }
            }
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Контент справа
        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .heightIn(max = 68.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                trailingContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FishingListItemPreview() {
    FishingTheme {
        Column {
            FishingListItem(
                title = "С иконкой",
                trailingText = "Значение",
                supportingText = "Дополнительное описание",
                leadingIcon = Icons.Default.Star,
                trailingContent = {
                    Switch(checked = true, onCheckedChange = { })
                }
            )
            FishingListItem(
                title = "Без иконки (отступ сохранен)",
                trailingText = "Значение",
                supportingText = "Текст выровнен по вертикали с элементом выше"
            )
        }
    }
}

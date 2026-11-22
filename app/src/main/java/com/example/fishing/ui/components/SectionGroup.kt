package com.example.fishing.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme

/**
 * Figma `SectionGroup`: an optional 40dp title, a list with 8dp vertical
 * padding and an optional 1dp outline divider below it.
 *
 * A null [title] is the Compose equivalent of the Figma `showSectionTitle`
 * switch; [showDivider] mirrors `showDivider`.
 */
@Composable
fun SectionGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            content = content
        )

        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 432)
@Composable
private fun SectionGroupPreview() {
    FishingTheme {
        Column {
            SectionGroup(title = "Section Title") {
                FishingListItem(
                    title = "Элемент списка 1",
                    trailingText = "Значение",
                    supportingText = "Дополнительное описание",
                    leadingIcon = Icons.Default.Add,
                    trailingContent = {
                        Switch(checked = true, onCheckedChange = { })
                    }
                )
                FishingListItem(title = "Элемент списка 2")
            }

            SectionGroup(title = "Без разделителя", showDivider = false) {
                FishingListItem(title = "Элемент списка 3")
            }

            SectionGroup {
                FishingListItem(title = "Секция без заголовка")
            }
        }
    }
}

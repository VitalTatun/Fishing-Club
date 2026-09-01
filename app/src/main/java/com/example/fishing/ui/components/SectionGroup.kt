package com.example.fishing.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch


@Composable
fun SectionGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        content()
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionGroupPreview() {
    FishingTheme {
        Column() {
            SectionGroup {
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
        }
    }
}

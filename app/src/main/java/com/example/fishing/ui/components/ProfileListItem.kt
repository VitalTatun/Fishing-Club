package com.example.fishing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.fishing.ui.theme.FishingTheme

@Composable
fun ProfileListItem(
    modifier: Modifier = Modifier,
    label: String? = null,
    value: String? = null,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        overlineContent = if (label != null) {
            {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        headlineContent = {
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        trailingContent = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileListItemPreview() {
    FishingTheme {
        Column {
            ProfileListItem(
                label = "Имя",
                value = "Никита Белозерцев"
            ) { }
            ProfileListItem(
                label = "Электронная почта",
                value = "nikita.bel@gmail.com",
                trailingIcon = null
            )
            ProfileListItem(
                label = "Версия приложения",
                value = "1.0.0 Alpha",
                trailingIcon = null
            )
            ProfileListItem(
                value = "Изменить пароль"
            ) { }
        }
    }
}

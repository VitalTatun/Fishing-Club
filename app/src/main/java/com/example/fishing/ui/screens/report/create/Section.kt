package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.ui.theme.FishingTheme

@Composable
internal fun Section(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hasData: Boolean = false,
    onAddClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = CreateReportColors.OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = CreateReportColors.OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (onAddClick != null) {
            FilledTonalIconButton(
                onClick = onAddClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (hasData) Icons.Default.ChevronRight else Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionPreview() {
    FishingTheme {
        Section(
            title = "Фотографии",
            onAddClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionWithDataPreview() {
    FishingTheme {
        Section(
            title = "Фотографии",
            hasData = true,
            onAddClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionWithSubtitlePreview() {
    FishingTheme {
        Section(
            title = "Фотографии",
            subtitle = "Добавьте фото вашего улова",
            onAddClick = {}
        )
    }
}

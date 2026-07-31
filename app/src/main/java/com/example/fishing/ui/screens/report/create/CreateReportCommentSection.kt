package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun CommentSection(
    comment: String = "",
    onArrowClick: () -> Unit = {}
) {
    SectionCard(contentPadding = PaddingValues(horizontal = 0.dp)) {
        Section(
            title = "Комментарий",
            hasData = comment.isNotBlank(),
            onArrowClick = onArrowClick
        )
        if (comment.isNotBlank()) {
            Text(
                text = comment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                color = CreateReportColors.OnSurface,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

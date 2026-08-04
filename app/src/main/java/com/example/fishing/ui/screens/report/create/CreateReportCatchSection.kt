package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.Fish
import com.example.fishing.ui.theme.FishingTheme

@Composable
internal fun CatchSection(
    selectedFish: List<Fish> = emptyList(),
    onArrowClick: () -> Unit = {},
    weight: Float = 0f,
    isRequired: Boolean = false
) {
    SectionCard(
        contentPadding = PaddingValues(
            start = 0.dp
        )
    ) {
        Section(
            title = stringResource(R.string.catch_label),
            hasData = selectedFish.isNotEmpty(),
            isRequired = isRequired,
            onArrowClick = onArrowClick
        )
        if (selectedFish.isNotEmpty()) {
            selectedFish.forEachIndexed { index, fish ->
                InfoRow(
                    label = fish.name,
                    value = stringResource(R.string.fish_count_short, fish.count),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
                )
                if (index < selectedFish.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = CreateReportColors.Divider
                    )
                }
            }
            if (weight > 0f) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = CreateReportColors.Divider
                )
                InfoRow(
                    label = stringResource(R.string.total_weight),
                    value = "${(weight * 10).toInt() / 10f} ${stringResource(R.string.kg)}",
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true, name = "Без данных")
@Composable
private fun CatchSectionEmptyPreview() {
    FishingTheme {
        CatchSection()
    }
}

@Preview(showBackground = true, name = "С данными")
@Composable
private fun CatchSectionWithDataPreview() {
    FishingTheme {
        CatchSection(
            selectedFish = listOf(
                Fish(name = "Окунь", count = 3),
                Fish(name = "Щука", count = 1)
            ),
            weight = 2.5f
        )
    }
}

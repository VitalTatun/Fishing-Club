package com.example.fishing.ui.screens.report.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.fishing.ui.theme.FishingTheme

@PreviewTest
@Preview(name = "400x400", widthDp = 400, heightDp = 400)
@Preview(name = "400x500", widthDp = 400, heightDp = 500)
@Preview(name = "400x1000", widthDp = 400, heightDp = 1000)
@Preview(name = "610x400", widthDp = 610, heightDp = 400)
@Preview(name = "610x500", widthDp = 610, heightDp = 500)
@Preview(name = "610x1000", widthDp = 610, heightDp = 1000)
@Preview(name = "900x400", widthDp = 900, heightDp = 400)
@Preview(name = "900x500", widthDp = 900, heightDp = 500)
@Preview(name = "900x1000", widthDp = 900, heightDp = 1000)
@Composable
fun FishingDateTimeScreenResponsivePreviewTest() {
    FishingTheme(dynamicColor = false) {
        FishingDateTimeScreen()
    }
}

@PreviewTest
@Preview(
    name = "Foldable inside",
    widthDp = 673,
    heightDp = 841,
    showBackground = true,
)
@Composable
fun FishingDateTimeScreenFoldablePreview() {
    FishingTheme(dynamicColor = false) {
        FishingDateTimeScreen(
            startDateText = "25 июл. 2026",
            startTimeText = "04:00",
            endDateText = "26 июл. 2026",
            endTimeText = "12:00",
        )
    }
}

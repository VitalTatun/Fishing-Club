package com.example.fishing.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class ReportField(val id: String) {
    
    data class ListItemField(
        val fieldId: String,
        val title: String,
        val overline: String? = null,
        val supportingText: String? = null,
        val leadingIcon: ImageVector? = null,
        val trailingText: String? = null,
        val isRequired: Boolean = false,
        val onClick: () -> Unit = {},
        val onTrailingTextClick: (() -> Unit)? = null
    ) : ReportField(fieldId)

    data class ToggleField(
        val fieldId: String,
        val title: String,
        val supportingText: String? = null,
        val leadingIcon: ImageVector? = null,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : ReportField(fieldId)

    data class CustomField(
        val fieldId: String,
        val isRequired: Boolean = false
    ) : ReportField(fieldId)

    data class PhotoPicker(
        val isRequired: Boolean = false
    ) : ReportField("photos")

    object MapPreview : ReportField("map")

    data class FishList(
        val isRequired: Boolean = false
    ) : ReportField("fish_list")
}

data class ReportFormSection(
    val id: String,
    val items: List<ReportField>
)

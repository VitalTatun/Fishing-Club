package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportField
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.viewmodel.CreateReportViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportFieldRenderer(
    field: ReportField,
    viewModel: CreateReportViewModel,
    onNavigateToWaterEdit: () -> Unit,
    onNavigateToWaterNameEdit: () -> Unit,
    onNavigateToMethodAndBaitEdit: () -> Unit,
    onNavigateToCatchEdit: () -> Unit,
    onNavigateToCommentEdit: () -> Unit,
    onDatePickerClick: (String) -> Unit,
    onTimePickerClick: (String) -> Unit,
    onPhotoPickerClick: () -> Unit,
    isDetailsExpanded: Boolean,
    onDetailsExpandClick: () -> Unit,
    haptic: HapticFeedback,
) {
    when (field) {
        is ReportField.ListItemField -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                FishingListItem(
                    overlineText = field.overline,
                    title = field.title,
                    isRequired = field.isRequired,
                    supportingText = field.supportingText,
                    leadingIcon = field.leadingIcon,
                    trailingText = field.trailingText,
                    isError = field.isError,
                    onRowClick = when {
                        field.fieldId == "water_body" -> { { onNavigateToWaterEdit() } }
                        field.fieldId == "add_water_name" || field.fieldId == "water_name" -> { { onNavigateToWaterNameEdit() } }
                        field.fieldId == "method" || field.fieldId == "fishing_method" || field.fieldId == "baits" -> { { onNavigateToMethodAndBaitEdit() } }
                        field.fieldId == "catch" || field.fieldId == "weight" -> { { onNavigateToCatchEdit() } }
                        field.fieldId == "comment" -> { { onNavigateToCommentEdit() } }
                        else -> null
                    },
                    onTitleClick = when (field.fieldId) {
                        "date_time" -> { { onDatePickerClick("start") } }
                        "date_time_end" -> { { onDatePickerClick("end") } }
                        else -> null
                    },
                    onTrailingTextClick = when (field.fieldId) {
                        "date_time" -> { { onTimePickerClick("start") } }
                        "date_time_end" -> { { onTimePickerClick("end") } }
                        else -> null
                    },
                    modifier = Modifier.then(
                        if (field.fieldId == "water_name" || field.fieldId == "baits" || field.fieldId == "fishing_method" || field.fieldId == "weight" || (field.fieldId == "date_time_end" && !field.isError)) {
                            Modifier.padding(start = 40.dp)
                        } else {
                            Modifier
                        }
                    )
                )

                if (field.fieldId == "catch" && viewModel.formSelectedFish.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.formSelectedFish.forEach { fish ->
                            CatchFishChip(
                                text = "${fish.name} ${stringResource(R.string.fish_count_short, fish.count)}"
                            )
                        }
                    }
                }
            }
        }

        is ReportField.ToggleField -> {
            FishingListItem(
                title = field.title,
                supportingText = field.supportingText,
                leadingIcon = field.leadingIcon,
                trailingContent = {
                    Switch(
                        checked = field.checked,
                        onCheckedChange = field.onCheckedChange
                    )
                }
            )
        }

        is ReportField.CustomField -> {
            when (field.fieldId) {
                "report_type" -> {
                    ReportTypeSelector(
                        reportType = viewModel.formReportType,
                        onReportTypeChange = { newType ->
                            viewModel.formReportType = newType
                            if (newType == FishingType.HAUL && viewModel.formSelectedFish.size > 1) {
                                viewModel.formSelectedFish =
                                    listOf(viewModel.formSelectedFish.first().copy(count = 1))
                            }
                        },
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                "water_details_header" -> {
                    val shoreText =
                        stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat)
                    val paidText =
                        if (viewModel.formIsPaidWater) " • ${stringResource(R.string.paid)}" else ""
                    FishingListItem(
                        title = "Детали",
                        supportingText = if (!isDetailsExpanded) "$shoreText$paidText" else null,
                        onRowClick = { onDetailsExpandClick() },
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }

                "water_details" -> {
                    if (isDetailsExpanded) {
                        WaterDetailsItems(viewModel, haptic)
                    }
                }
            }
        }

        is ReportField.PhotoPicker -> {
            FishingListItem(
                title = stringResource(R.string.photos),
                isRequired = field.isRequired,
                supportingText = stringResource(R.string.photos_subtitle),
                leadingIcon = Icons.Default.AddPhotoAlternate,
                onRowClick = { onPhotoPickerClick() }
            )

            if (viewModel.formPhotos.isNotEmpty()) {
                ReportPhotosList(
                    selectedPhotos = viewModel.formPhotos,
                    onRemoveClick = { photo ->
                        viewModel.formPhotos = viewModel.formPhotos - photo
                    }
                )
            }
        }

        ReportField.MapPreview -> {
            viewModel.formLocation?.let {
                MapPreview(
                    location = it,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        is ReportField.ErrorField -> {
            Text(
                text = field.text,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun CatchFishChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun WaterDetailsItems(
    viewModel: CreateReportViewModel,
    haptic: HapticFeedback
) {
    FishingListItem(
        title = stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat),
        trailingContent = {
            Switch(
                checked = viewModel.formFishingFromShore,
                onCheckedChange = {
                    viewModel.formFishingFromShore = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        },
        modifier = Modifier.padding(start = 40.dp)
    )
    FishingListItem(
        title = stringResource(R.string.paid_water),
        trailingContent = {
            Switch(
                checked = viewModel.formIsPaidWater,
                onCheckedChange = {
                    viewModel.formIsPaidWater = it
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            )
        },
        modifier = Modifier.padding(start = 40.dp)
    )
}

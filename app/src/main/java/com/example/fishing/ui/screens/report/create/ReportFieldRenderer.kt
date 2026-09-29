package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.MaterialTheme
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
            FishingListItem(
                overlineText = field.overline,
                title = field.title,
                isRequired = field.isRequired,
                supportingText = field.supportingText,
                leadingIcon = field.leadingIcon,
                trailingText = field.trailingText,
                isError = field.isError,
                onRowClick = when (field.fieldId) {
                    "water_body" -> { { onNavigateToWaterEdit() } }
                    "add_water_name", "water_name" -> { { onNavigateToWaterNameEdit() } }
                    "method" -> { { onNavigateToMethodAndBaitEdit() } }
                    "comment" -> { { onNavigateToCommentEdit() } }
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
                    if (field.fieldId == "water_name" || field.fieldId == "baits" || field.fieldId == "weight" || (field.fieldId == "date_time_end" && !field.isError)) {
                        Modifier.padding(start = 40.dp)
                    } else {
                        Modifier
                    }
                )
            )
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

        is ReportField.FishList -> {
            val hasCatch = viewModel.formSelectedFish.isNotEmpty()
            val firstFish = viewModel.formSelectedFish.firstOrNull()
            FishingListItem(
                title = (if (hasCatch && firstFish != null) firstFish.name
                else stringResource(R.string.catch_label)),
                isRequired = field.isRequired,
                leadingIcon = Icons.Default.SetMeal,
                trailingText = if (hasCatch && firstFish != null) {
                    stringResource(R.string.fish_count_short, firstFish.count)
                } else null,
                onRowClick = { onNavigateToCatchEdit() }
            )

            if (viewModel.formSelectedFish.size > 1) {
                viewModel.formSelectedFish.drop(1).forEach { fish ->
                    FishingListItem(
                        title = fish.name,
                        trailingText = stringResource(R.string.fish_count_short, fish.count),
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }
            }
        }
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

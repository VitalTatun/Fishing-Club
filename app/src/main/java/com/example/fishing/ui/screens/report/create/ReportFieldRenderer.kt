package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportField
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
    onDatePickerClick: () -> Unit,
    onTimePickerClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    isDetailsExpanded: Boolean,
    onDetailsExpandClick: () -> Unit,
    haptic: HapticFeedback,
) {
    when (field) {
        is ReportField.ListItemField -> {
            ListItem(
                overlineContent = field.overline?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
                headlineContent = {
                    Text(
                        text = field.title + if (field.isRequired) " *" else "",
                        modifier = if (field.fieldId == "date_time") Modifier.clickable { onDatePickerClick() } else Modifier
                    )
                },
                supportingContent = field.supportingText?.let { { Text(it) } },
                leadingContent = field.leadingIcon?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingContent = field.trailingText?.let {
                    {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = if (field.fieldId == "date_time") Modifier.clickable { onTimePickerClick() } else Modifier
                        )
                    }
                },
                modifier = Modifier
                    .then(
                        if (field.fieldId == "water_name" || field.fieldId == "baits" || field.fieldId == "weight" || field.fieldId == "placeholder_date") {
                            Modifier.padding(start = 40.dp)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        when (field.fieldId) {
                            "water_body" -> Modifier.clickable { onNavigateToWaterEdit() }
                            "add_water_name", "water_name" -> Modifier.clickable { onNavigateToWaterNameEdit() }
                            "method" -> Modifier.clickable { onNavigateToMethodAndBaitEdit() }
                            "comment" -> Modifier.clickable { onNavigateToCommentEdit() }
                            else -> Modifier
                        }
                    )
            )
        }

        is ReportField.ToggleField -> {
            ListItem(
                headlineContent = { Text(field.title) },
                supportingContent = field.supportingText?.let { { Text(it) } },
                leadingContent = field.leadingIcon?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                    ListItem(
                        headlineContent = { Text("Детали") },
                        supportingContent = if (!isDetailsExpanded) {
                            { Text("$shoreText$paidText") }
                        } else null,
                        modifier = Modifier
                            .padding(start = 40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDetailsExpandClick() }
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
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.photos) + if (field.isRequired) " *" else "")
                },
                supportingContent = { Text(stringResource(R.string.photos_subtitle)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { onPhotoPickerClick() }
            )

            if (viewModel.formSelectedPhotoUris.isNotEmpty()) {
                ReportPhotosList(
                    selectedPhotoUris = viewModel.formSelectedPhotoUris,
                    onRemoveClick = { uri ->
                        viewModel.formSelectedPhotoUris -= uri
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

        is ReportField.FishList -> {
            val hasCatch = viewModel.formSelectedFish.isNotEmpty()
            val firstFish = viewModel.formSelectedFish.firstOrNull()
            ListItem(
                headlineContent = {
                    Text(
                        text = (if (hasCatch && firstFish != null) firstFish.name
                        else stringResource(R.string.catch_label)) + if (field.isRequired) " *" else ""
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SetMeal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = if (hasCatch && firstFish != null) {
                    {
                        Text(
                            text = stringResource(R.string.fish_count_short, firstFish.count),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else null,
                modifier = Modifier.clickable { onNavigateToCatchEdit() }
            )

            if (viewModel.formSelectedFish.size > 1) {
                viewModel.formSelectedFish.drop(1).forEach { fish ->
                    ListItem(
                        headlineContent = { Text(fish.name) },
                        trailingContent = {
                            Text(
                                text = stringResource(R.string.fish_count_short, fish.count),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
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
    ListItem(
        headlineContent = {
            Text(stringResource(if (viewModel.formFishingFromShore) R.string.fishing_from_shore else R.string.fishing_from_boat))
        },
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
    ListItem(
        headlineContent = { Text(stringResource(R.string.paid_water)) },
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

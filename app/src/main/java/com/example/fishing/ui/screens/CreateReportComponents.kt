package com.example.fishing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal object CreateReportColors {
    val ScreenBackground = Color(0xFFF7F7F5)
    val Surface = Color.White
    val OnSurface = Color(0xFF1A1B20)
    val OnSurfaceVariant = Color(0xFF44474F)
    val Outline = Color(0xFF75777F)
    val OutlineVariant = Color(0xFFC5C6D0)
    val SecondaryContainer = Color(0xFFDBE2F9)
    val OnSecondaryContainer = Color(0xFF3F4759)
    val Divider = Color(0xFFD6D6D6)
}

@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CreateReportColors.Surface)
            .padding(contentPadding),
        content = content
    )
}

@Composable
internal fun ReportTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    minLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(4.dp),
        colors = reportTextFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(4.dp),
            colors = reportTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun ReportPickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Box(modifier = modifier) {
        ReportTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            readOnly = true,
            leadingIcon = leadingIcon,
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    showArrow: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onArrowClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null && onArrowClick == null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else Modifier
            ),
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
                style = MaterialTheme.typography.titleMedium,
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
        if (badge != null) {
            Text(
                text = badge,
                modifier = Modifier
                    .clip(RoundedCornerShape(21.dp))
                    .background(CreateReportColors.SecondaryContainer)
                    .padding(horizontal = 15.dp, vertical = 7.dp),
                color = CreateReportColors.OnSecondaryContainer,
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (showArrow) {
            if (onArrowClick != null) {
                IconButton(onClick = onArrowClick, enabled = enabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (enabled) CreateReportColors.OnSurface else CreateReportColors.Outline
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) CreateReportColors.OnSurface else CreateReportColors.Outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CreateReportColors.OnSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = CreateReportColors.OnSurface,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = CreateReportColors.OnSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    if (showDivider) {
        HorizontalDivider(color = CreateReportColors.Outline)
    }
}

@Composable
private fun reportTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CreateReportColors.Surface,
    unfocusedContainerColor = CreateReportColors.Surface,
    disabledContainerColor = CreateReportColors.Surface,
    focusedBorderColor = CreateReportColors.Outline,
    unfocusedBorderColor = CreateReportColors.Outline,
    focusedLabelColor = CreateReportColors.OnSurface,
    unfocusedLabelColor = CreateReportColors.OnSurfaceVariant,
    focusedTextColor = CreateReportColors.OnSurface,
    unfocusedTextColor = CreateReportColors.OnSurface,
    focusedPlaceholderColor = CreateReportColors.OnSurfaceVariant,
    unfocusedPlaceholderColor = CreateReportColors.OnSurfaceVariant,
    focusedLeadingIconColor = CreateReportColors.OnSurfaceVariant,
    unfocusedLeadingIconColor = CreateReportColors.OnSurfaceVariant,
    focusedTrailingIconColor = CreateReportColors.OnSurfaceVariant,
    unfocusedTrailingIconColor = CreateReportColors.OnSurfaceVariant
)

package com.example.fishing.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.ui.theme.FishingTheme

/**
 * Decrease / increase / remove controls for one catch row.
 *
 * At `count == 1` the decrease button turns into a delete button, and the
 * separate close button always removes the whole row. The increase button is
 * disabled when the row cannot grow (for example, the trophy report).
 */
@Composable
fun QuantityStepper(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    isIncrementEnabled: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val hapticFeedback = LocalHapticFeedback.current
        val isRemove = count <= 1

        FilledTonalIconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
                onDecrement()
            },
            modifier = Modifier
                .size(52.dp, 40.dp)
                .testTag("quantity_decrement"),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                bottomStart = 16.dp,
                topEnd = 4.dp,
                bottomEnd = 4.dp
            ),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isRemove) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isRemove) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        ) {
            Icon(
                imageVector = if (isRemove) Icons.Default.Delete else Icons.Default.Remove,
                contentDescription = if (isRemove) {
                    stringResource(R.string.delete)
                } else {
                    stringResource(R.string.decrease)
                }
            )
        }

        FilledTonalIconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onIncrement()
            },
            enabled = isIncrementEnabled,
            modifier = Modifier
                .size(52.dp, 40.dp)
                .testTag("quantity_increment"),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isIncrementEnabled) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color(0xFFE0E0E0)
                },
                contentColor = if (isIncrementEnabled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    Color(0xFF9E9E9E)
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.increase)
            )
        }

        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onRemove()
            },
            modifier = Modifier
                .size(48.dp)
                .testTag("quantity_remove")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuantityStepperPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuantityStepper(
                count = 3,
                onDecrement = {},
                onIncrement = {},
                onRemove = {}
            )
            QuantityStepper(
                count = 1,
                onDecrement = {},
                onIncrement = {},
                onRemove = {}
            )
        }
    }
}

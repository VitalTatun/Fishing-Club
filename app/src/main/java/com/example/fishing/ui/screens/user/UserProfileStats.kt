package com.example.fishing.ui.screens.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.R

/**
 * Second block of the user page: aggregate counters derived from the loaded
 * reports, so the numbers always agree with the list below.
 *
 * Achievements ("7 из 54" in the design) are intentionally absent: the backend
 * has no achievements yet. Add a third [UserStat] here once it exists.
 */
@Composable
fun UserProfileStats(
    caughtCount: Int,
    reportsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserStat(
            label = stringResource(R.string.user_caught),
            value = caughtCount.toString()
        )
        UserStat(
            label = stringResource(R.string.user_reports_count),
            value = reportsCount.toString()
        )
    }
}

@Composable
private fun UserStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

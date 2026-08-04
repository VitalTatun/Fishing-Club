package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.ui.screens.report.create.CreateReportColors

@Composable
fun ProfileScreen(
    userEmail: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userEmail != null) {
            Text(
                text = userEmail,
                style = MaterialTheme.typography.titleMedium,
                color = CreateReportColors.OnSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CreateReportColors.OnSurface
            )
        ) {
            Text(stringResource(R.string.logout))
        }
    }
}

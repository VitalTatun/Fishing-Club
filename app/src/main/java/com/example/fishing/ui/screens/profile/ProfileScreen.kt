package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.ui.components.ProfileListItem
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.utils.AppUtils

@Composable
fun ProfileScreen(
    userEmail: String?,
    modifier: Modifier = Modifier,
    userName: String? = null,
    avatarUrl: String? = null,
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onChangeHistoryClick: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход") },
            text = { Text("Вы уверены, что хотите выйти из профиля?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userEmail != null) {
            ProfileHeader(
                userName = userName ?: "Пользователь",
                userEmail = userEmail,
                avatarUrl = avatarUrl,
                onClick = onEditClick
            )
        }
        
        val context = LocalContext.current
        val versionName = remember { AppUtils.getVersionName(context) }
        
        ProfileListItem(
            label = "Версия приложения",
            value = versionName,
            onClick = onChangeHistoryClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            TextButton(
                onClick = {
                    showLogoutDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Выйти",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_launcher_foreground),
            placeholder = painterResource(R.drawable.ic_launcher_foreground)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    FishingTheme {
        ProfileScreen(
            userName = "Никита Белозерцев",
            userEmail = "nikita.bel@gmail.com",
            avatarUrl = null,
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenOnlyEmailPreview() {
    FishingTheme {
        ProfileScreen(
            userEmail = "only.email@example.com",
            onLogoutClick = {}
        )
    }
}



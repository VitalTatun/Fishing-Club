package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.ui.theme.FishingTheme

@Composable
fun ProfileScreen(
    userEmail: String?,
    modifier: Modifier = Modifier,
    userName: String? = null,
    userHandle: String? = null,
    avatarUrl: String? = null,
    onEditClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userEmail != null) {
            ProfileHeader(
                userName = userName ?: "Рыболов",
                userHandle = userHandle,
                userEmail = userEmail,
                avatarUrl = avatarUrl,
                onClick = onEditClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    userHandle: String?,
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
            .padding(vertical = 16.dp, horizontal = 8.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (userHandle != null) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = userHandle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    FishingTheme {
        ProfileScreen(
            userName = "Никита Белозерцев",
            userHandle = "@Пескарь",
            userEmail = "nikita.bel@gmail.com",
            avatarUrl = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenOnlyEmailPreview() {
    FishingTheme {
        ProfileScreen(
            userEmail = "only.email@example.com"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenDarkPreview() {
    FishingTheme(darkTheme = true) {
        Surface(color = Color(0xFF000000)) {
            ProfileScreen(
                userName = "Никита Белозерцев",
                userHandle = "@Пескарь",
                userEmail = "nikita.bel@gmail.com",
                avatarUrl = null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

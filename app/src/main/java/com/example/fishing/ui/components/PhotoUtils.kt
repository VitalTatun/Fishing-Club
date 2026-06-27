package com.example.fishing.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.fishing.data.FishingRepository

@Composable
fun rememberPhotoUrl(
    photoPath: String,
    repository: FishingRepository
): String? {
    var url by remember(photoPath) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(photoPath) {
        url = when {
            photoPath.startsWith("http") -> photoPath
            photoPath.startsWith("/") -> photoPath
            repository.isStoragePath(photoPath) -> {
                repository.getPhotoSignedUrl(photoPath) ?: photoPath
            }
            else -> photoPath
        }
    }
    
    return url
}

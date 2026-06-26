package com.example.fishing.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.fishing.data.FishingRepository

@Composable
fun PhotoImage(
    photoPath: String,
    repository: FishingRepository,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    var resolvedUrl by remember(photoPath) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(photoPath) {
        resolvedUrl = when {
            photoPath.startsWith("http") -> photoPath
            photoPath.startsWith("/") -> photoPath
            repository.isStoragePath(photoPath) -> {
                repository.getPhotoSignedUrl(photoPath) ?: photoPath
            }
            else -> photoPath
        }
    }
    
    AsyncImage(
        model = resolvedUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

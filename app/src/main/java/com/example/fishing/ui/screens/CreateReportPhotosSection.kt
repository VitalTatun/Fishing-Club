package com.example.fishing.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.fishing.ui.theme.FishingTheme

private const val MaxPhotos = 6

@Composable
internal fun PhotosSection(
    selectedPhotoUris: List<Uri> = emptyList(),
    onPhotosChange: (List<Uri>) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MaxPhotos)
    ) { uris ->
        val availableSlots = MaxPhotos - selectedPhotoUris.size
        val newUris = uris.take(availableSlots).filter { it !in selectedPhotoUris }
        onPhotosChange(selectedPhotoUris + newUris)
    }

    PhotosSectionContent(
        selectedPhotoUris = selectedPhotoUris,
        onAddClick = {
            if (selectedPhotoUris.size < MaxPhotos) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        },
        onRemoveClick = { uri ->
            onPhotosChange(selectedPhotoUris - uri)
        }
    )
}

@Composable
internal fun PhotosSectionContent(
    selectedPhotoUris: List<Uri>,
    onAddClick: () -> Unit,
    onRemoveClick: (Uri) -> Unit
) {
    SectionCard(contentPadding = PaddingValues(0.dp)) {
        SectionHeader(
            title = "Фотографии",
            badge = "${selectedPhotoUris.size}/$MaxPhotos",
            onArrowClick = onAddClick,
            enabled = selectedPhotoUris.size < MaxPhotos,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (selectedPhotoUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items = selectedPhotoUris, key = { index, _ -> index }) { _, photoUri ->
                    PhotoTile(
                        photoUri = photoUri,
                        onRemoveClick = { onRemoveClick(photoUri) },
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photoUri: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(28.dp),
            onClick = onRemoveClick,
            shape = CircleShape,
            color = CreateReportColors.SecondaryContainer,
            contentColor = CreateReportColors.OnSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить фото",
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotosSectionEmptyPreview() {
    FishingTheme {
        PhotosSectionContent(
            selectedPhotoUris = emptyList(),
            onAddClick = {},
            onRemoveClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotosSectionWithPhotosPreview() {
    FishingTheme {
        PhotosSectionContent(
            selectedPhotoUris = listOf(
                Uri.parse("1"),
                Uri.parse("2"),
                Uri.parse("3")
            ),
            onAddClick = {},
            onRemoveClick = {}
        )
    }
}

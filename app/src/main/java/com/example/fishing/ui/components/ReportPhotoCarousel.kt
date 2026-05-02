package com.example.fishing.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.fishing.ui.theme.FishingTheme

// 1. Фото Карусель
@Composable
fun ReportPhotoCarousel(photos: List<Int>, modifier: Modifier = Modifier) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState { photos.size }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Отступы по бокам, чтобы фото не касалось краев экрана
            contentPadding = PaddingValues(horizontal = 8.dp),
            // Расстояние между фотографиями
            pageSpacing = 8.dp
        ) { index ->
            Image(
                painter = painterResource(id = photos[index]),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)), // Рамка для каждого фото
                contentScale = ContentScale.Crop
            )
        }

        // Счетчик страниц
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 20.dp), // Смещение, чтобы быть внутри рамки фото
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "${pagerState.currentPage + 1} из ${photos.size}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportPhotoCarouselPreview() {
    FishingTheme {
        ReportPhotoCarousel(
            photos = listOf(
                android.R.drawable.ic_menu_gallery,
                android.R.drawable.ic_menu_camera,
                android.R.drawable.ic_menu_manage,
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

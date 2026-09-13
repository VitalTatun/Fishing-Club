package com.example.fishing.ui.screens.report.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.fishing.R
import kotlin.math.abs

@Composable
fun PhotoViewerOverlay(
    photos: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            ZoomableImage(model = photos[page])
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ZoomableImage(model: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                    val focalX = centroid.x
                    val focalY = centroid.y

                    val zoomAdjX = (focalX / oldScale - focalX / newScale) - pan.x
                    val zoomAdjY = (focalY / oldScale - focalY / newScale) - pan.y
                    val newOffset = offset + Offset(zoomAdjX, zoomAdjY)

                    val vpW = size.width.toFloat()
                    val vpH = size.height.toFloat()
                    val maxOffX = ((newScale * vpW - vpW) / 2f).coerceAtLeast(0f)
                    val maxOffY = ((newScale * vpH - vpH) / 2f).coerceAtLeast(0f)

                    offset = Offset(
                        newOffset.x.coerceIn(-maxOffX, maxOffX),
                        newOffset.y.coerceIn(-maxOffY, maxOffY)
                    )
                    scale = newScale

                    if (scale <= minScale) {
                        offset = Offset.Zero
                    }
                }
            }
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        )
    }
}

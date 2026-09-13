package com.example.fishing.ui.screens.report.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.example.fishing.R

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
                .align(Alignment.TopStart)
                .statusBarsPadding()
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
    // State is keyed on model so each photo starts with a clean zoom/pan state.
    var scale by remember(model) { mutableFloatStateOf(1f) }
    var offset by remember(model) { mutableStateOf(Offset.Zero) }
    var viewport by remember(model) { mutableStateOf(IntSize.Zero) }
    var imageIntrinsic by remember(model) { mutableStateOf<IntSize?>(null) }

    val minScale = 1f
    val maxScale = 5f

    // Displayed image size after ContentScale.Fit inside the viewport.
    // Falls back to viewport size while the image is loading.
    val vpW = viewport.width.toFloat()
    val vpH = viewport.height.toFloat()
    val intrinsic = imageIntrinsic
    val baseW: Float
    val baseH: Float
    if (intrinsic != null && vpW > 0 && vpH > 0 && intrinsic.width > 0 && intrinsic.height > 0) {
        val fit = minOf(vpW / intrinsic.width, vpH / intrinsic.height)
        baseW = intrinsic.width * fit
        baseH = intrinsic.height * fit
    } else {
        baseW = vpW
        baseH = vpH
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewport = it }
            .pointerInput(model, viewport, imageIntrinsic) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
                    // Effective factor after coercion, so hitting min/max doesn't jump.
                    val k = if (oldScale == 0f) 1f else newScale / oldScale

                    // graphicsLayer scales around the viewport center, so the
                    // focal correction must be relative to the center, and the
                    // previous offset scales with k (translation is in screen px).
                    val center = Offset(vpW / 2f, vpH / 2f)
                    val corrected = (centroid - center) * (1f - k) + offset * k + pan

                    val maxOffX = ((baseW * newScale - vpW) / 2f).coerceAtLeast(0f)
                    val maxOffY = ((baseH * newScale - vpH) / 2f).coerceAtLeast(0f)

                    offset = Offset(
                        corrected.x.coerceIn(-maxOffX, maxOffX),
                        corrected.y.coerceIn(-maxOffY, maxOffY)
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
            onSuccess = { state: AsyncImagePainter.State.Success ->
                val s = state.painter.intrinsicSize
                if (s != Size.Unspecified && s.width > 0 && s.height > 0) {
                    imageIntrinsic = IntSize(s.width.toInt(), s.height.toInt())
                }
            },
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

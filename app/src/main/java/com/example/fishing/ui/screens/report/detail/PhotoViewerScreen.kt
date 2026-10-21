package com.example.fishing.ui.screens.report.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.example.fishing.R
import kotlinx.coroutines.launch

@Composable
fun PhotoViewerOverlay(
    photos: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }

        val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
        val haptic = LocalHapticFeedback.current
        var previousPage by remember { mutableIntStateOf(initialIndex) }

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != previousPage) {
                previousPage = pagerState.currentPage
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        
        // Track scale for each page to enable/disable pager scrolling when zoomed in
        val scales = remember { mutableStateMapOf<Int, Float>() }
        val currentScale = scales[pagerState.currentPage] ?: 1f
        val isZoomed = currentScale > 1.01f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                userScrollEnabled = !isZoomed
            ) { page ->
                ZoomableImage(
                    model = photos[page],
                    onScaleChanged = { scale ->
                        scales[page] = scale
                    }
                )
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
}

@Composable
fun ZoomableImage(
    model: String,
    onScaleChanged: (Float) -> Unit = {}
) {
    // State is keyed on model so each photo starts with a clean zoom/pan state.
    var scale by remember(model) { mutableFloatStateOf(1f) }
    var offset by remember(model) { mutableStateOf(Offset.Zero) }
    var viewport by remember(model) { mutableStateOf(IntSize.Zero) }
    var imageIntrinsic by remember(model) { mutableStateOf<IntSize?>(null) }

    val minScale = 1f
    val maxScale = 5f

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }

    // Displayed image size after ContentScale.Fit inside the viewport.
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
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        val pressedCount = pressedChanges.size

                        // If at 100% scale and only 1 pointer is touching, do not consume
                        // so that HorizontalPager can handle horizontal swipes.
                        if (scale <= 1.01f && pressedCount < 2) {
                            continue
                        }

                        pressedChanges.forEach { it.consume() }

                        val currentCentroidSize = event.calculateCentroidSize(useCurrent = true)
                        val previousCentroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoom = if (currentCentroidSize > 0f && previousCentroidSize > 0f) {
                            currentCentroidSize / previousCentroidSize
                        } else {
                            1f
                        }

                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)

                        if (zoom.isNaN() || pan.x.isNaN() || pan.y.isNaN() || centroid.isUnspecified) {
                            continue
                        }

                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
                        val k = if (oldScale == 0f) 1f else newScale / oldScale

                        val center = Offset(vpW / 2f, vpH / 2f)
                        val corrected = (centroid - center) * (1f - k) + offset * k + pan

                        val maxOffX = ((baseW * newScale - vpW) / 2f).coerceAtLeast(0f)
                        val maxOffY = ((baseH * newScale - vpH) / 2f).coerceAtLeast(0f)

                        val newOffset = Offset(
                            corrected.x.coerceIn(-maxOffX, maxOffX),
                            corrected.y.coerceIn(-maxOffY, maxOffY)
                        )

                        scale = newScale
                        offset = if (newScale <= minScale) Offset.Zero else newOffset
                    }
                }
            }
            .pointerInput(model) {
                detectTapGestures(
                    onDoubleTap = { _ ->
                        coroutineScope.launch {
                            val targetScale = if (scale > 1.01f) 1f else 2.5f
                            val targetOffset = Offset.Zero

                            val animScale = Animatable(scale)
                            val animOffset = Animatable(offset, Offset.VectorConverter)

                            launch {
                                animScale.animateTo(
                                    targetValue = targetScale,
                                    animationSpec = tween(durationMillis = 300)
                                ) {
                                    scale = value
                                }
                            }
                            launch {
                                animOffset.animateTo(
                                    targetValue = targetOffset,
                                    animationSpec = tween(durationMillis = 300)
                                ) {
                                    offset = value
                                }
                            }
                        }
                    }
                )
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

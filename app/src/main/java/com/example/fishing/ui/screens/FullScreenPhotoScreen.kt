package com.example.fishing.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.fishing.data.FishingRepository
import com.example.fishing.ui.components.PhotoImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun FullScreenPhotoScreen(
    photos: List<String>,
    repository: FishingRepository,
    initialPage: Int = 0,
    onBackClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Synchronize LazyRow with Pager
    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        listState.animateScrollToItem(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            userScrollEnabled = !isCurrentPageZoomed,
            beyondViewportPageCount = 1
        ) { page ->
            // Resolve the photo URL
            var resolvedUrl by remember(photos[page]) { mutableStateOf<String?>(null) }
            LaunchedEffect(photos[page]) {
                resolvedUrl = when {
                    photos[page].startsWith("http") -> photos[page]
                    photos[page].startsWith("/") -> photos[page]
                    repository.isStoragePath(photos[page]) -> {
                        repository.getPhotoSignedUrl(photos[page]) ?: photos[page]
                    }
                    else -> photos[page]
                }
            }
            
            resolvedUrl?.let { url ->
                ZoomableImage(
                    model = url,
                    onZoomChange = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isCurrentPageZoomed = zoomed
                            if (zoomed) showControls = false
                        }
                    },
                    onTap = { showControls = !showControls }
                )
            }
        }

        // Overlay Controls (Top)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                FilledIconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }

                if (photos.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        modifier = Modifier.padding(16.dp).align(Alignment.TopCenter),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Thumbnails Strip (Bottom)
        if (photos.size > 1) {
            AnimatedVisibility(
                visible = showControls && !isCurrentPageZoomed,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp)
                ) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(photos) { index, photoUrl ->
                            val isSelected = pagerState.currentPage == index
                            // Resolve the photo URL for thumbnail
                            var thumbnailUrl by remember(photoUrl) { mutableStateOf<String?>(null) }
                            LaunchedEffect(photoUrl) {
                                thumbnailUrl = when {
                                    photoUrl.startsWith("http") -> photoUrl
                                    photoUrl.startsWith("/") -> photoUrl
                                    repository.isStoragePath(photoUrl) -> {
                                        repository.getPhotoSignedUrl(photoUrl) ?: photoUrl
                                    }
                                    else -> photoUrl
                                }
                            }
                            
                            thumbnailUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    model: String,
    modifier: Modifier = Modifier,
    onZoomChange: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    var targetScale by remember { mutableFloatStateOf(1f) }
    var targetOffset by remember { mutableStateOf(Offset.Zero) }
    val painter = rememberAsyncImagePainter(model = model)
    val imageSize = painter.intrinsicSize

    val scale by animateFloatAsState(targetValue = targetScale, label = "ScaleAnimation")
    val offset by animateOffsetAsState(targetValue = targetOffset, label = "OffsetAnimation")

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val areaWidth = constraints.maxWidth.toFloat()
        val areaHeight = constraints.maxHeight.toFloat()

        val scaleFit = minOf(areaWidth / imageSize.width, areaHeight / imageSize.height)
        val actualWidth = imageSize.width * scaleFit
        val actualHeight = imageSize.height * scaleFit

        val maxScale = (1f / scaleFit).coerceAtLeast(3f)

        LaunchedEffect(targetScale, targetOffset) {
            val extraWidth = (actualWidth * targetScale - areaWidth).coerceAtLeast(0f)
            val extraHeight = (actualHeight * targetScale - areaHeight).coerceAtLeast(0f)
            val maxX = extraWidth / 2
            val maxY = extraHeight / 2

            targetOffset = Offset(
                x = targetOffset.x.coerceIn(-maxX, maxX),
                y = targetOffset.y.coerceIn(-maxY, maxY)
            )
        }

        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                // Сначала применяем графические трансформации
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                // Обработка жестов: когда масштаб 1x, горизонтальный свайп отдаём Pager'у
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var accumulatedZoom = 1f
                        var accumulatedPan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val zoomChange = event.zoomChange()
                            val panChange = event.panChange()

                            if (!pastTouchSlop) {
                                accumulatedZoom *= zoomChange
                                accumulatedPan += panChange

                                val centroidSize = event.centroidSize(useCurrent = true)
                                val zoomMotion = abs(1f - accumulatedZoom) * centroidSize
                                val panMotion = accumulatedPan.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val wasZoomed = targetScale > 1f

                                if (zoomChange != 1f) {
                                    val newScale = (targetScale * zoomChange).coerceIn(1f, 10f)
                                    if (newScale != targetScale) {
                                        targetScale = newScale
                                        if (targetScale == 1f) {
                                            targetOffset = Offset.Zero
                                        }
                                        onZoomChange(targetScale > 1f)
                                    }
                                }

                                if (targetScale > 1f) {
                                    targetOffset += panChange
                                }

                                val shouldConsume = zoomChange != 1f || wasZoomed || targetScale > 1f
                                if (shouldConsume) {
                                    event.changes.forEach { it.consume() }
                                }
                            }

                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
                // И в конце - тапы, чтобы они не мешали скроллу родителя
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tapOffset ->
                            if (targetScale > 1f) {
                                targetScale = 1f
                                targetOffset = Offset.Zero
                                onZoomChange(false)
                            } else {
                                targetScale = maxScale
                                onZoomChange(true)
                                val centerX = areaWidth / 2
                                val centerY = areaHeight / 2
                                val diffX = (tapOffset.x - centerX) * maxScale
                                val diffY = (tapOffset.y - centerY) * maxScale
                                targetOffset = Offset(x = -diffX, y = -diffY)
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )
    }
}

private fun PointerEvent.centroid(useCurrent: Boolean): Offset {
    val relevantChanges = changes.filter { it.pressed }
    if (relevantChanges.isEmpty()) return Offset.Zero

    var sumX = 0f
    var sumY = 0f
    relevantChanges.forEach { change ->
        val position = if (useCurrent) change.position else change.previousPosition
        sumX += position.x
        sumY += position.y
    }
    return Offset(sumX / relevantChanges.size, sumY / relevantChanges.size)
}

private fun PointerEvent.centroidSize(useCurrent: Boolean): Float {
    val relevantChanges = changes.filter { it.pressed }
    if (relevantChanges.size < 2) return 0f

    val centroid = centroid(useCurrent = useCurrent)
    var sum = 0f
    relevantChanges.forEach { change ->
        val position = if (useCurrent) change.position else change.previousPosition
        sum += hypot(position.x - centroid.x, position.y - centroid.y)
    }
    return sum / relevantChanges.size
}

private fun PointerEvent.panChange(): Offset {
    if (changes.none { it.pressed }) return Offset.Zero
    return centroid(useCurrent = true) - centroid(useCurrent = false)
}

private fun PointerEvent.zoomChange(): Float {
    val currentSize = centroidSize(useCurrent = true)
    val previousSize = centroidSize(useCurrent = false)
    if (previousSize == 0f || currentSize == 0f) return 1f
    return currentSize / previousSize
}

package com.example.fishing.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.ui.components.MapReportSheetContent
import com.example.fishing.ui.components.MarkerDrawableUtils
import com.example.fishing.ui.components.MarkerShape
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    reports: List<FishingReport>,
    onReportClick: (FishingReport) -> Unit,
    viewModel: MainViewModel? = null,
    onBackClick: (() -> Unit)? = null,
    isLocationEnabled: Boolean = true,
    markersInteractive: Boolean = true,
    initialReportId: UUID? = null,
    repository: FishingRepository
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    var lastCenterLat by remember { mutableStateOf(viewModel?.mapLastCenterLat) }
    var lastCenterLon by remember { mutableStateOf(viewModel?.mapLastCenterLon) }
    var lastZoom by remember { mutableDoubleStateOf(viewModel?.mapLastZoom ?: 6.0) }
    var hasInitialLocationBeenSet by remember { mutableStateOf(false) }

    val requestedLocation by viewModel?.mapRequestedLocation?.collectAsState() ?: remember { mutableStateOf(null) }
    val fallbackCenter = remember(reports) {
        val validReports = reports.filter { report ->
            report.water.latitude != 0.0 || report.water.longitude != 0.0
        }
        if (validReports.isNotEmpty()) {
            GeoPoint(
                validReports.map { it.water.latitude }.average(),
                validReports.map { it.water.longitude }.average()
            )
        } else {
            GeoPoint(53.9, 27.5667)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(requestedLocation) {
        requestedLocation?.let { location ->
            mapView.controller.animateTo(location, 13.0, 500L)
            viewModel?.requestMapLocation(null)
            lastCenterLat = location.latitude
            lastCenterLon = location.longitude
            lastZoom = 13.0
        }
    }

    val trophyColor = FishingTheme.colors.trophyYellow.toArgb()
    val regularColor = MaterialTheme.colorScheme.primary.toArgb()
    val trophyIconColor = android.graphics.Color.parseColor("#50250A")

    var selectedReportId by remember { mutableStateOf(initialReportId) }

    // ===== Custom Sheet State =====
    var selectedReport by remember { mutableStateOf<FishingReport?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }
    val sheetHeightFraction = 0.5f
    val sheetAnimY = remember { Animatable(1f) } // 1 = скрыт, 0 = half, -1 = full
    var isSheetExpanded by remember { mutableStateOf(false) }

    fun dismissSheet() {
        coroutineScope.launch {
            sheetAnimY.animateTo(1f, tween(300))
            sheetVisible = false
            selectedReport = null
            isSheetExpanded = false
        }
    }

    fun showSheetFor(report: FishingReport) {
        selectedReport = report
        sheetVisible = true
        isSheetExpanded = false
        coroutineScope.launch {
            sheetAnimY.snapTo(1f)
            sheetAnimY.animateTo(0f, tween(300))
        }
    }

    fun expandSheet() {
        isSheetExpanded = true
        coroutineScope.launch {
            sheetAnimY.animateTo(-1f, tween(300))
        }
    }

    fun collapseSheet() {
        isSheetExpanded = false
        coroutineScope.launch {
            sheetAnimY.animateTo(0f, tween(300))
        }
    }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    }

    DisposableEffect(myLocationOverlay, isLocationEnabled) {
        if (isLocationEnabled) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.runOnFirstFix {
                val location = myLocationOverlay.myLocation
                if (location != null) {
                    mapView.post {
                        if (!hasInitialLocationBeenSet && lastCenterLat == null) {
                            mapView.controller.animateTo(location)
                            mapView.controller.setZoom(15.0)
                            myLocationOverlay.disableFollowLocation()
                            hasInitialLocationBeenSet = true
                        }
                    }
                }
            }
        } else {
            myLocationOverlay.disableMyLocation()
            myLocationOverlay.disableFollowLocation()
        }

        onDispose {
            myLocationOverlay.disableMyLocation()
            myLocationOverlay.disableFollowLocation()
        }
    }

    LaunchedEffect(mapView) {
        if (lastCenterLat != null && lastCenterLon != null) {
            mapView.controller.setCenter(GeoPoint(lastCenterLat!!, lastCenterLon!!))
            mapView.controller.setZoom(lastZoom)
            myLocationOverlay.disableFollowLocation()
        }
    }

    LaunchedEffect(mapView, fallbackCenter) {
        if (lastCenterLat == null && requestedLocation == null) {
            mapView.controller.setCenter(fallbackCenter)
            mapView.controller.setZoom(11.0)
            if (!isLocationEnabled) {
                lastCenterLat = fallbackCenter.latitude
                lastCenterLon = fallbackCenter.longitude
                lastZoom = 11.0
            }
        }
    }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapView.mapCenter
                lastCenterLat = center.latitude
                lastCenterLon = center.longitude
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                lastZoom = mapView.zoomLevelDouble
                return true
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    SideEffect {
        viewModel?.let {
            it.mapLastCenterLat = lastCenterLat
            it.mapLastCenterLon = lastCenterLon
            it.mapLastZoom = lastZoom
        }
    }

    // ===== Layout =====
    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            mapView = mapView,
            myLocationOverlay = myLocationOverlay,
            modifier = Modifier.fillMaxSize(),
            reports = reports,
            onMarkerClick = { report ->
                showSheetFor(report)
                coroutineScope.launch {
                    delay(500L)
                    mapView.controller.animateTo(
                        GeoPoint(report.water.latitude, report.water.longitude),
                        mapView.zoomLevelDouble,
                        300L
                    )
                }
            },
            onReportSelected = { selectedReportId = it },
            selectedReportId = selectedReportId,
            markersInteractive = markersInteractive,
            trophyColor = trophyColor,
            regularColor = regularColor,
            trophyIconColor = trophyIconColor,
            initialZoom = lastZoom
        )

        if (onBackClick != null) {
            FilledIconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        }

        FloatingActionButton(
            onClick = {
                val location = myLocationOverlay.myLocation
                if (location != null) {
                    mapView.controller.animateTo(location, 15.0, 500L)
                } else {
                    myLocationOverlay.enableMyLocation()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location"
            )
        }
    }

    // ===== Custom Bottom Sheet =====
    if (sheetVisible && selectedReport != null) {
        val screenHeightPx = with(density) { LocalContext.current.resources.displayMetrics.heightPixels.toFloat() }
        val halfHeightPx = screenHeightPx * sheetHeightFraction
        val fullHeightPx = screenHeightPx * 0.92f

        // Вычисляем текущий offset Y (0 = верх экрана)
        val currentOffsetY = when {
            sheetAnimY.value >= 0f -> {
                // От скрытого (screenHeight) до halfHeight
                halfHeightPx + (screenHeightPx - halfHeightPx) * sheetAnimY.value
            }
            else -> {
                // От halfHeight до fullHeight
                halfHeightPx - (halfHeightPx - fullHeightPx) * (-sheetAnimY.value)
            }
        }
        val currentHeightPx = screenHeightPx - currentOffsetY
        val currentHeightDp = with(density) { currentHeightPx.toDp() }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Карта ниже — полностью интерактивна, тапы проходят

            // Sheet поверх
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeightDp)
                    .align(Alignment.BottomCenter)
                    .pointerInput(sheetVisible) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val y = sheetAnimY.value
                                when {
                                    y > 0.5f -> dismissSheet()
                                    y > -0.3f -> collapseSheet()
                                    else -> expandSheet()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val range = fullHeightPx - halfHeightPx
                                    val normalizedDelta = dragAmount / range
                                    val newValue = (sheetAnimY.value + normalizedDelta).coerceIn(-1f, 1f)
                                    sheetAnimY.snapTo(newValue)
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                MapReportSheetContent(
                    report = selectedReport!!,
                    repository = repository,
                    onPhotoClick = { },
                    showMapPreview = false
                )
            }
        }
    }
}

@Composable
fun OsmMapView(
    mapView: MapView,
    myLocationOverlay: MyLocationNewOverlay,
    modifier: Modifier = Modifier,
    reports: List<FishingReport>,
    onMarkerClick: (FishingReport) -> Unit,
    onReportSelected: (UUID?) -> Unit = {},
    selectedReportId: UUID? = null,
    markersInteractive: Boolean = true,
    trophyColor: Int,
    regularColor: Int,
    trophyIconColor: Int = android.graphics.Color.WHITE,
    initialZoom: Double = 6.0
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            (mapView.parent as? ViewGroup)?.removeView(mapView)

            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)

                if (zoomLevelDouble <= 1.0) {
                    controller.setZoom(initialZoom)
                }

                if (!overlays.contains(myLocationOverlay)) {
                    overlays.add(myLocationOverlay)
                }
            }
        },
        modifier = modifier,
        update = { mv ->
            val currentOverlays = mv.overlays

            val markersToRemove = currentOverlays.filterIsInstance<Marker>()
            currentOverlays.removeAll(markersToRemove)

            reports.forEach { report ->
                try {
                    val shape = if (report.id == selectedReportId) MarkerShape.DROP else MarkerShape.CIRCLE
                    val marker = Marker(mv).apply {
                        position = GeoPoint(report.water.latitude, report.water.longitude)
                        val anchorY = if (shape == MarkerShape.DROP) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER
                        setAnchor(Marker.ANCHOR_CENTER, anchorY)

                        if (markersInteractive) {
                            title = report.name
                            subDescription = report.water.waterName
                            setOnMarkerClickListener { _, _ ->
                                onReportSelected(
                                    if (selectedReportId == report.id) null else report.id
                                )
                                onMarkerClick(report)
                                true
                            }
                        } else {
                            setInfoWindow(null)
                        }

                        val color = if (report.type == FishingType.HAUL) trophyColor else regularColor
                        val iconColor = if (report.type == FishingType.HAUL) trophyIconColor else android.graphics.Color.WHITE
                        icon = MarkerDrawableUtils.getMarkerDrawable(context, shape, color, report.fishingMethod, iconColor)
                    }
                    currentOverlays.add(marker)
                } catch (e: Exception) {
                }
            }
            mv.invalidate()
        }
    )
}

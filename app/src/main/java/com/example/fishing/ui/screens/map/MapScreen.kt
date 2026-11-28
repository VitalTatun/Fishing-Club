package com.example.fishing.ui.screens.map

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fishing.R
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingType
import com.example.fishing.model.MAP_ZOOM_THRESHOLD
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MarkerDomain
import com.example.fishing.ui.components.MarkerDrawableUtils
import com.example.fishing.ui.components.MarkerShape
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.filter.ReportFilters
import java.util.UUID
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    markers: List<MarkerDomain>,
    onMarkerClick: (MarkerDomain) -> Unit,
    viewModel: MainViewModel? = null,
    onBackClick: (() -> Unit)? = null,
    isLocationEnabled: Boolean = true,
    markersInteractive: Boolean = true,
    initialReportId: UUID? = null,
    selectedMarkerDotStyle: Boolean = false,
    repository: FishingRepository? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    /**
     * True on the main map tab: the camera drives viewport loading and the zoom
     * threshold applies. Full-map from a report disables both.
     */
    viewportMode: Boolean = true
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var currentViewport by remember(mapView) { mutableStateOf<MapBounds?>(null) }

    // Reports the real viewport to the coordinator. Below the zoom threshold
    // the coordinator ignores it, and full-map mode does not load viewports.
    val notifyViewportChange = remember(mapView, viewModel, viewportMode) {
        {
            if (viewportMode && mapView.width > 0 && mapView.height > 0) {
                val box = mapView.boundingBox
                val viewport = MapBounds(
                    south = box.latSouth,
                    north = box.latNorth,
                    west = box.lonWest,
                    east = box.lonEast
                )
                currentViewport = viewport
                viewModel?.onMapViewportChanged(viewport, mapView.zoomLevelDouble)
            }
        }
    }

    val highlightedPolygon by viewModel?.highlightedPolygon?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    var lastCenterLat by remember { mutableStateOf(viewModel?.mapLastCenterLat) }
    var lastCenterLon by remember { mutableStateOf(viewModel?.mapLastCenterLon) }
    var lastZoom by remember { mutableDoubleStateOf(viewModel?.mapLastZoom ?: 6.0) }
    var hasInitialLocationBeenSet by remember { mutableStateOf(false) }

    val requestedLocation by viewModel?.mapRequestedLocation?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    val mapFilters by viewModel?.mapFilters?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(ReportFilters()) }
    val isFilterActive = mapFilters.isActive

    val coordinatorZoomedOut by viewModel?.mapIsZoomedOut?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val truncated by viewModel?.mapTruncated?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val hasMarkersInViewport by viewModel?.mapHasMarkersInViewport
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(markers.isNotEmpty()) }
    val showingOwnOnly by viewModel?.mapShowingOwnOnly?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }

    val isZoomedOut = viewportMode &&
        if (viewModel != null) coordinatorZoomedOut else lastZoom < MAP_ZOOM_THRESHOLD
    val displayedMarkers = if (isZoomedOut) emptyList() else markers
    val effectiveHasMarkersInViewport =
        if (viewportMode) hasMarkersInViewport else displayedMarkers.isNotEmpty()
    // The session cache keeps markers from previously visited areas; only the
    // current viewport decides between "nothing here" and "markers exist".
    val visibleMarkers = remember(displayedMarkers, currentViewport) {
        currentViewport?.let { viewport ->
            displayedMarkers.filter { marker ->
                viewport.contains(marker.waterLat, marker.waterLng)
            }
        } ?: displayedMarkers
    }

    val fallbackCenter = remember(displayedMarkers) {
        val validMarkers = displayedMarkers.filter { it.waterLat != 0.0 || it.waterLng != 0.0 }
        if (validMarkers.isNotEmpty()) {
            GeoPoint(validMarkers.map { it.waterLat }.average(), validMarkers.map { it.waterLng }.average())
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
            mapView.post { notifyViewportChange() }
        }
    }

    val trophyColor = FishingTheme.colors.trophyYellow.toArgb()
    val regularColor = MaterialTheme.colorScheme.primary.toArgb()
    val trophyIconColor = android.graphics.Color.parseColor("#50250A")

    var selectedMarkerId by remember { mutableStateOf(initialReportId) }

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
            mapView.post { notifyViewportChange() }
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
            mapView.post { notifyViewportChange() }
        }
    }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapView.mapCenter
                lastCenterLat = center.latitude
                lastCenterLon = center.longitude
                notifyViewportChange()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                lastZoom = mapView.zoomLevelDouble
                notifyViewportChange()
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

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            mapView = mapView,
            myLocationOverlay = myLocationOverlay,
            modifier = Modifier.fillMaxSize(),
            markers = displayedMarkers,
            highlightedPolygon = highlightedPolygon,
            onMarkerClick = { marker ->
                viewModel?.setHighlightedPolygon(null)
                onMarkerClick(marker)
            },
            onMarkerSelected = { selectedMarkerId = it },
            selectedMarkerId = selectedMarkerId,
            markersInteractive = markersInteractive,
            selectedMarkerDotStyle = selectedMarkerDotStyle,
            trophyColor = trophyColor,
            regularColor = regularColor,
            trophyIconColor = trophyIconColor,
            initialZoom = lastZoom
        )

        when (
            mapOverlayState(
                rawMarkers = displayedMarkers,
                filteredMarkers = visibleMarkers,
                isLoading = isLoading,
                errorMessage = errorMessage,
                isFilterActive = isFilterActive,
                isZoomedOut = isZoomedOut,
                hasMarkersInViewport = effectiveHasMarkersInViewport
            )
        ) {
            MapOverlayState.Loading -> Unit
            MapOverlayState.Error -> {
                MapStateOverlayCard(modifier = Modifier.align(Alignment.Center)) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.map_loading_failed),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            // Zoomed-out and viewport-empty are intentionally silent: the map
            // stays clean, markers are simply hidden until the camera changes.
            MapOverlayState.ZoomedOut,
            MapOverlayState.ViewportEmpty -> Unit
            MapOverlayState.Empty -> Unit
            MapOverlayState.FilteredEmpty -> Unit
            MapOverlayState.None -> Unit
        }

        if (onBackClick != null) {
            FilledIconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }

        if (viewportMode && truncated && displayedMarkers.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = stringResource(R.string.map_truncated_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        if (errorMessage != null && displayedMarkers.isNotEmpty()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        top = if (viewportMode && truncated) 80.dp else 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                action = {
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            ) {
                Text(
                    stringResource(
                        if (viewportMode && showingOwnOnly) {
                            R.string.map_offline_own_only
                        } else {
                            R.string.map_stale_data
                        }
                    )
                )
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
                .then(
                    if (onBackClick != null) Modifier.navigationBarsPadding()
                    else Modifier
                )
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.my_location)
            )
        }
    }
}

@Composable
fun OsmMapView(
    mapView: MapView,
    myLocationOverlay: MyLocationNewOverlay,
    modifier: Modifier = Modifier,
    markers: List<MarkerDomain>,
    highlightedPolygon: List<GeoPoint>? = null,
    onMarkerClick: (MarkerDomain) -> Unit,
    onMarkerSelected: (UUID?) -> Unit = {},
    selectedMarkerId: UUID? = null,
    markersInteractive: Boolean = true,
    selectedMarkerDotStyle: Boolean = false,
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

            // --- Polygon: remove old, add new if needed ---
            val polygonsToRemove = currentOverlays.filterIsInstance<Polygon>()
            currentOverlays.removeAll(polygonsToRemove.toSet())

            highlightedPolygon?.let { points ->
                val polygon = Polygon(mv).apply {
                    points.forEach { addPoint(it) }
                    fillPaint.color = android.graphics.Color.argb(75, 0, 0, 255)
                    outlinePaint.color = android.graphics.Color.BLUE
                    outlinePaint.strokeWidth = 2f
                }
                currentOverlays.add(polygon)
            }

            // --- Markers: diff-based update ---
            val existingMarkers = currentOverlays.filterIsInstance<Marker>()
                .associateBy { it.title }

            val newIds = markers.map { it.id.toString() }.toSet()
            val existingIds = existingMarkers.keys

            // Remove stale markers
            val toRemove = existingIds - newIds
            toRemove.forEach { id ->
                existingMarkers[id]?.let { currentOverlays.remove(it) }
            }

            // Add or update markers
            markers.forEach { marker ->
                val id = marker.id.toString()
                val existing = existingMarkers[id]

                if (existing != null) {
                    // Update in place — only touch what changed
                    existing.position = GeoPoint(marker.waterLat, marker.waterLng)

                    val isSelected = marker.id == selectedMarkerId
                    val shape = when {
                        !isSelected -> MarkerShape.CIRCLE
                        selectedMarkerDotStyle -> MarkerShape.DOT
                        else -> MarkerShape.DROP
                    }
                    val anchorY = if (shape == MarkerShape.DROP) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER
                    existing.setAnchor(Marker.ANCHOR_CENTER, anchorY)

                    val color = if (shape != MarkerShape.DOT && marker.type == FishingType.HAUL) {
                        trophyColor
                    } else {
                        regularColor
                    }
                    val iconColor = if (marker.type == FishingType.HAUL) trophyIconColor else android.graphics.Color.WHITE
                    existing.icon = MarkerDrawableUtils.getMarkerDrawable(context, shape, color, marker.fishingMethod, iconColor)
                } else {
                    // Create new marker
                    try {
                        val shape = when {
                            marker.id != selectedMarkerId -> MarkerShape.CIRCLE
                            selectedMarkerDotStyle -> MarkerShape.DOT
                            else -> MarkerShape.DROP
                        }
                        val markerOverlay = Marker(mv).apply {
                            title = marker.id.toString()
                            position = GeoPoint(marker.waterLat, marker.waterLng)
                            val anchorY = if (shape == MarkerShape.DROP) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER
                            setAnchor(Marker.ANCHOR_CENTER, anchorY)

                            if (markersInteractive) {
                                subDescription = marker.waterName
                                setOnMarkerClickListener { _, _ ->
                                    val currentSelected = selectedMarkerId
                                    onMarkerSelected(
                                        if (currentSelected == marker.id) null else marker.id
                                    )
                                    onMarkerClick(marker)
                                    true
                                }
                            } else {
                                setInfoWindow(null)
                            }

                            val color = if (shape != MarkerShape.DOT && marker.type == FishingType.HAUL) {
                                trophyColor
                            } else {
                                regularColor
                            }
                            val iconColor = if (marker.type == FishingType.HAUL) trophyIconColor else android.graphics.Color.WHITE
                            icon = MarkerDrawableUtils.getMarkerDrawable(context, shape, color, marker.fishingMethod, iconColor)
                        }
                        currentOverlays.add(markerOverlay)
                    } catch (_: Exception) {
                    }
                }
            }

            mv.invalidate()
        }
    )
}

internal enum class MapOverlayState {
    Loading,
    Error,
    ZoomedOut,
    Empty,
    ViewportEmpty,
    FilteredEmpty,
    None
}

internal fun mapOverlayState(
    rawMarkers: List<MarkerDomain>,
    filteredMarkers: List<MarkerDomain>,
    isLoading: Boolean,
    errorMessage: String?,
    isFilterActive: Boolean,
    isZoomedOut: Boolean = false,
    /** Unfiltered cache had at least one marker inside the current viewport. */
    hasMarkersInViewport: Boolean = rawMarkers.isNotEmpty()
): MapOverlayState = when {
    isZoomedOut -> MapOverlayState.ZoomedOut
    rawMarkers.isEmpty() && isLoading -> MapOverlayState.Loading
    rawMarkers.isEmpty() && errorMessage != null -> MapOverlayState.Error
    rawMarkers.isEmpty() && hasMarkersInViewport && isFilterActive ->
        MapOverlayState.FilteredEmpty
    rawMarkers.isEmpty() && hasMarkersInViewport -> MapOverlayState.ViewportEmpty
    rawMarkers.isEmpty() -> MapOverlayState.Empty
    filteredMarkers.isEmpty() && hasMarkersInViewport && isFilterActive ->
        MapOverlayState.FilteredEmpty
    filteredMarkers.isEmpty() -> MapOverlayState.ViewportEmpty
    else -> MapOverlayState.None
}

@Composable
private fun MapStateOverlayCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

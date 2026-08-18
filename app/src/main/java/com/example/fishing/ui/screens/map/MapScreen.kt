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
import com.example.fishing.R
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.ui.components.FishingFilterChips
import com.example.fishing.ui.components.MarkerDrawableUtils
import com.example.fishing.ui.components.MarkerShape
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import java.util.UUID
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
    markers: List<MarkerDomain>,
    onMarkerClick: (MarkerDomain) -> Unit,
    viewModel: MainViewModel? = null,
    onBackClick: (() -> Unit)? = null,
    isLocationEnabled: Boolean = true,
    markersInteractive: Boolean = true,
    initialReportId: UUID? = null,
    repository: FishingRepository,
    favoriteReports: List<FishingReport> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    var lastCenterLat by remember { mutableStateOf(viewModel?.mapLastCenterLat) }
    var lastCenterLon by remember { mutableStateOf(viewModel?.mapLastCenterLon) }
    var lastZoom by remember { mutableDoubleStateOf(viewModel?.mapLastZoom ?: 6.0) }
    var hasInitialLocationBeenSet by remember { mutableStateOf(false) }

    val requestedLocation by viewModel?.mapRequestedLocation?.collectAsState() ?: remember { mutableStateOf(null) }
    val fallbackCenter = remember(markers) {
        val validMarkers = markers.filter { it.waterLat != 0.0 || it.waterLng != 0.0 }
        if (validMarkers.isNotEmpty()) {
            GeoPoint(validMarkers.map { it.waterLat }.average(), validMarkers.map { it.waterLng }.average())
        } else {
            GeoPoint(53.9, 27.5667)
        }
    }

    var localIsFavoritesSelected by remember { mutableStateOf(false) }
    var localIsTrophySelected by remember { mutableStateOf(false) }
    var localIsPaidSelected by remember { mutableStateOf(false) }
    var localSelectedCatch by remember { mutableStateOf<String?>(null) }
    var localSelectedMethod by remember { mutableStateOf<FishingMethod?>(null) }

    val isFavoritesSelected = viewModel?.mapIsFavoritesSelected ?: localIsFavoritesSelected
    val isTrophySelected = viewModel?.mapIsTrophySelected ?: localIsTrophySelected
    val isPaidSelected = viewModel?.mapIsPaidSelected ?: localIsPaidSelected
    val selectedCatch = viewModel?.mapSelectedCatch ?: localSelectedCatch
    val selectedMethod = viewModel?.mapSelectedMethod ?: localSelectedMethod

    val uniqueFish = remember(markers) {
        markers.flatMap { it.fishNames }.distinct().sorted()
    }

    val filteredMarkers = remember(
        markers,
        isFavoritesSelected,
        isTrophySelected,
        isPaidSelected,
        selectedCatch,
        selectedMethod,
        favoriteReports
    ) {
        markers.filter { marker ->
            val matchesFavorites = if (!isFavoritesSelected) true else {
                favoriteReports.any { it.id == marker.id }
            }
            
            val matchesTrophy = if (!isTrophySelected) true else marker.type == FishingType.HAUL
            val matchesPaid = if (!isPaidSelected) true else marker.isPaidWater
            val matchesCatch = if (selectedCatch == null) true else marker.fishNames.contains(selectedCatch)
            val matchesMethod = if (selectedMethod == null) true else marker.fishingMethod == selectedMethod

            matchesFavorites && matchesTrophy && matchesPaid && matchesCatch && matchesMethod
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

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            mapView = mapView,
            myLocationOverlay = myLocationOverlay,
            modifier = Modifier.fillMaxSize(),
            markers = filteredMarkers,
            onMarkerClick = { marker ->
                onMarkerClick(marker)
            },
            onMarkerSelected = { selectedMarkerId = it },
            selectedMarkerId = selectedMarkerId,
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
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                FishingFilterChips(
                    isFavoritesSelected = isFavoritesSelected,
                    onFavoritesClick = {
                        if (viewModel != null) viewModel.mapIsFavoritesSelected = !viewModel.mapIsFavoritesSelected
                        else localIsFavoritesSelected = !localIsFavoritesSelected
                    },
                    isTrophySelected = isTrophySelected,
                    onTrophyClick = {
                        if (viewModel != null) viewModel.mapIsTrophySelected = !viewModel.mapIsTrophySelected
                        else localIsTrophySelected = !localIsTrophySelected
                    },
                    isPaidSelected = isPaidSelected,
                    onPaidClick = {
                        if (viewModel != null) viewModel.mapIsPaidSelected = !viewModel.mapIsPaidSelected
                        else localIsPaidSelected = !localIsPaidSelected
                    },
                    selectedCatch = selectedCatch,
                    onCatchSelected = {
                        if (viewModel != null) viewModel.mapSelectedCatch = it
                        else localSelectedCatch = it
                    },
                    onClearCatch = {
                        if (viewModel != null) viewModel.mapSelectedCatch = null
                        else localSelectedCatch = null
                    },
                    selectedMethod = selectedMethod,
                    onMethodSelected = {
                        if (viewModel != null) viewModel.mapSelectedMethod = it
                        else localSelectedMethod = it
                    },
                    onClearMethod = {
                        if (viewModel != null) viewModel.mapSelectedMethod = null
                        else localSelectedMethod = null
                    },
                    uniqueFish = uniqueFish
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
    onMarkerClick: (MarkerDomain) -> Unit,
    onMarkerSelected: (UUID?) -> Unit = {},
    selectedMarkerId: UUID? = null,
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

            markers.forEach { marker ->
                try {
                    val shape = if (marker.id == selectedMarkerId) MarkerShape.DROP else MarkerShape.CIRCLE
                    val markerOverlay = Marker(mv).apply {
                        position = GeoPoint(marker.waterLat, marker.waterLng)
                        val anchorY = if (shape == MarkerShape.DROP) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER
                        setAnchor(Marker.ANCHOR_CENTER, anchorY)

                        if (markersInteractive) {
                            title = marker.name
                            subDescription = marker.waterName
                            setOnMarkerClickListener { _, _ ->
                                onMarkerSelected(
                                    if (selectedMarkerId == marker.id) null else marker.id
                                )
                                onMarkerClick(marker)
                                true
                            }
                        } else {
                            setInfoWindow(null)
                        }

                        val color = if (marker.type == FishingType.HAUL) trophyColor else regularColor
                        val iconColor = if (marker.type == FishingType.HAUL) trophyIconColor else android.graphics.Color.WHITE
                        icon = MarkerDrawableUtils.getMarkerDrawable(context, shape, color, marker.fishingMethod, iconColor)
                    }
                    currentOverlays.add(markerOverlay)
                } catch (e: Exception) {
                    // Игнорируем ошибки инициализации маркеров в переходных состояниях
                }
            }
            mv.invalidate()
        }
    )
}

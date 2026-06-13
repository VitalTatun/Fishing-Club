package com.example.fishing.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingLocationScreen(
    onBackClick: () -> Unit,
    onSaveClick: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    initialLocation: GeoPoint? = null
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    var selectedLocation by rememberSaveable { mutableStateOf(initialLocation) }
    var lastZoom by rememberSaveable { mutableDoubleStateOf(15.0) }
    var hasInitialLocationBeenSet by rememberSaveable { mutableStateOf(false) }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    }

    // Создаем иконку маркера из MaterialIcons.Default.Place
    val iconPainter = rememberVectorPainter(Icons.Default.Place)
    val iconColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val customMarkerIcon = remember(iconPainter, iconColor, density) {
        val sizePx = with(density) { 48.dp.toPx() }.toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
        val drawScope = CanvasDrawScope()
        
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            with(iconPainter) {
                draw(
                    size = androidx.compose.ui.geometry.Size(sizePx.toFloat(), sizePx.toFloat()),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
        BitmapDrawable(context.resources, bitmap)
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = customMarkerIcon
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    DisposableEffect(myLocationOverlay) {
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.runOnFirstFix {
            val location = myLocationOverlay.myLocation
            if (location != null && !hasInitialLocationBeenSet && selectedLocation == null) {
                mapView.post {
                    mapView.controller.animateTo(location)
                    mapView.controller.setZoom(15.0)
                    myLocationOverlay.disableFollowLocation()
                    hasInitialLocationBeenSet = true
                }
            }
        }
        onDispose {
            myLocationOverlay.disableMyLocation()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Место ловли",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(
                        onClick = { selectedLocation?.let { onSaveClick(it) } },
                        enabled = selectedLocation != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = {
                    (mapView.parent as? ViewGroup)?.removeView(mapView)
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(lastZoom)
                        
                        selectedLocation?.let {
                            controller.setCenter(it)
                            marker.position = it
                            if (!overlays.contains(marker)) {
                                overlays.add(marker)
                            }
                        }

                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                selectedLocation = p
                                marker.position = p
                                if (!overlays.contains(marker)) {
                                    overlays.add(marker)
                                }
                                invalidate()
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        })
                        overlays.add(eventsOverlay)
                        
                        if (!overlays.contains(myLocationOverlay)) {
                            overlays.add(myLocationOverlay)
                        }
                    }
                },
                update = { mv ->
                    selectedLocation?.let {
                        marker.position = it
                        if (!mv.overlays.contains(marker)) {
                            mv.overlays.add(marker)
                        }
                        mv.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            FloatingActionButton(
                onClick = {
                    val location = myLocationOverlay.myLocation
                    if (location != null) {
                        mapView.controller.animateTo(location)
                        mapView.controller.setZoom(15.0)
                    } else {
                        myLocationOverlay.enableMyLocation()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Мое местоположение"
                )
            }
        }
    }
}

package com.example.fishing.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
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
    
    var lastCenterLat by rememberSaveable { mutableStateOf(initialLocation?.latitude) }
    var lastCenterLon by rememberSaveable { mutableStateOf(initialLocation?.longitude) }
    var lastZoom by rememberSaveable { mutableDoubleStateOf(15.0) }
    var hasInitialLocationBeenSet by rememberSaveable { mutableStateOf(false) }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
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
            if (location != null && !hasInitialLocationBeenSet && lastCenterLat == null) {
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
                    IconButton(onClick = { 
                        onSaveClick(mapView.mapCenter as GeoPoint)
                    }) {
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
                        if (lastCenterLat != null && lastCenterLon != null) {
                            controller.setCenter(GeoPoint(lastCenterLat!!, lastCenterLon!!))
                        } else if (initialLocation != null) {
                            controller.setCenter(initialLocation)
                        }
                        
                        if (!overlays.contains(myLocationOverlay)) {
                            overlays.add(myLocationOverlay)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Центрированный маркер выбора (белая точка как на картинке)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(16.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {}

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

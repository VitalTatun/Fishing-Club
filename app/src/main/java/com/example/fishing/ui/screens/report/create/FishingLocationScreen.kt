package com.example.fishing.ui.screens.report.create

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import android.location.LocationManager
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fishing.R
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
    initialLocation: GeoPoint? = null,
    onSearchClick: () -> Unit = {},
    searchLocation: GeoPoint? = null,
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
        val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
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
        bitmap.toDrawable(context.resources)
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = customMarkerIcon
            setInfoWindow(null)
        }
    }

    LaunchedEffect(searchLocation) {
        searchLocation?.let { point ->
            selectedLocation = point
            marker.position = point
            hasInitialLocationBeenSet = true
            mapView.post {
                mapView.controller.animateTo(point, 15.0, 500L)
            }
            myLocationOverlay.disableFollowLocation()
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
            if ((location != null) && !hasInitialLocationBeenSet) {
                mapView.post {
                    mapView.controller.animateTo(location, 15.0, 500L)
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
                        text = stringResource(R.string.fishing_place),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(
                        onClick = { selectedLocation?.let { onSaveClick(it) } },
                        enabled = selectedLocation != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
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
        Box(modifier = Modifier
            .padding(top = paddingValues.calculateTopPadding())
            .fillMaxSize()) {
            AndroidView(
                factory = {
                    (mapView.parent as? ViewGroup)?.removeView(mapView)
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(lastZoom)

                        if (selectedLocation != null) {
                            controller.setCenter(selectedLocation)
                        } else if (!hasInitialLocationBeenSet) {
                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            try {
                                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (lastKnown != null) {
                                    controller.setCenter(GeoPoint(lastKnown.latitude, lastKnown.longitude))
                                }
                            } catch (_: SecurityException) { }
                        }

                        selectedLocation?.let {
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

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isMarkerSet = selectedLocation != null
                    IconButton(
                        onClick = {
                            selectedLocation?.let {
                                mapView.controller.animateTo(it, 15.0, 500L)
                            }
                        },
                        enabled = isMarkerSet
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = if (isMarkerSet) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.width(24.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    IconButton(
                        onClick = {
                            val location = myLocationOverlay.myLocation
                            if (location != null) {
                                mapView.controller.animateTo(location, 15.0, 500L)
                            } else {
                                myLocationOverlay.enableMyLocation()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.my_location),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


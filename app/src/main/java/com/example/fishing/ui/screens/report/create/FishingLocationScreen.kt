package com.example.fishing.ui.screens.report.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
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
    onAddWaterNameClick: () -> Unit = {},
    waterName: String = "",
    isPaid: Boolean = false,
    isFishingFromShore: Boolean = true,
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    if (waterName.isEmpty()) {
                        val isEnabled = selectedLocation != null
                        TextButton(
                            onClick = onAddWaterNameClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEnabled
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.add_water_name_button),
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = waterName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton (onClick = onAddWaterNameClick) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isPaid) {
                                WaterBadge(
                                    text = stringResource(R.string.paid),
                                    icon = Icons.Default.AttachMoney
                                )
                            }
                            if (isFishingFromShore) {
                                WaterBadge(
                                    text = stringResource(R.string.fishing_from_shore),
                                    icon = Icons.Default.Place
                                )
                            }
                        }
                    }
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
                    .navigationBarsPadding()
                    .padding(bottom = 130.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.my_location)
                )
            }
        }
    }
}

@Composable
private fun WaterBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

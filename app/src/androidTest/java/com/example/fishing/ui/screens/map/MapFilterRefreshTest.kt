package com.example.fishing.ui.screens.map

import android.content.Context
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MapFilterRefreshTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context
        get() = composeRule.activity

    @Test
    fun markerRefresh_filtersChange_redrawsAndSyncesOverlays() {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val first = marker(firstId, 54.0)
        val second = marker(secondId, 53.0)

        lateinit var mapView: DeterministicMapView
        lateinit var myLocationOverlay: MyLocationNewOverlay
        composeRule.runOnUiThread {
            mapView = DeterministicMapView(context)
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        }

        val markersState = mutableStateOf(listOf(first, second))
        val markers by markersState

        composeRule.setContent {
            FishingTheme {
                Box(Modifier.fillMaxSize()) {
                    OsmMapView(
                        mapView = mapView,
                        myLocationOverlay = myLocationOverlay,
                        modifier = Modifier.fillMaxSize(),
                        markers = markers,
                        onMarkerClick = {},
                        trophyColor = Color.RED,
                        regularColor = Color.CYAN,
                        initialZoom = 6.0
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(
            setOf(firstId.toString(), secondId.toString()),
            overlayTitles(mapView)
        )

        val beforeToggle = mapView.invalidateCount

        markersState.value = listOf(first)

        composeRule.waitForIdle()
        assertEquals(setOf(firstId.toString()), overlayTitles(mapView))
        assertTrue(
            "Changing markers must trigger a MapView redraw",
            mapView.invalidateCount > beforeToggle
        )

        val beforeRestore = mapView.invalidateCount

        markersState.value = listOf(first, second)

        composeRule.waitForIdle()
        assertEquals(
            setOf(firstId.toString(), secondId.toString()),
            overlayTitles(mapView)
        )
        assertTrue(
            "Restoring all markers must trigger a MapView redraw",
            mapView.invalidateCount > beforeRestore
        )
    }

    private fun overlayTitles(mapView: MapView): Set<String> =
        mapView.overlays.filterIsInstance<Marker>().mapNotNull { it.title }.toSet()

    private fun marker(id: UUID, lat: Double) = MarkerDomain(
        id = id,
        name = "reporter",
        waterName = "TestWater",
        waterLat = lat,
        waterLng = 27.0,
        type = FishingType.HAUL,
        fishingMethod = FishingMethod.SPINNING,
        fishingStartAt = null,
        isPublic = true
    )

    /**
     * Tracks MapView.invalidate() calls and skips installing a network tile source so
     * asynchronous tile downloads cannot cause unrelated redraws during the assertions.
     */
    private class DeterministicMapView(context: Context) : MapView(context) {
        var invalidateCount = 0
            private set

        override fun invalidate() {
            invalidateCount++
            super.invalidate()
        }

        override fun setTileSource(aTileSource: ITileSource?) {
            // Intentionally no-op: keep the default empty tile provider.
        }
    }
}
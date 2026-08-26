# Implementation Plan - Water Body Highlighting on Map

This plan outlines the steps to implement highlighting of water bodies (polygons) on the map when a user selects a search result.

## User Review Required

> [!NOTE]
> We will switch from the default Android `Geocoder` to the **Nominatim (OpenStreetMap) API**. This requires internet access for searching and adheres to OSM's usage policy (we should set a User-Agent).

## Proposed Changes

### Data Layer

#### [NEW] [NominatimModels.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/network/NominatimModels.kt)
Define data classes for Nominatim search results, including GeoJSON support.

#### [NEW] [NominatimService.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/network/NominatimService.kt)
Implement a Ktor-based service to fetch search results from `https://nominatim.openstreetmap.org/search`.

### View Model

#### [MODIFY] [MainViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/viewmodel/MainViewModel.kt)
- Add `highlightedPolygon: StateFlow<List<GeoPoint>?>` to store the current polygon to be drawn.
- Add a function `setHighlightedPolygon(List<GeoPoint>?)`.

### UI Layer

#### [MODIFY] [LocationSearchScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/search/LocationSearchScreen.kt)
- Replace `Geocoder` usage with `NominatimService`.
- Update `onLocationSelected` to pass polygon points if available.

#### [MODIFY] [MapScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/map/MapScreen.kt)
- In `OsmMapView`, add logic to render a `org.osmdroid.views.overlay.Polygon` overlay if `highlightedPolygon` is present.
- Clear the polygon when needed (e.g., when a marker is clicked or map is long-pressed).

#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Pass polygon data from `LocationSearchScreen` to `MainViewModel`.

## Verification Plan

### Automated Tests
- N/A for this prototype (UI focused).

### Manual Verification
1. Open the search screen on the map.
2. Search for a known lake (e.g., "Озеро Нарочь" or "Минское море").
3. Select the result.
4. Verify the map centers on the location and displays a blue semi-transparent polygon highlighting the water body.
5. Verify the polygon disappears or updates when another search is performed.

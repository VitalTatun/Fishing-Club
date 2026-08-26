# Implementation Plan - Add Water Name Field on Map Screen

The goal is to add a text field at the bottom of the map selection screen (`FishingLocationScreen`) to allow users to enter the water body name directly while picking the location.

## User Review Required

> [!NOTE]
> The UI element will be placed at the bottom center of the map screen, styled to match the provided image (plus icon and placeholder text).

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/res/values/strings.xml)
- Add `<string name="add_water_name">Добавить название водоема</string>`

### UI Components

#### [MODIFY] [FishingLocationScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/create/FishingLocationScreen.kt)
- Update `FishingLocationScreen` signature to include `initialWaterName: String`.
- Update `onSaveClick` to return both `GeoPoint` and `String`.
- Add internal state `waterName`.
- Add the "Add water name" UI element at the bottom of the map view.

#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Update `"water_edit"` destination to pass `waterName` and receive it back.

#### [MODIFY] [CreateReportScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt)
- Pass `viewModel.formWaterName` to the `"water_edit"` route.

## Verification Plan

### Manual Verification
1. Start creating a new report.
2. Click on the "Water body" section.
3. Observe the new "Add water body name" field at the bottom of the map.
4. Enter a name and select a location.
5. Click the "Check" icon in the top bar.
6. Verify that both the location and the name are correctly updated in the report creation screen.

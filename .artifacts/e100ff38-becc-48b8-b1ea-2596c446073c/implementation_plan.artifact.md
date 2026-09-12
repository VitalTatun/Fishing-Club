# Implementation Plan - P1.1 Photos: Camera + Fullscreen Viewer

This plan covers adding camera capture to the report creation flow and implementing a robust fullscreen photo viewer.

## User Review Required

> [!IMPORTANT]
> - Adding `android.permission.CAMERA` to the manifest.
> - Deleting the existing (non-functional) `FullScreenPhotoScreen.kt` and replacing it with a new implementation.
> - The camera flow will use `ActivityResultContracts.TakePicture` with a temporary file.

## Proposed Changes

### 1. Configuration & Manifest

#### [MODIFY] [AndroidManifest.xml](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.CAMERA" />`.
- Remove `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />` as it's redundant for modern Scoped Storage and Photo Picker.
- Add `<provider>` for `FileProvider` to support sharing URIs for the camera.

### 2. Domain & ViewModel

#### [MODIFY] [ReportField.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/model/ReportField.kt)
- No changes needed to the model itself, but `PhotoPicker` renderer will be updated.

#### [MODIFY] [CreateReportViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/viewmodel/CreateReportViewModel.kt)
- Add utility to create a temporary photo file for the camera.
- Ensure `resetFormState` cleans up any remaining temporary files if necessary (though they are in cache).

### 3. UI - Create Report (Camera)

#### [MODIFY] [ReportFieldRenderer.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/create/ReportFieldRenderer.kt)
- Update `ReportField.PhotoPicker` rendering to support two actions: "Gallery" and "Camera".
- Possibly use an `IconButton` or a simple dialog when the main item is clicked.

#### [MODIFY] [CreateReportScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt)
- Implement `rememberLauncherForActivityResult` for `ActivityResultContracts.TakePicture`.
- Add permission handling logic for `Manifest.permission.CAMERA`.
- Implement a "Source Selector" dialog (Camera vs Gallery).

### 4. UI - Fullscreen Viewer

#### [DELETE] [FullScreenPhotoScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/detail/FullScreenPhotoScreen.kt)
- Remove the old non-functional implementation.

#### [NEW] [PhotoViewerScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/detail/PhotoViewerScreen.kt)
- Implement a clean `HorizontalPager` based viewer.
- Add support for Zoom (pinch and double tap) using a simplified `pointerInput` approach.
- Support loading and error states for images.

### 5. Navigation & Integration

#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Update the `full_screen_photo` destination to use the new `PhotoViewerScreen`.

#### [MODIFY] [ReportHeader.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/components/ReportHeader.kt)
- Make `ReportPhotoCarousel` items clickable to navigate to the fullscreen viewer.

#### [MODIFY] [FishingReportItem.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/components/FishingReportItem.kt)
- (Optional/Secondary) Allow clicking photos in the list if it doesn't conflict with item click. Primary focus is Detail.

## Verification Plan

### Automated Tests
- `CreateReportViewModelTest`: Verify adding camera photos doesn't break the report model.

### Manual Verification
1. **Camera**:
   - Open Create Report.
   - Click "Add Photo" -> Select "Camera".
   - Grant permission.
   - Take a photo.
   - Verify it appears in the list.
   - Save report and verify it's uploaded to Supabase.
2. **Fullscreen Viewer**:
   - Open a report with photos.
   - Click on a photo in the carousel.
   - Verify it opens at the correct index.
   - Swipe between photos.
   - Test zoom if implemented.
   - Press Back and verify return to Detail.

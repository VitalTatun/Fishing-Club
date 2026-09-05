# Implementation Plan - Sorting for Fishing Reports

Add sorting functionality to the Main Screen to allow users to sort reports by "Date Added" (published date) and "Fishing Date".

## User Review Required

> [!IMPORTANT]
> The sorting logic will use `publishedAt` (falling back to `fishingTime` if null) for "Date Added" and `fishingTime` for "Fishing Date". This aligns with existing UI display logic.

## Proposed Changes

### Domain Layer

#### [MODIFY] [FishingReport.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/model/FishingReport.kt)
- Add `ReportSortOrder` enum.

### View Model

#### [MODIFY] [MainViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/viewmodel/MainViewModel.kt)
- Add `reportSortOrder: MutableStateFlow<ReportSortOrder>`.
- Expose `sortedReports: StateFlow<List<FishingReport>>` which combines `reports`, `favoriteReports`, and `reportSortOrder`.
- Add `setSortOrder(ReportSortOrder)` function.

### UI Layer

#### [MODIFY] [MainScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/main/MainScreen.kt)
- Update `MainScreen` to accept `sortedReports` instead of (or in addition to) the raw lists, or simply use the ViewModel's state if available.
- Implement `DropdownMenu` for the `SwapVert` icon to select sorting options.
- Fix the `SwapVert` icon action (it currently incorrectly calls `onCreateReportClick`).

## Verification Plan

### Manual Verification
1. Open the App on the Home tab.
2. Click the `SwapVert` icon in the TopAppBar.
3. Select "По дате добавления" and verify the order (newest published first).
4. Select "По дате рыбалки" and verify the order (newest fishing time first).
5. Verify that adding/removing favorites doesn't break the sorting.

# P0.5 Delete Report Implementation

This plan covers the implementation of the "Delete Report" flow, including backend deletion (Supabase DB + Storage), local cache cleanup (Room Transaction), and UI/Navigation handling.

## User Review Required

> [!IMPORTANT]
> **Storage Strategy: Option A (Storage then DB)**
> I have chosen to delete files from Supabase Storage **before** deleting the report from the Supabase Database.
> * **Why:** This ensures that if Storage deletion fails, the report still exists in the DB, allowing the user to **retry** the operation. A retry will fetch the same storage paths and attempt cleanup again.
> * **Side Effect:** If Storage succeeds but DB fails, the report will temporarily have broken image links until the user successfully retries the deletion. Given this is a delete flow, this is acceptable as the end goal is total removal.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [SupabaseFishingRepository.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/SupabaseFishingRepository.kt)
* Update `deleteReport(id)` to:
    1. Fetch storage paths from Supabase `fishing_photos`.
    2. Delete files from Supabase Storage `fishing_photos`.
    3. Delete report from Supabase DB `fishing` (this triggers CASCADE on backend).
    4. Perform Room cleanup inside `database.withTransaction`.
* Make Storage cleanup failures visible (throw exception) to allow retry if DB is still present.
* Ensure Room transaction wraps all 3 DAO delete calls.

#### [MODIFY] [MarkerDao.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/local/dao/MarkerDao.kt)
* Add `deleteById(id: UUID)` if it's missing or ensure it's correct. (Already exists according to `grep`, but checking).

---

### [Business Logic / ViewModel]

#### [MODIFY] [MainViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/viewmodel/MainViewModel.kt)
* Remove redundant `loadReports(force = true)` and `loadMapMarkers(force = true)` from `deleteReport` success handler.
* Rely on Room Flow for automatic UI updates across Home and Map screens.

---

### [UI & Navigation]

#### [MODIFY] [ReportDetailScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/report/detail/ReportDetailScreen.kt)
* (Verification) Ensure `isDeleting` state correctly disables the "Delete" button and show a progress indicator (already implemented).
* (Verification) Ensure `deleteError` is shown via Snackbar (already implemented).

---

## Verification Plan

### Automated Tests
* **MainViewModelTest**:
    * `delete own report removes it`: Verify it calls repository and sets `deletedReportId`.
    * `delete failure publishes error and allows retry`: Verify error state and subsequent success.
    * `duplicate delete while in-flight is ignored`: Verify double-submit protection.
* **SupabaseFishingRepositoryTest** (if possible, or manual verification):
    * Verify order: Storage -> DB -> Room.
    * Verify Room Transaction usage.

### Manual Verification
1. Open a report owned by the current user.
2. Select "Delete" from the menu.
3. Confirm deletion in the dialog.
4. Observe progress indicator.
5. Verify navigation pops back to the previous screen.
6. Verify the report is gone from Home list and Map.
7. (Simulated) Verify Supabase DB and Storage are empty.

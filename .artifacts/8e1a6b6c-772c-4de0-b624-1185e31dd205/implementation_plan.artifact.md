# P0.4.2 — Stable Report Identity + Idempotency

This plan aims to stabilize identifiers (Report ID, Fish ID, Photo ID) to ensure that retrying a report creation is idempotent and doesn't create duplicate records or orphaned storage files.

## User Review Required

> [!IMPORTANT]
> I will be changing `FishingReport.photo` from `List<String>` to `List<FishingPhoto>`. This requires updating multiple UI components and the repository.
> I will also introduce a `ReportPhoto` wrapper in `CreateReportViewModel` to track photos with stable IDs.

## Proposed Changes

### Domain Models

#### [MODIFY] [FishingReport.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/model/FishingReport.kt)
- Change `photo: List<String>` to `photos: List<FishingPhoto>`.
- `FishingPhoto` will contain a stable `id: UUID` and a `url: String`.

#### [MODIFY] [Fish.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/model/Fish.kt)
- Ensure `Fish` objects used in the form maintain their `id` across retries. (Mostly architectural, but I'll check if any changes are needed to the `copy` logic).

---

### ViewModel layer

#### [MODIFY] [CreateReportViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/viewmodel/CreateReportViewModel.kt)
- Add `private var currentReportId: UUID = UUID.randomUUID()`.
- Introduce `ReportPhoto` data class: `data class ReportPhoto(val id: UUID, val uri: Uri)`.
- Change `formSelectedPhotoUris: List<Uri>` to `formPhotos: List<ReportPhoto>`.
- Update `saveReport` to use `currentReportId`.
- Ensure `currentReportId` is only reset on successful save or explicit form reset.
- Update photo processing to maintain `ReportPhoto.id`.

---

### Data layer

#### [MODIFY] [SupabaseFishingRepository.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/SupabaseFishingRepository.kt)
- Update `saveReport` to use `photo.id` in the storage path: `${userId}/${reportId}/${photoId}.${ext}`.
- Ensure `upsert` is used for all child entities (fish, baits, photos).
- Document assumptions about `fishing_baits` unique constraint.

#### [MODIFY] [SupabaseDtos.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/supabase/SupabaseDtos.kt)
- Ensure `PhotoDto` and `FishDto` use stable IDs provided by the domain models.

---

### UI layer

#### [MODIFY] [FishingReportItem.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/components/FishingReportItem.kt)
#### [MODIFY] [ReportHeader.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/components/ReportHeader.kt)
#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Update usages of `report.photo` to `report.photos.map { it.url }` or similar.

---

## Verification Plan

### Automated Tests
- `CreateReportViewModelTest`: Verify that `currentReportId` remains the same after a failed save attempt.
- `SupabaseFishingRepositoryTest` (if exists): Verify that `upsert` is called with correct IDs and storage paths.
- New test case for partial failure: DB success + Storage failure, verify retry uses same IDs.

### Manual Verification
1. Open Create Report screen.
2. Fill data, add fish, add photo.
3. Simulate network error during save.
4. Verify "Error" state is shown, form data is preserved.
5. Tap "Save" again.
6. Verify (via logs) that the same `report.id` and `storagePath` are used.
7. Verify successful completion resets the form and generates a new `report.id`.

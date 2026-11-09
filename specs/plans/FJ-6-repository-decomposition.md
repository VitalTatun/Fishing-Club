# FJ-6 — Разделение SupabaseFishingRepository на доменные компоненты

## 1. Что есть сейчас

`SupabaseFishingRepository` (1044 строки) одновременно отвечает за:

| Зона ответственности | Методы | Строки |
|---|---|---|
| Лента (пагинация, кэш) | `getHomeReports`, `refreshHomeReports`, `loadMoreHomeReports`, `fetchReportPage` | 100–159 |
| Детали отчёта | `getReportDetails`, `refreshReportDetails` (+ мёртвый `fetchReportDetails`) | 284–352, 702–725 |
| Маркеры карты | `getMapMarkers`, `refreshMapMarkers` | 280–321 |
| Избранное | `loadFavoriteReports`, `getFavoriteReportIds`, `refreshFavoriteIds`, `fetchFavoriteLinks`, `persistFavoriteLinks`, `fetchFavoriteEntities`, `addFavorite`, `removeFavorite` | 161–231, 545–580 |
| Лайки | `getLikeStates`, `refreshLikes`, `addLike`, `removeLike`, `mutateLike`, `preserveRecentLikes` | 582–700 |
| Сохранение/удаление отчёта | `saveReport`, `deleteReport` | 354–543 |
| Фото и Storage | `getPhotoSignedUrl`, `getPhotoSignedUrls`, `isStoragePath` | 727–768 |
| Маппинг DTO/Entity/Domain | `toDomain`, `toMarkerEntity`, `toReportDetailsEntity`, `toFishingDto`, `buildPublishPayload`, `toFishDto`, парсинг дат/enum | 770–1044 |
| Скрытое состояние | пагинация (`homeLoadedOwnCount`, `homeHasMore`), версии лайков/избранного | 66–80 |

### Наблюдения

1. **Мёртвый код:** `fetchReportDetails` (702–725) полностью дублирует тело
   `refreshReportDetails` и не вызывается; `FishingDto.toDomain(fish, baits, photos)`
   (810–842) тоже не используется.
2. **Прямой доступ к Supabase вне `ReportRemoteDataSource`:** `deleteReport` сам
   удаляет объекты Storage и строку из `fishing`; остальные записи уже идут через
   `ReportRemoteDataSource`.
3. **Скрытое состояние на два домена:** `preserveRecentLikes` нужен и ленте, и
   деталям отчёта → компонент лайков должен быть зависимостью этих компонентов,
   а не наоборот.
4. **Downcast в UI-слое:** `MainViewModel` проверяет `repository is
   SupabaseFishingRepository`, чтобы вызвать `refreshReportDetails`. Это мешает
   подмене реализации в тестах.

## 2. Целевая структура

Новый пакет `com.example.fishing.data.report`:

```
data/report/
  ReportRemoteQueries.kt      — интерфейс + Supabase-реализация всех select'ов
  ReportMapper.kt             — DTO ↔ Entity ↔ Domain
  SupabaseDateFormats.kt      — даты, Instant, enum-значения
  PhotoUrlProvider.kt         — signed URL для путей Storage
  LikesStore.kt               — лайки + версии локальных мутаций
  FavoritesStore.kt           — избранное + версии локальных мутаций
  HomeFeedStore.kt            — первая страница/догрузка, состояние пагинации
  ReportDetailsStore.kt       — чтение/обновление одного отчёта
  MarkerStore.kt              — маркеры карты
  ReportWriter.kt             — saveReport/deleteReport (upload → publish → кэш)
```

`SupabaseFishingRepository` остаётся единственной реализацией `FishingRepository`
и превращается в тонкий фасад (~200–250 строк), делегирующий вызовы компонентам.
Публичный контракт `FishingRepository` не меняется.

## 3. Этапы (каждый — отдельный проверяемый коммит)

1. **Мапперы и даты.** Вынести `SupabaseDateFormats` и `ReportMapper`, удалить
   мёртвый код. Покрыть чистые функции unit-тестами. Поведение не меняется.
2. **Фото/Storage.** `PhotoUrlProvider` (`getPhotoSignedUrl`, `getPhotoSignedUrls`,
   `isStoragePath`), батчи по 50 путей.
3. **Remote-чтение.** Вынести все `postgrest["..."].select {}` в
   `ReportRemoteQueries`; репозиторий перестаёт знать таблицы и `Columns`.
4. **Лайки.** `LikesStore` вместе с `likeMutationSeq/versions/opsInFlight` и
   `preserveRecentLikes`. Лента и детали получают его как зависимость.
5. **Избранное.** `FavoritesStore` с версиями мутаций.
6. **Чтение отчётов.** `HomeFeedStore` (пагинация), `ReportDetailsStore`,
   `MarkerStore`.
7. **Запись отчётов.** `ReportWriter`; удаление Storage/строки переносится в
   `ReportRemoteDataSource`, чтобы у записи был один шлюз.
8. **Фасад и Hilt.** Тонкий `SupabaseFishingRepository`, `@Binds` для новых
   интерфейсов. Опционально — добавить `refreshReportDetails` в
   `FishingRepository`, чтобы убрать downcast из `MainViewModel` (изменение
   контракта, требует согласования).

## 4. Правила безопасности рефакторинга

- Никаких изменений логики «по ходу»: только перенос кода и переименования.
- После каждого этапа: `./gradlew :app:testDebugUnitTest`, в конце —
  `:app:assembleDebug`.
- Офлайн-поведение и обработку ошибок (`CancellationException` rethrow,
  best-effort cleanup, транзакции Room) сохранять дословно.
- Новые компоненты не создают циклических зависимостей: запись/чтение зависят от
  `LikesStore`/`FavoritesStore`, обратного направления нет.

## 5. Проверка результата

- `SupabaseFishingRepository` ≤ ~250 строк, без `postgrest`/`storage` обращений.
- Существующие тесты `SupabaseFishingRepositoryTest` проходят без изменения
  сценариев.
- Добавлены тесты на маппинг, даты и изоляцию новых компонентов.

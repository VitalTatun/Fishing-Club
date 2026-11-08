# Аудит RLS Supabase — FJ-4

**Дата:** 2026-11-08
**Проект:** Fishing Journal (Supabase project `newwheannhymapusagnm`, сайт `nibbbana36.atlassian.net`)
**Задача:** [FJ-4](https://nibbbana36.atlassian.net/browse/FJ-4) — проверить и задокументировать RLS-политики Supabase
**Метод:** инспекция `pg_policies` / `pg_tables` / `information_schema`, разбор `SECURITY DEFINER`-функций, SQL-сценарии под ролями `authenticated` и `anon` с откатом транзакции, сверка с клиентским кодом (`app/src/main/...`).

> Все проверки выполнялись в транзакциях с `rollback` — данные проекта не изменялись. Миграции из этого PR **не применялись** к проекту.

## 1. Модель владения

| Ресурс | Владелец | Как определяется |
| --- | --- | --- |
| `public.fishing` | `user_id` | прямая колонка |
| `public.fishing_fish` | через `fishing_id` | `EXISTS (fishing.user_id = auth.uid())` |
| `public.fishing_baits` | через `fishing_id` | `EXISTS (fishing.user_id = auth.uid())` |
| `public.fishing_photos` | через `fishing_id` | `EXISTS (fishing.user_id = auth.uid())` |
| `public.favorites` | `user_id` | прямая колонка |
| `public.likes` | `user_id` | прямая колонка |
| `public.profiles` | `id` | `id = auth.uid()` |
| `storage.objects` (`fishing_photos`) | префикс пути | `<user_id>/<report_id>/<photo_id>.<ext>` |
| `storage.objects` (`avatars`) | сегмент пути | `avatars/<user_id>/<uuid>.<ext>` |

Пути хранения в коде:
- фото отчётов — `SupabaseFishingRepository.kt:398`: `"${report.userId}/${report.id}/${photo.id}.$ext"`;
- аватары — `SupabaseAuthRepository.kt:167`: `"avatars/${currentUser.id}/${UUID.randomUUID()}.$ext"`.

## 2. Матрица RLS

RLS включён на всех таблицах `public` и на `storage.objects`.

| Таблица | RLS | Политики | SELECT | INSERT | UPDATE | DELETE |
| --- | --- | --- | --- | --- | --- | --- |
| `fishing` | ✅ | `fishing_select`, `fishing_insert_own`, `fishing_update_own`, `fishing_delete_own` | свои **или** `is_public = true` | `user_id = auth.uid()` | свои (`USING`, он же `WITH CHECK`) | свои |
| `fishing_fish` | ✅ | `fishing_fish_select`, `fishing_fish_write_own` (ALL) | родитель свой или публичный | родитель свой | родитель свой | родитель свой |
| `fishing_baits` | ✅ | `fishing_baits_select`, `fishing_baits_write_own` (ALL) | родитель свой или публичный | родитель свой | родитель свой | родитель свой |
| `fishing_photos` | ✅ | `fishing_photos_select`, `fishing_photos_write_own` (ALL) | родитель свой или публичный | родитель свой | родитель свой (**путь не проверяется — см. F-1**) | родитель свой |
| `favorites` | ✅ | `favorites_select_own`, `favorites_insert_own`, `favorites_delete_own` | свои | `user_id = auth.uid()` | — | свои |
| `likes` | ✅ | `likes_select_own`, `likes_insert_own`, `likes_delete_own` | свои (только `authenticated`) | свой `user_id` + видимый отчёт | — | свои (только `authenticated`) |
| `profiles` | ✅ | `profiles_select_own`, `profiles_select_for_authenticated`, `profiles_insert_own`, `profiles_update_own` | свои **или любой `authenticated`** (см. F-2) | `id = auth.uid()` | свои | — |
| `storage.objects` | ✅ | см. ниже | по bucket/пути | только свой префикс | только свой префикс | только свой префикс |

Storage-политики (`storage.objects`):

| Политика | Bucket | Операция | Условие |
| --- | --- | --- | --- |
| `avatars_public_read` | `avatars` | SELECT | `bucket_id = 'avatars'` |
| `avatars_insert_own` / `avatars_update_own` | `avatars` | INSERT / UPDATE | 2-й сегмент пути = `auth.uid()` |
| `avatars_delete_own` | `avatars` | DELETE | 2-й сегмент пути = `auth.uid()` |
| `fishing_photos_storage_read_own` | `fishing_photos` | SELECT | 1-й сегмент пути = `auth.uid()` |
| `fishing_photos_storage_insert_own` / `_update_own` / `_delete_own` | `fishing_photos` | INSERT / UPDATE / DELETE | 1-й сегмент пути = `auth.uid()` |
| `public_fishing_photos_read` | `fishing_photos` | SELECT | существует `fishing_photos.storage_path = objects.name` у публичного отчёта |

Buckets: `avatars` — public, `fishing_photos` — **private** (контент отдаётся через signed URL).

## 3. Проверенные сценарии

Актор — существующий пользователь; жертва — другой пользователь, владелец приватного отчёта. Все операции в `BEGIN … ROLLBACK`.

| # | Сценарий | Ожидание | Факт | Итог |
| --- | --- | --- | --- | --- |
| A | `authenticated` читает `profiles` | только свой профиль | видны **все 8 профилей и 8 e-mail** | ❌ F-2 |
| B | чужой приватный отчёт | недоступен | 0 строк | ✅ |
| C | чужой публичный отчёт: SELECT | доступен | 1 строка | ✅ (by design) |
| C | чужой отчёт: UPDATE / DELETE | 0 строк | 0 / 0 | ✅ |
| D | прямое чтение чужого приватного объекта Storage | 0 строк | 0 | ✅ |
| E | вставка `fishing_photos.storage_path` = путь чужого приватного файла | запрещено | **вставка успешна** | ❌ F-1 |
| F | `anon` читает чужой объект после шага E | недоступен | **1 объект доступен** | ❌ F-1 (critical) |
| F | `anon` читает приватный отчёт | недоступен | 0 строк | ✅ |
| G | `authenticated` меняет свой `likes_count` напрямую | запрещено | **обновлено 2 строки** | ❌ F-3 |
| G | `authenticated` вставляет отчёт с `likes_count = 123456` | значение игнорируется | **сохранено 123456** | ❌ F-3 |
| H | `anon` вставляет отчёт | запрещено | `42501 new row violates row-level security policy` | ✅ |

Воспроизводимый скрипт: [`supabase/tests/fj4_rls_checks.sql`](../../supabase/tests/fj4_rls_checks.sql).

## 4. Найденные проблемы

### F-1 (Critical) — IDOR через `fishing_photos.storage_path` → утечка приватных фото

Политика `fishing_photos_write_own` проверяет только владение родительским отчётом, но не формат `storage_path`. Пользователь может:

1. создать свой **публичный** отчёт;
2. добавить в него `fishing_photos` с `storage_path` любого чужого объекта;
3. политика `public_fishing_photos_read` начнёт отдавать чужой объект всем, включая `anon`.

Даже «приватный» бакет `fishing_photos` перестаёт защищать файл. Дополнительный риск: отчёт мог быть публичным, а затем снят с публикации — путь и ID остаются известны, и доступ не отзывается.

Подтверждение: сценарии E и F (0 → 1 доступный объект).

Исправление: `supabase/migrations/20261108000000_fj4_photo_path_ownership.sql` — проверять, что первый сегмент `storage_path` равен `auth.uid()`, и требовать совпадения владельца пути в `public_fishing_photos_read`.

### F-2 (High) — любой авторизованный читает e-mail всех пользователей

Политика `profiles_select_for_authenticated` (`auth.role() = 'authenticated'`) открывает `SELECT` на всю таблицу, включая колонку `email`. Клиент запрашивает `profiles` без списка колонок (`SupabaseFishingRepository.kt:259,342,719`), то есть получает e-mail всех авторов.

Подтверждение: сценарий A — 8 из 8 профилей и e-mail.

Исправление (требует синхронного обновления клиента): `supabase/migrations/20261108020000_fj4_profiles_email_privacy.sql` — публичное представление `profiles_public` (`id, name, avatar_url, created_at`) и чтение `profiles` только своей строки. В клиенте заменить чтение авторов на `profiles_public`; запись профиля (upsert) оставить в `profiles`.

### F-3 (Medium) — подделка `likes_count`

RLS разрешает владельцу обновлять любые колонки своего отчёта, а `INSERT` не ограничивает `likes_count`. Счётчик можно накрутить прямыми REST-запросами в обход RPC `like_report`/`unlike_report`.

Подтверждение: сценарий G.

Исправление: `supabase/migrations/20261108010000_fj4_likes_count_integrity.sql` — отозвать табличные `INSERT/UPDATE` и выдать колоночные права без `likes_count`. RPC остаются рабочими, так как `like_report`/`unlike_report` — `SECURITY DEFINER`.

### F-4 (Low) — `handle_new_user`: mutable `search_path` и вызов через RPC

`SECURITY DEFINER`-функция без фиксированного `search_path`, `EXECUTE` выдан `anon`/`authenticated`. Прямой вызов как trigger-функции падает, но это нарушение least privilege и рекомендаций Supabase linter (`function_search_path_mutable`, `anon_security_definer_function_executable`).

Исправление: в `20261108030000_fj4_function_and_grants_hardening.sql` — `SET search_path = ''` и `REVOKE EXECUTE`.

### F-5 (Low) — избыточные привилегии `anon`

У `anon` есть табличные `INSERT/UPDATE/DELETE/TRUNCATE` на все таблицы `public`. RLS блокирует DML, но `TRUNCATE` RLS не покрывает, а сами гранты нарушают least privilege. Через PostgREST `TRUNCATE` недоступен, поэтому оценка — Low.

Исправление: тот же файл `20261108030000_…` — отзыв write-грантов у `anon` и `TRUNCATE` у `anon`/`authenticated`.

### F-6 (Info) — рекомендации без изменения схемы

- **Leaked password protection** выключена (Supabase Auth) — включить в Dashboard → Authentication → Providers.
- **49 orphan-объектов** в `fishing_photos` без строки в `fishing_photos` — не уязвимость, но стоит периодически чистить (`OrphanPhotoCleaner` чистит только локальные кандидаты).
- `favorites` INSERT не проверяет видимость отчёта — можно добавить в избранное произвольный UUID. Утечки данных нет, поэтому оставлено как есть.
- `likes_select_own` не даёт увидеть, кто лайкнул (только свой лайк). Функциональное ограничение, не безопасность.
- `fishing` доступен `anon` для публичных отчётов — соответствует публичной ленте, но стоит подтвердить продуктово, нужен ли анонимный доступ.

## 5. Что проверено и работает корректно

- RLS включён на всех таблицах.
- Чужие приватные отчёты, их дочерние строки и файлы недоступны (B, D).
- Чужие записи нельзя обновить или удалить (C).
- Нельзя создать отчёт от имени другого пользователя или без авторизации (H).
- Storage-политики корректно ограничивают прямые операции по префиксу `<uid>/`.
- `like_report` / `unlike_report` проверяют `auth.uid()` и видимость отчёта, `search_path` зафиксирован.
- `publish_report` проверяет `user_id` и не перезаписывает чужие отчёты (RLS + `WHERE f.user_id = v_uid`).

## 6. Порядок внедрения

1. Применить `20261108000000_fj4_photo_path_ownership.sql` — закрывает F-1, клиентских изменений не требует.
2. Применить `20261108010000_fj4_likes_count_integrity.sql` — закрывает F-3.
3. Применить `20261108030000_fj4_function_and_grants_hardening.sql` — F-4, F-5.
4. Обновить клиент на `profiles_public` и только затем применить `20261108020000_fj4_profiles_email_privacy.sql` (F-2).
5. Повторно прогнать `supabase/tests/fj4_rls_checks.sql` — сценарии E/F/G должны давать 0/0/0, A — только свой профиль.

## 7. Ограничения аудита

- Проверялась только RLS/REST/Storage-поверхность; Edge Functions и прямые подключения к БД вне рамок.
- Тесты выполнялись от имени существующих пользователей через `request.jwt.claims`; отдельные тестовые аккаунты не создавались.
- `TRUNCATE` через PostgREST недоступен, оценка F-5 основана на грамматике привилегий, а не на эксплуатации.

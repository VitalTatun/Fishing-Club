# План: ACH-01 — каталог рыб

**Статус:** реализовано 2026-11-22 (сервер + Android). Проверка: `:app:testDebugUnitTest`, `:app:assembleDebug`.

## Обзор

Каталог пресноводных видов рыб Беларуси и соседних водоёмов — общий источник истины
для поиска на экране улова (`ACH-01a`), фильтров и системы достижений. Каждый вид
получает постоянный ID, каноническое имя, алиасы и признак популярности. Свободный
ввод сохраняется: неизвестная рыба учитывается по нормализованному имени и
сопоставляется с каталогом, когда появляется подходящий вид.

Источник истины — Supabase, локальный кэш — Room, начальный сид — в APK. Это
соответствует [принятому решению №7](../../docs/to-do.md): «Supabase — источник истины
и место выдачи наград; Room — локальный кэш».

## Цели

1. Таблица `public.fish_species` с постоянными ID, алиасами и популярностью.
2. Серверные правила нормализации и сопоставления названий, общие с клиентом.
3. Хранение `species_id` в `fishing_fish` без изменения контракта `publish_report`.
4. Android-модель, Room-кэш и репозиторий каталога для поиска и достижений.
5. Начальный сид в APK для offline-first и первого запуска.
6. Тесты правил сопоставления и кэша.

## Scope

- **Внутри:** схема Supabase, сид каталога, `fishing_fish.species_id`, нормализация,
  сопоставление, Android-слой каталога (домен, Room, репозиторий, сид).
- **Вне:** экран поиска (`ACH-01a`), глобальная частота (`ACH-09`), выдача наград
  (`ACH-03`), каталог водоёмов (`ACH-08`).

## Принятые решения

| Вопрос | Решение |
|---|---|
| Источник истины | Supabase; Room — кэш; сид в APK для offline |
| Формат ID | стабильный латинский slug (`esox_lucius`), не UUID |
| Имена | канонические русские + алиасы (падежи, синонимы, `ё/е`) |
| Популярность | `is_popular` + `sort_order`, 10 видов |
| Свободный ввод | разрешён; `species_id` может быть `null` |
| Сопоставление на сервере | триггер на `fishing_fish` по нормализованному имени |
| Сопоставление на клиенте | те же правила: канон → алиас → prefix → contains (для поиска) |
| `publish_report` | не меняется: `species_id` заполняет триггер |
| Начальный объём | 54 вида (популярные + обычные), расширяется миграцией |

## Схема Supabase

`public.fish_species`:

| Поле | Тип | Смысл |
|---|---|---|
| `id` | `text` PK | постоянный slug вида |
| `canonical_name` | `text not null` | отображаемое имя |
| `aliases` | `text[] not null default '{}'` | варианты написания |
| `is_popular` | `boolean not null default false` | fallback-список |
| `sort_order` | `int not null default 0` | порядок популярных |
| `created_at`, `updated_at` | `timestamptz` | служебные |

- RLS включён, есть только `SELECT` для `anon` и `authenticated`; запись — `service_role`.
- `public.normalize_fish_name(text)` — trim, схлопывание пробелов, `lower`, `ё→е`, пустое → `null`.
- `public.resolve_fish_species(text)` — точное совпадение канона или алиаса; при неоднозначности
  приоритет: канон, затем популярный, затем `sort_order`.
- `public.fishing_fish_resolve_species()` — `BEFORE INSERT OR UPDATE` триггер на `fishing_fish`:
  `species_id = coalesce(resolve(name), species_id)`.
- `fishing_fish.species_id text null references fish_species(id) on delete set null` + индекс.
- Backfill: существующие строки получают `species_id` по имени.

## Каталог

Утверждённый список — 54 позиции (дубликаты «Сиг» и «Сом» схлопнуты). Источник —
миграция `20261122125849_sync_fish_species_list.sql`; клиентский ассет повторяет её.

- ID — латинские slug'и; переименование отображаемого имени не меняет ID.
- Алиасы включают падежи и распространённые синонимы: «плотва» → сорога, чебак;
  «лещ» → подлещик; «корюшка» → снеток; «уклея» → уклейка; «сырть» → рыбец.
- Спорные формы: «подлещик» — алиас леща; «карась золотой» — алиас карася;
  карп и сазан — отдельные позиции (культурная и дикая формы).
- Обобщённые группы получают групповой ID `*_spp`, конкретные виды — в алиасах:
  Бычок, Минога, Осетр, Толстолобик, Буффало.
- Популярные: Плотва, Карась, Щука, Лещ, Окунь, Язь, Судак, Линь, Сом, Голавль.
- Расширение — только новой миграцией `insert ... on conflict (id) do update`.

## Правила сопоставления

1. Нормализация: `trim`, схлопывание пробелов, `lowercase(Locale.ROOT)`, `ё → е`.
2. Для ID: точное совпадение с каноническим именем, затем с алиасом.
3. Для поиска (`ACH-01a`): точное совпадение → `startsWith` → `contains`; ранжирование
   по типу совпадения, затем `sort_order`; дедупликация по `id`.
4. Свободный ввод: имя сохраняется как введено (с обрезкой), `species_id` — `null`,
   если совпадения нет; при совпадении ID проставляется.
5. Уникальность видов для достижений: `species_id`, иначе нормализованное имя.

## Android-часть

- Домен `FishSpecies(id, canonicalName, aliases, isPopular, sortOrder)`.
- Room: `FishSpeciesEntity` + `FishSpeciesDao` (`observeAll`, `observePopular`, `replaceAll`,
  `count`), `AppDatabase` 13 → 14, регистрация в `DatabaseModule`.
- `FishCatalogRepository`: `observeAll`, `observePopular`, `suspend refresh()`.
  Реализация: Room + Supabase-запрос + сид из `assets/fish_species.json` при пустой таблице.
- `FishNameNormalizer` и `FishCatalogMatcher` — чистые функции, общие для поиска и записи.
- `Fish` получает `speciesId: String? = null`; `FishDto` — `@SerialName("species_id")`.
- Отдельная тестовая реализация каталога для Compose/unit-тестов.

## Шаги

### Step 1: Серверная схема и сид

**Действия:**
1. Добавить миграции `supabase/migrations/20261122125238_create_fish_species.sql`
   (таблица, RLS, индексы, функции), `20261122125311_seed_fish_species.sql`
   (первоначальный сид, `fishing_fish.species_id`, триггер, backfill) и
   `20261122125849_sync_fish_species_list.sql` (утверждённый список из 54 позиций).
2. Применить миграцию и проверить `list_tables`, `get_advisors(security)`.
3. Проверить: `select count(*) from fish_species`, резолв «щук» → `esox_lucius`,
   существующие `fishing_fish` получили `species_id` там, где имя совпало.

**Критерии:**
- [ ] Таблица создана, RLS включён, SELECT доступен `anon`.
- [ ] Утверждённый список: 54 позиции, 10 популярных.
- [ ] Триггер заполняет `species_id` на insert/update.
- [ ] Backfill не падает на незнакомых названиях.

**Зависимости:** нет.

### Step 2: Сид в APK

**Действия:**
1. `app/src/main/assets/fish_species.json` — тот же список, что в миграции.
2. Загрузчик сида: при пустой таблице Room заполняет её из ассета.
3. Тест на соответствие количества/уникальности ID и сортировке.

**Критерии:**
- [ ] Первый запуск без сети даёт полный каталог.
- [ ] ID в ассете и на сервере совпадают.

**Зависимости:** Step 1.

### Step 3: Android-слой каталога

**Действия:**
1. `FishSpecies`, `FishSpeciesEntity`, DAO, миграция Room 13 → 14.
2. `FishCatalogRemoteDataSource` (Supabase `fish_species`) и `FishCatalogRepository`.
3. Синхронизация: `refresh()` заменяет локальные строки серверными, сид — только при пустой таблице.
4. Hilt-биндинги + mock для тестов.

**Критерии:**
- [ ] Каталог наблюдается как `Flow`, переживает перезапуск.
- [ ] `refresh()` не ломает офлайн-режим.
- [ ] Популярные отдаются в порядке `sort_order`.

**Зависимости:** Step 1, Step 2.

### Step 4: Нормализация и сопоставление на клиенте

**Действия:**
1. `normalizeFishName` — те же правила, что в SQL.
2. `resolveSpecies(name)` и `searchSpecies(query)`.
3. `Fish.speciesId` в модели, DTO и мапперах; заполнение при выборе из каталога и при вводе.

**Критерии:**
- [ ] «Щук» находит «Щука»; «ёрш» и «ерш» — один вид.
- [ ] «подлещик» резолвится в `abramis_brama`.
- [ ] Свободный ввод не ломается.

**Зависимости:** Step 3.

### Step 5: Тесты и проверка

**Действия:**
1. Unit: нормализация, резолв, алиасы, популярные, лимиты.
2. Robolectric: миграция 13 → 14, сид, DAO.
3. `./gradlew :app:testDebugUnitTest` и `:app:assembleDebug`.

**Критерии:**
- [ ] Тесты зелёные, существующие не сломаны.

**Зависимости:** Step 2–4.

## Риски и снижение

- **Триггер перезаписывает явный `species_id`.** Пока приоритет у имени; при появлении
  явных ID в payload (позже) правило `coalesce` нужно пересмотреть.
- **Омонимы и опечатки.** Резолв только по точному канону/алиасу; опечатки остаются
  свободным вводом.
- **Дрейф сида и сервера.** Тест сверяет ID/количество; сервер — источник истины.
- **Миграция Room.** Добавляется новая таблица, данные не теряются.
- **Офлайн-первый запуск.** Полный сид в APK; сервер лишь обновляет его.

## Проверка

- `supabase_get_advisors(type: security)` после миграции.
- `select count(*), count(*) filter (where is_popular) from fish_species;`
- `select id, name, species_id from fishing_fish order by fishing_id limit 20;`
- `./gradlew :app:testDebugUnitTest`

## References

- Достижения: [docs/Achivements/achivements.md](../../docs/Achivements/achivements.md)
- Очередь: [docs/to-do.md](../../docs/to-do.md)
- Экран поиска: [ACH-01a](ACH-01a-catch-search.md)
- Серверная схема: `supabase/migrations/`, `data/supabase/SupabaseDtos.kt`
- Клиент: `data/FishingRepository.kt`, `data/report/ReportMapper.kt`, `data/local/AppDatabase.kt`
- Модель: `model/Fish.kt`

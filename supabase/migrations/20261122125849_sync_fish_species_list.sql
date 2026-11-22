-- ACH-01: утверждённый список видов (54 позиции, дубликаты Сиг/Сом схлопнуты).
-- Заменяет первоначальный сид из 20261122125311_seed_fish_species.sql.
-- Обобщённые группы (Бычок, Минога, Осетр, Толстолобик, Буффало) получают
-- групповые ID *_spp; конкретные виды внутри группы перечислены в алиасах.

-- 1. Удаляем виды, которых нет в утверждённом списке -------------------------

delete from public.fish_species
 where id not in (
    'ctenopharyngodon_idella',   -- Амур
    'ballerus_sapa',             -- Белоглазка
    'bester',                    -- Бестер
    'alburnoides_bipunctatus',   -- Быстрянка
    'neogobius_spp',             -- Бычок
    'polyodon_spathula',         -- Веслонос
    'leucaspius_delineatus',     -- Верховка
    'misgurnus_fossilis',        -- Вьюн
    'squalius_cephalus',         -- Голавль
    'barbatula_barbatula',       -- Голец
    'perccottus_glenii',         -- Ротан
    'phoxinus_phoxinus',         -- Гольян
    'rhodeus_amarus',            -- Горчак
    'blicca_bjoerkna',           -- Густера
    'leuciscus_leuciscus',       -- Елец
    'gymnocephalus_cernua',      -- Ёрш
    'aspius_aspius',             -- Жерех
    'carassius_gibelio',         -- Карась
    'cyprinus_carpio',           -- Карп
    'osmerus_eperlanus',         -- Корюшка
    'silurus_glanis',            -- Сом
    'scardinius_erythrophthalmus', -- Красноперка
    'lampetra_spp',              -- Минога
    'salmo_trutta',              -- Кумжа
    'abramis_brama',             -- Лещ
    'tinca_tinca',               -- Линь
    'salmo_salar',               -- Лосось
    'lota_lota',                 -- Налим
    'perca_fluviatilis',         -- Окунь
    'acipenser_spp',             -- Осетр
    'coregonus_peled',           -- Пелядь
    'gobio_gobio',               -- Пескарь
    'rutilus_rutilus',           -- Плотва
    'cottus_gobio',              -- Подкаменщик
    'chondrostoma_nasus',        -- Подуст
    'hypophthalmichthys_spp',    -- Толстолобик
    'coregonus_albula',          -- Ряпушка
    'cyprinus_carpio_wild',      -- Сазан
    'coregonus_lavaretus',       -- Сиг
    'ballerus_ballerus',         -- Синец
    'sander_lucioperca',         -- Судак
    'vimba_vimba',               -- Сырть
    'anguilla_anguilla',         -- Угорь
    'alburnus_alburnus',         -- Уклея
    'barbus_barbus',             -- Усач
    'salmo_trutta_fario',        -- Форель
    'thymallus_thymallus',       -- Хариус
    'pseudorasbora_parva',       -- Чебачек
    'pelecus_cultratus',         -- Чехонь
    'acipenser_ruthenus',        -- Стерлядь
    'ictiobus_spp',              -- Буффало
    'cobitis_taenia',            -- Щиповка
    'esox_lucius',               -- Щука
    'leuciscus_idus'             -- Язь
 );

-- 2. Утверждённый каталог ----------------------------------------------------

insert into public.fish_species (id, canonical_name, aliases, is_popular, sort_order) values
    ('rutilus_rutilus', 'Плотва', array['сорога', 'чебак', 'плотва обыкновенная', 'плотвы', 'плотву'], true, 1),
    ('carassius_gibelio', 'Карась', array['карась серебряный', 'серебряный карась', 'карась золотой', 'золотой карась', 'караси', 'карася'], true, 2),
    ('esox_lucius', 'Щука', array['щука обыкновенная', 'щуки', 'щуку', 'щучка'], true, 3),
    ('abramis_brama', 'Лещ', array['лещ обыкновенный', 'лещи', 'леща', 'подлещик', 'подлещики', 'молодой лещ'], true, 4),
    ('perca_fluviatilis', 'Окунь', array['окунь обыкновенный', 'окунь речной', 'окуни', 'окуня', 'полосатик'], true, 5),
    ('leuciscus_idus', 'Язь', array['язь обыкновенный', 'язи', 'язя'], true, 6),
    ('sander_lucioperca', 'Судак', array['судак обыкновенный', 'судаки', 'судака'], true, 7),
    ('tinca_tinca', 'Линь', array['линь обыкновенный', 'лини', 'линя'], true, 8),
    ('silurus_glanis', 'Сом', array['сом обыкновенный', 'сомы', 'сома', 'европейский сом'], true, 9),
    ('squalius_cephalus', 'Голавль', array['голавли', 'голавля', 'головень'], true, 10),
    ('ctenopharyngodon_idella', 'Амур', array['белый амур', 'амур белый', 'амуры'], false, 100),
    ('ballerus_sapa', 'Белоглазка', array['белоглазка обыкновенная', 'глазач', 'клепец'], false, 101),
    ('bester', 'Бестер', array['бестер', 'гибрид осетра и стерляди'], false, 102),
    ('alburnoides_bipunctatus', 'Быстрянка', array['быстрянка обыкновенная'], false, 103),
    ('neogobius_spp', 'Бычок', array['бычок-песочник', 'бычок песочник', 'бычок-кругляк', 'бычок кругляк', 'бычки'], false, 104),
    ('polyodon_spathula', 'Веслонос', array['веслонос обыкновенный'], false, 105),
    ('leucaspius_delineatus', 'Верховка', array['овсянка', 'верховодка', 'верховки'], false, 106),
    ('misgurnus_fossilis', 'Вьюн', array['вьюн обыкновенный', 'вьюны'], false, 107),
    ('barbatula_barbatula', 'Голец', array['голец усатый', 'усатый голец', 'гольцы'], false, 108),
    ('perccottus_glenii', 'Ротан', array['ротан-головешка', 'головешка', 'ротаны'], false, 109),
    ('phoxinus_phoxinus', 'Гольян', array['гольян обыкновенный', 'гольяны'], false, 110),
    ('rhodeus_amarus', 'Горчак', array['горчак обыкновенный', 'горчаки'], false, 111),
    ('blicca_bjoerkna', 'Густера', array['густеры', 'густеру', 'плоскирка'], false, 112),
    ('leuciscus_leuciscus', 'Елец', array['ельцы', 'ельца', 'елец обыкновенный'], false, 113),
    ('gymnocephalus_cernua', 'Ёрш', array['ерш', 'ерш обыкновенный', 'ерши', 'ерша'], false, 114),
    ('aspius_aspius', 'Жерех', array['жерех обыкновенный', 'жерехи', 'шереспер'], false, 115),
    ('cyprinus_carpio', 'Карп', array['карп обыкновенный', 'карпы', 'карпа'], false, 116),
    ('osmerus_eperlanus', 'Корюшка', array['корюшка европейская', 'корюшки', 'снеток', 'снетки'], false, 117),
    ('scardinius_erythrophthalmus', 'Красноперка', array['краснопёрка', 'красноперки', 'краснопёрки'], false, 118),
    ('lampetra_spp', 'Минога', array['ручьевая минога', 'минога ручьевая', 'украинская минога', 'минога украинская', 'миноги'], false, 119),
    ('salmo_trutta', 'Кумжа', array['кумжа', 'черноморский лосось'], false, 120),
    ('salmo_salar', 'Лосось', array['лосось атлантический', 'атлантический лосось', 'семга', 'сёмга'], false, 121),
    ('lota_lota', 'Налим', array['налим обыкновенный', 'налимы', 'налима'], false, 122),
    ('acipenser_spp', 'Осетр', array['русский осетр', 'осетр русский', 'сибирский осетр', 'осетры'], false, 123),
    ('coregonus_peled', 'Пелядь', array['пелядь', 'сырок'], false, 124),
    ('gobio_gobio', 'Пескарь', array['пескарь обыкновенный', 'пескари', 'пескаря'], false, 125),
    ('cottus_gobio', 'Подкаменщик', array['подкаменщик обыкновенный', 'широколобка'], false, 126),
    ('chondrostoma_nasus', 'Подуст', array['подуст обыкновенный', 'подусты'], false, 127),
    ('hypophthalmichthys_spp', 'Толстолобик', array['белый толстолобик', 'пестрый толстолобик', 'пёстрый толстолобик', 'толстолобики'], false, 128),
    ('coregonus_albula', 'Ряпушка', array['ряпушка европейская', 'рипус', 'ряпушки'], false, 129),
    ('cyprinus_carpio_wild', 'Сазан', array['сазан обыкновенный', 'дикий карп', 'сазаны'], false, 130),
    ('coregonus_lavaretus', 'Сиг', array['сиг обыкновенный', 'сиги'], false, 131),
    ('ballerus_ballerus', 'Синец', array['синец обыкновенный', 'сопа'], false, 132),
    ('vimba_vimba', 'Сырть', array['рыбец', 'рыбцы', 'сырть'], false, 133),
    ('anguilla_anguilla', 'Угорь', array['угорь речной', 'европейский угорь', 'угри'], false, 134),
    ('alburnus_alburnus', 'Уклея', array['уклейка', 'уклейки', 'уклейку'], false, 135),
    ('barbus_barbus', 'Усач', array['усач обыкновенный', 'мирон', 'усачи'], false, 136),
    ('salmo_trutta_fario', 'Форель', array['форель ручьевая', 'ручьевая форель', 'пеструшка'], false, 137),
    ('thymallus_thymallus', 'Хариус', array['хариус европейский', 'хариусы'], false, 138),
    ('pseudorasbora_parva', 'Чебачек', array['чебачок', 'амурский чебачок', 'чебачок амурский'], false, 139),
    ('pelecus_cultratus', 'Чехонь', array['чехони', 'сабля-рыба'], false, 140),
    ('acipenser_ruthenus', 'Стерлядь', array['стерляди', 'стерлядь'], false, 141),
    ('ictiobus_spp', 'Буффало', array['буффало большеротый', 'буффало малоротый', 'рыба-буффало'], false, 142),
    ('cobitis_taenia', 'Щиповка', array['щиповка обыкновенная', 'щиповки', 'вьюн щиповка'], false, 143)
on conflict (id) do update set
    canonical_name = excluded.canonical_name,
    aliases = excluded.aliases,
    is_popular = excluded.is_popular,
    sort_order = excluded.sort_order,
    updated_at = now();

-- 3. Пересчитываем species_id у существующих строк ---------------------------

update public.fishing_fish
   set species_id = public.resolve_fish_species(name);

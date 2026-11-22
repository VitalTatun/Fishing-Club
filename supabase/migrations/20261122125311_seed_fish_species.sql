-- ACH-01 (часть 2/2): начальный каталог и species_id в улове.
-- Каталог расширяется только новой миграцией (insert ... on conflict ... do update).

-- 1. Начальный каталог (54 вида, 10 популярных) -------------------------------

insert into public.fish_species (id, canonical_name, aliases, is_popular, sort_order) values
    ('rutilus_rutilus', 'Плотва', array['сорога', 'чебак', 'плотва обыкновенная', 'плотвы', 'плотву'], true, 1),
    ('carassius_gibelio', 'Карась', array['карась серебряный', 'серебряный карась', 'караси', 'карася'], true, 2),
    ('esox_lucius', 'Щука', array['щука обыкновенная', 'щуки', 'щуку', 'щучка'], true, 3),
    ('abramis_brama', 'Лещ', array['лещ обыкновенный', 'лещи', 'леща', 'подлещик', 'подлещики', 'молодой лещ'], true, 4),
    ('perca_fluviatilis', 'Окунь', array['окунь обыкновенный', 'окунь речной', 'окуни', 'окуня', 'полосатик'], true, 5),
    ('leuciscus_idus', 'Язь', array['язь обыкновенный', 'язи', 'язя'], true, 6),
    ('sander_lucioperca', 'Судак', array['судак обыкновенный', 'судаки', 'судака'], true, 7),
    ('tinca_tinca', 'Линь', array['линь обыкновенный', 'лини', 'линя'], true, 8),
    ('silurus_glanis', 'Сом', array['сом обыкновенный', 'сомы', 'сома', 'европейский сом'], true, 9),
    ('squalius_cephalus', 'Голавль', array['голавли', 'голавля', 'головень'], true, 10),
    ('sander_volgensis', 'Берш', array['берши', 'берша'], false, 100),
    ('gymnocephalus_cernua', 'Ёрш', array['ерш', 'ерш обыкновенный', 'ерши', 'ерша'], false, 101),
    ('scardinius_erythrophthalmus', 'Красноперка', array['краснопёрка', 'красноперки', 'краснопёрки'], false, 102),
    ('blicca_bjoerkna', 'Густера', array['густеры', 'густеру', 'плоскирка'], false, 103),
    ('carassius_carassius', 'Карась золотой', array['золотой карась', 'карась золотистый', 'золотые караси'], false, 104),
    ('cyprinus_carpio', 'Карп', array['карп обыкновенный', 'сазан', 'дикий карп', 'карпы'], false, 105),
    ('aspius_aspius', 'Жерех', array['жерех обыкновенный', 'жерехи', 'шереспер'], false, 106),
    ('chondrostoma_nasus', 'Подуст', array['подуст обыкновенный', 'подусты'], false, 107),
    ('leuciscus_leuciscus', 'Елец', array['ельцы', 'ельца', 'елец обыкновенный'], false, 108),
    ('alburnus_alburnus', 'Уклейка', array['уклея', 'уклейки', 'уклейку'], false, 109),
    ('alburnoides_bipunctatus', 'Быстрянка', array['быстрянки', 'быстрянка обыкновенная'], false, 110),
    ('leucaspius_delineatus', 'Верховка', array['овсянка', 'верховодка', 'верховки'], false, 111),
    ('rhodeus_amarus', 'Горчак', array['горчак обыкновенный', 'горчаки'], false, 112),
    ('gobio_gobio', 'Пескарь', array['пескарь обыкновенный', 'пескари', 'пескаря'], false, 113),
    ('cobitis_taenia', 'Щиповка', array['щиповка обыкновенная', 'щиповки', 'вьюн щиповка'], false, 114),
    ('misgurnus_fossilis', 'Вьюн', array['вьюн обыкновенный', 'вьюны', 'вьюна'], false, 115),
    ('barbatula_barbatula', 'Голец', array['голец усатый', 'усатый голец', 'гольцы'], false, 116),
    ('phoxinus_phoxinus', 'Гольян', array['гольян обыкновенный', 'гольяны'], false, 117),
    ('lota_lota', 'Налим', array['налим обыкновенный', 'налимы', 'налима'], false, 118),
    ('anguilla_anguilla', 'Угорь', array['угорь речной', 'европейский угорь', 'угри'], false, 119),
    ('acipenser_ruthenus', 'Стерлядь', array['стерляди', 'стерлядь'], false, 120),
    ('acipenser_gueldenstaedtii', 'Осетр русский', array['осетр', 'русский осетр', 'осетры'], false, 121),
    ('salmo_trutta', 'Форель ручьевая', array['форель', 'ручьевая форель', 'пеструшка'], false, 122),
    ('thymallus_thymallus', 'Хариус', array['хариус европейский', 'хариусы'], false, 123),
    ('hucho_hucho', 'Таймень', array['таймени', 'обыкновенный таймень'], false, 124),
    ('coregonus_albula', 'Ряпушка', array['ряпушка европейская', 'рипус', 'ряпушки'], false, 125),
    ('osmerus_eperlanus', 'Корюшка', array['корюшка европейская', 'корюшки', 'снеток', 'снетки'], false, 126),
    ('coregonus_lavaretus', 'Сиг', array['сиг обыкновенный', 'сиги'], false, 127),
    ('coregonus_peled', 'Пелядь', array['пелядь', 'сырок'], false, 128),
    ('hypophthalmichthys_molitrix', 'Толстолобик белый', array['белый толстолобик', 'толстолобики'], false, 129),
    ('hypophthalmichthys_nobilis', 'Толстолобик пестрый', array['пестрый толстолобик', 'пёстрый толстолобик'], false, 130),
    ('ctenopharyngodon_idella', 'Белый амур', array['амур', 'белый амур', 'амуры'], false, 131),
    ('pelecus_cultratus', 'Чехонь', array['чехони', 'сабля-рыба'], false, 132),
    ('vimba_vimba', 'Рыбец', array['сырть', 'рыбцы'], false, 133),
    ('neogobius_fluviatilis', 'Бычок-песочник', array['бычок песочник', 'бычки'], false, 134),
    ('neogobius_melanostomus', 'Бычок-кругляк', array['бычок кругляк', 'бычки'], false, 135),
    ('perccottus_glenii', 'Ротан', array['ротан-головешка', 'головешка', 'ротаны'], false, 136),
    ('lepomis_gibbosus', 'Солнечный окунь', array['ушастый окунь', 'солнечные окуни'], false, 137),
    ('lampetra_planeri', 'Минога ручьевая', array['ручьевая минога', 'миноги'], false, 138),
    ('eudontomyzon_mariae', 'Минога украинская', array['украинская минога', 'миноги'], false, 139),
    ('gasterosteus_aculeatus', 'Колюшка трехиглая', array['трехиглая колюшка', 'колюшки'], false, 140),
    ('pungitius_pungitius', 'Колюшка девятииглая', array['девятииглая колюшка', 'малая колюшка'], false, 141),
    ('clupeonella_cultriventris', 'Тюлька', array['тюльки', 'черноморско-каспийская тюлька'], false, 142),
    ('sabanejewia_baltica', 'Щиповка балтийская', array['балтийская щиповка', 'щиповки'], false, 143)
on conflict (id) do update set
    canonical_name = excluded.canonical_name,
    aliases = excluded.aliases,
    is_popular = excluded.is_popular,
    sort_order = excluded.sort_order,
    updated_at = now();

-- 2. species_id в улове ------------------------------------------------------

alter table public.fishing_fish
    add column if not exists species_id text;

alter table public.fishing_fish
    drop constraint if exists fishing_fish_species_id_fkey;

alter table public.fishing_fish
    add constraint fishing_fish_species_id_fkey
    foreign key (species_id) references public.fish_species(id) on delete set null;

create index if not exists fishing_fish_species_idx
    on public.fishing_fish (species_id);

-- Триггер проставляет вид по имени. publish_report не меняется: он по-прежнему
-- вставляет name/count, а species_id заполняется здесь. Если явный species_id
-- когда-нибудь придёт из клиента, он сохраняется как fallback.
create or replace function public.fishing_fish_resolve_species()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.species_id := coalesce(public.resolve_fish_species(new.name), new.species_id);
    return new;
end;
$$;

drop trigger if exists fishing_fish_resolve_species on public.fishing_fish;
create trigger fishing_fish_resolve_species
    before insert or update of name, species_id on public.fishing_fish
    for each row
    execute function public.fishing_fish_resolve_species();

-- 3. Backfill существующих строк ---------------------------------------------

update public.fishing_fish
   set species_id = public.resolve_fish_species(name)
 where species_id is null;

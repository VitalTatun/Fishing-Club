-- ACH-01 (часть 1/2): справочник видов рыб и правила нормализации/сопоставления.
-- Источник истины — Supabase; клиент кэширует каталог в Room и приносит сид в APK.

-- 1. Справочник видов ---------------------------------------------------------

create table if not exists public.fish_species (
    id             text primary key,
    canonical_name text not null,
    aliases        text[] not null default '{}'::text[],
    is_popular     boolean not null default false,
    sort_order     integer not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

comment on table public.fish_species is
    'ACH-01: постоянный каталог видов рыб. ID — латинский slug, не меняется при переименовании.';

alter table public.fish_species enable row level security;

-- Справочник только читается клиентами. Запись — service_role (без политик).
drop policy if exists fish_species_select on public.fish_species;
create policy fish_species_select
    on public.fish_species
    for select
    to anon, authenticated
    using (true);

grant select on public.fish_species to anon, authenticated;

create index if not exists fish_species_popular_idx
    on public.fish_species (is_popular, sort_order);

-- 2. Нормализация и сопоставление --------------------------------------------

-- Общие правила для клиента и сервера: trim, пробелы, регистр, ё → е.
create or replace function public.normalize_fish_name(value text)
returns text
language sql
immutable
set search_path = public
as $$
    select nullif(
        regexp_replace(
            translate(lower(trim(coalesce(value, ''))), 'ё', 'е'),
            '\s+', ' ', 'g'
        ),
        ''
    );
$$;

-- Точное совпадение с каноническим именем или алиасом.
-- При неоднозначности: канон, затем популярный, затем sort_order.
create or replace function public.resolve_fish_species(raw_name text)
returns text
language sql
stable
set search_path = public
as $$
    with normalized as (
        select public.normalize_fish_name(raw_name) as value
    )
    select fs.id
      from public.fish_species fs, normalized n
     where n.value is not null
       and (
           public.normalize_fish_name(fs.canonical_name) = n.value
           or exists (
               select 1
                 from unnest(fs.aliases) as a(alias)
                where public.normalize_fish_name(a.alias) = n.value
           )
       )
     order by (public.normalize_fish_name(fs.canonical_name) = n.value) desc,
              fs.is_popular desc,
              fs.sort_order,
              fs.id
     limit 1;
$$;

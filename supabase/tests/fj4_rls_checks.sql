-- =============================================================================
-- FJ-4: воспроизводимые проверки RLS (Supabase / PostgreSQL)
-- -----------------------------------------------------------------------------
-- Запускать в SQL Editor (роль postgres) или через psql.
-- Скрипт работает в одной транзакции и завершается ROLLBACK — данные не меняются.
--
-- Требование: в проекте должен быть хотя бы один приватный отчёт с фото,
-- владелец которого отличается от actor1. Значения actor/victim подбираются
-- автоматически.
--
-- Ожидаемые значения ПОСЛЕ применения миграций FJ-4:
--   A_profiles_visible            = 1   (только свой профиль)
--   A_emails_visible              = 1
--   B_private_report_visible      = 0
--   C_other_report_select         = 1   (публичный отчёт, by design)
--   C_update_others               = 0
--   C_delete_others               = 0
--   D_storage_direct_read         = 0
--   E_pivot_row_inserted          = 0   (после F-1; до исправления — 1)
--   F_anon_read_victim_object     = 0   (после F-1; до исправления — 1)
--   F_anon_read_private_report    = 0
--   G_update_own_likes_count      = 0   (после F-3; до исправления — >0)
--   G_insert_fake_likes_count     = 0   (после F-3)
-- =============================================================================

begin;

create temp table _fj4_ids on commit drop as
select
  (select p.id from public.profiles p order by p.created_at nulls last limit 1) as actor1,
  (select f.user_id
     from public.fishing f
    where not f.is_public
      and f.user_id <> (select p.id from public.profiles p order by p.created_at nulls last limit 1)
      and exists (select 1 from public.fishing_photos fp where fp.fishing_id = f.id)
    limit 1) as victim,
  (select o.name
     from storage.objects o
     join public.fishing_photos fp on fp.storage_path = o.name
     join public.fishing f on f.id = fp.fishing_id
    where o.bucket_id = 'fishing_photos'
      and not f.is_public
      and f.user_id <> (select p.id from public.profiles p order by p.created_at nulls last limit 1)
    limit 1) as victim_path,
  (select f.id
     from public.fishing f
    where f.is_public
      and f.user_id <> (select p.id from public.profiles p order by p.created_at nulls last limit 1)
    limit 1) as other_report,
  (select f.id
     from public.fishing f
    where not f.is_public
      and f.user_id <> (select p.id from public.profiles p order by p.created_at nulls last limit 1)
    limit 1) as private_report;

create temp table _fj4_results (check_name text, value int) on commit drop;

grant select on _fj4_ids to authenticated, anon;
grant insert, select on _fj4_results to authenticated, anon;

-- ---------------------------------------------------------------------------
-- Сценарии от имени авторизованного пользователя actor1
-- ---------------------------------------------------------------------------
set local role authenticated;
select set_config(
  'request.jwt.claims',
  json_build_object('sub', (select actor1 from _fj4_ids), 'role', 'authenticated')::text,
  true
);

insert into _fj4_results select 'A_profiles_visible', count(*)::int from public.profiles;
insert into _fj4_results select 'A_emails_visible', count(email)::int from public.profiles;

insert into _fj4_results
select 'B_private_report_visible', count(*)::int
from public.fishing
where id = (select private_report from _fj4_ids);

insert into _fj4_results
select 'C_other_report_select', count(*)::int
from public.fishing
where id = (select other_report from _fj4_ids);

with u as (
  update public.fishing set name = name
  where id = (select other_report from _fj4_ids)
  returning 1
)
insert into _fj4_results select 'C_update_others', count(*)::int from u;

with d as (
  delete from public.fishing
  where id = (select other_report from _fj4_ids)
  returning 1
)
insert into _fj4_results select 'C_delete_others', count(*)::int from d;

insert into _fj4_results
select 'D_storage_direct_read', count(*)::int
from storage.objects
where name = (select victim_path from _fj4_ids);

-- F-1: строка рыбалки и фото с чужим storage_path
insert into public.fishing (id, user_id, type, name, is_public)
select 'f1000000-0000-4000-8000-000000000001', actor1, 'FISHING_LOG', 'fj4-rls-check', true
from _fj4_ids;

insert into public.fishing_photos (id, fishing_id, storage_path)
select 'f1000000-0000-4000-8000-000000000002',
       'f1000000-0000-4000-8000-000000000001',
       victim_path
from _fj4_ids;

insert into _fj4_results
select 'E_pivot_row_inserted', count(*)::int
from public.fishing_photos
where id = 'f1000000-0000-4000-8000-000000000002';

-- F-3: подделка счётчика лайков
with u as (
  update public.fishing set likes_count = 99999
  where user_id = (select actor1 from _fj4_ids)
  returning 1
)
insert into _fj4_results select 'G_update_own_likes_count', count(*)::int from u;

insert into public.fishing (id, user_id, type, name, is_public, likes_count)
select 'f1000000-0000-4000-8000-000000000003', actor1, 'FISHING_LOG', 'fj4-like-check', false, 123456
from _fj4_ids;

insert into _fj4_results
select 'G_insert_fake_likes_count',
       coalesce((select likes_count from public.fishing
                 where id = 'f1000000-0000-4000-8000-000000000003'), -1);

-- ---------------------------------------------------------------------------
-- Сценарии от имени anon
-- ---------------------------------------------------------------------------
reset role;
set local role anon;
select set_config(
  'request.jwt.claims',
  json_build_object('role', 'anon')::text,
  true
);

insert into _fj4_results
select 'F_anon_read_victim_object', count(*)::int
from storage.objects
where name = (select victim_path from _fj4_ids);

insert into _fj4_results
select 'F_anon_read_private_report', count(*)::int
from public.fishing
where id = (select private_report from _fj4_ids);

reset role;

select check_name, value
from _fj4_results
order by check_name;

rollback;

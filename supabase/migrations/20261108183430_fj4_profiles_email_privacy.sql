-- =============================================================================
-- FJ-4 / F-2: e-mail пользователей не должен быть виден другим пользователям
-- -----------------------------------------------------------------------------
-- Подтверждено: profiles_select_for_authenticated открывал SELECT на всю
-- таблицу, включая email. Любой авторизованный клиент получал e-mail всех
-- пользователей через PostgREST (приложение запрашивает profiles без списка
-- колонок).
--
-- ВНИМАНИЕ: применять ТОЛЬКО после обновления клиента!
--   * чтение авторов заменить с таблицы public.profiles на представление
--     public.profiles_public (SupabaseFishingRepository.kt:259, 342, 719);
--   * запись профиля (upsert) оставить в public.profiles;
--   * чтение собственного профиля при необходимости — из public.profiles.
-- Иначе лента перестанет получать имена/аватары авторов.
-- =============================================================================

begin;

-- Представление выполняется от владельца (postgres) и намеренно обходит RLS
-- базовой таблицы, но отдаёт только безопасные колонки.
create or replace view public.profiles_public as
select
  id,
  name,
  avatar_url,
  created_at
from public.profiles;

comment on view public.profiles_public is
  'Публичная проекция профиля для чтения другими пользователями (без email). FJ-4.';

revoke all on public.profiles_public from anon, authenticated;
grant select on public.profiles_public to authenticated;

-- Оставляем только чтение собственного профиля (profiles_select_own).
drop policy if exists profiles_select_for_authenticated on public.profiles;

commit;

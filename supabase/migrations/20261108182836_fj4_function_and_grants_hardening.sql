-- =============================================================================
-- FJ-4 / F-4, F-5: ужесточение функций и грантов
-- -----------------------------------------------------------------------------
-- F-4: public.handle_new_user — SECURITY DEFINER с mutable search_path и
--      EXECUTE для anon/authenticated (Supabase linter 0011, 0028, 0029).
-- F-5: у anon были INSERT/UPDATE/DELETE/TRUNCATE на все таблицы public,
--      у authenticated — TRUNCATE. RLS блокирует DML, но TRUNCATE им не
--      покрывается, а сами гранты нарушают least privilege.
--
-- Клиентских изменений не требуется: anon не должен писать в таблицы,
-- приложение работает под authenticated.
-- =============================================================================

begin;

-- F-4
alter function public.handle_new_user() set search_path = '';

revoke execute on function public.handle_new_user() from public;
revoke execute on function public.handle_new_user() from anon, authenticated;

-- F-5
revoke insert, update, delete, truncate on all tables in schema public from anon;
revoke truncate on all tables in schema public from authenticated;

commit;

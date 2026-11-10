-- =============================================================================
-- AUD-03: удаление аккаунта из приложения
-- -----------------------------------------------------------------------------
-- Клиент не может удалять строки из auth.users напрямую: нужен service_role.
-- Функция ниже выполняется от владельца схемы (postgres) и удаляет только
-- аккаунт вызывающего пользователя (auth.uid()). RLS здесь не участвует,
-- поэтому проверка uid обязательна.
--
-- Порядок удаления:
--   1. отчёты пользователя (дочерние строки уходят по ON DELETE CASCADE);
--   2. публичный профиль;
--   3. строка auth.users — после неё сессия становится недействительной.
--
-- Объекты в Storage автоматически не удаляются: metadata и бинарники лежат
-- вне транзакции БД. Приватные бакеты недоступны без подписанных ссылок, но
-- осиротевшие файлы должен подчищать отдельный серверный job (см. AUD-04/09).
-- =============================================================================

begin;

create or replace function public.delete_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'Not authenticated' using errcode = '28000';
    end if;

    delete from public.fishing where user_id = uid;
    delete from public.profiles where id = uid;
    delete from auth.users where id = uid;
end;
$$;

comment on function public.delete_account() is
    'Deletes the calling user''s reports, profile and auth account.';

revoke all on function public.delete_account() from public;
revoke all on function public.delete_account() from anon;
grant execute on function public.delete_account() to authenticated;

commit;

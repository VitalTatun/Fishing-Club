-- =============================================================================
-- FJ-4 / F-1: фото отчёта может ссылаться только на объект владельца
-- -----------------------------------------------------------------------------
-- Подтверждённая проблема: политика fishing_photos_write_own проверяла только
-- владение родительским отчётом, но не storage_path. Пользователь мог добавить
-- в свой публичный отчёт строку с чужим путём, после чего
-- public_fishing_photos_read отдавала чужой приватный файл всем (включая anon).
--
-- Совместимость: существующие строки корректны (0 несоответствий на момент
-- аудита), клиентских изменений не требуется.
-- =============================================================================

begin;

-- 1. Запись в fishing_photos разрешена только владельцу отчёта и только
--    в собственный префикс хранилища: <auth.uid()>/...
drop policy if exists fishing_photos_write_own on public.fishing_photos;

create policy fishing_photos_write_own
on public.fishing_photos
for all
to authenticated
using (
  exists (
    select 1
    from public.fishing f
    where f.id = fishing_photos.fishing_id
      and f.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.fishing f
    where f.id = fishing_photos.fishing_id
      and f.user_id = auth.uid()
  )
  and split_part(storage_path, '/', 1) = auth.uid()::text
);

-- 2. Публичное чтение объекта разрешено, только если путь принадлежит
--    владельцу публичного отчёта. Защита в глубину: даже при появлении
--    некорректной строки в fishing_photos чужой файл не станет публичным.
drop policy if exists public_fishing_photos_read on storage.objects;

create policy public_fishing_photos_read
on storage.objects
for select
to anon, authenticated
using (
  bucket_id = 'fishing_photos'
  and exists (
    select 1
    from public.fishing_photos p
    join public.fishing f on f.id = p.fishing_id
    where p.storage_path = name
      and f.is_public
      and split_part(name, '/', 1) = f.user_id::text
  )
);

commit;

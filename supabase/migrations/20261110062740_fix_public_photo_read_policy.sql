-- =============================================================================
-- FJ-4 follow-up: публичные фото снова читаются другими пользователями
-- -----------------------------------------------------------------------------
-- Проблема: в политике public_fishing_photos_read (миграция 20261108182818)
-- неквалифицированный name разрешался в fishing.name, а не в storage.objects.name,
-- потому что таблица fishing входит в FROM подзапроса. В результате условие
--   p.storage_path = f.name
-- сравнивало путь объекта с названием отчёта («Свислочь», «Водохранилище», …)
-- и никогда не выполнялось. Проверка на живых данных: 0 совпадений вместо 25.
--
-- Следствие: authenticated/anon не получали SELECT на объекты чужих публичных
-- отчётов, Storage не выдавал signed URL, и в ленте пропадали фото других
-- авторов. Свои фото продолжали открываться через
-- fishing_photos_storage_read_own, поэтому проблема выглядела как «пропали
-- фото других пользователей».
--
-- Исправление: явно квалифицировать objects.name в подзапросе и сохранить
-- прежние ограничения (только публичные отчёты, путь принадлежит автору
-- отчёта). Приватные объекты других авторов остаются недоступны.
-- =============================================================================

begin;

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
    where p.storage_path = storage.objects.name
      and f.is_public
      and split_part(storage.objects.name, '/', 1) = f.user_id::text
  )
);

commit;

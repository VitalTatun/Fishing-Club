-- =============================================================================
-- FJ-4 / F-3: запрет прямой подделки fishing.likes_count
-- -----------------------------------------------------------------------------
-- Подтверждено: владелец отчёта мог прямым REST-запросом выставить любое
-- значение likes_count (UPDATE) или создать отчёт с любым значением (INSERT)
-- в обход RPC like_report/unlike_report.
--
-- Решение: отзываем табличные INSERT/UPDATE и выдаём колоночные гранты без
-- likes_count. RPC остаются рабочими — они SECURITY DEFINER и обновляют
-- счётчик от имени владельца функции.
--
-- Клиентских изменений не требуется: приложение пишет отчёты через
-- publish_report, а likes_count меняет только через RPC.
-- =============================================================================

begin;

revoke insert, update on public.fishing from anon;
revoke insert, update on public.fishing from authenticated;

-- id/user_id выданы на INSERT, чтобы publish_report мог создавать строки,
-- но на UPDATE их нет: владение записью изменить нельзя.
grant insert (
  id, user_id, type, name, water_name, water_lat, water_lng, water_paid,
  spot_lat, spot_lng, fishing_start_at, fishing_end_at, weight, fishing_method,
  comment, shore, is_public, created_at, updated_at, published_at
) on public.fishing to authenticated;

grant update (
  type, name, water_name, water_lat, water_lng, water_paid,
  spot_lat, spot_lng, fishing_start_at, fishing_end_at, weight, fishing_method,
  comment, shore, is_public, created_at, updated_at, published_at
) on public.fishing to authenticated;

commit;

-- FrameByNavin Creator OS v2.0 Alpha 1.2A.1
-- Authenticated restore catalog/download RPCs for returning-user recovery.

create or replace function public.creator_restore_points()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_result jsonb;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  select coalesce(
    jsonb_agg(
      jsonb_build_object(
        'id', b.id,
        'backup_kind', b.backup_kind,
        'captured_at', b.captured_at,
        'snapshot_day', b.snapshot_day,
        'app_version', b.app_version,
        'project_count', b.project_count,
        'idea_count', b.idea_count,
        'weekly_slot_count', b.weekly_slot_count,
        'active_reminder_count', b.active_reminder_count
      ) order by b.captured_at desc
    ),
    '[]'::jsonb
  )
  into v_result
  from (
    select id, backup_kind, captured_at, snapshot_day, app_version,
           project_count, idea_count, weekly_slot_count, active_reminder_count
    from public.creator_backups
    where user_id = v_user
    order by captured_at desc
    limit 24
  ) b;

  return v_result;
end;
$$;

revoke all on function public.creator_restore_points() from public, anon, authenticated;
grant execute on function public.creator_restore_points() to authenticated;

create or replace function public.creator_download_backup(p_backup_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_result jsonb;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  select jsonb_build_object(
    'payload', b.payload,
    'payload_sha256', b.payload_sha256
  )
  into v_result
  from public.creator_backups b
  where b.id = p_backup_id
    and b.user_id = v_user;

  if v_result is null then
    raise exception 'Backup not found' using errcode = 'P0002';
  end if;

  return v_result;
end;
$$;

revoke all on function public.creator_download_backup(uuid) from public, anon, authenticated;
grant execute on function public.creator_download_backup(uuid) to authenticated;

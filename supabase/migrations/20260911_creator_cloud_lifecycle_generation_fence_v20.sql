-- FrameByNavin Creator OS v2.0 Alpha 1.2A
-- Deployed to production on 2026-09-11.
-- Purpose: separate authenticated identity from backup lifecycle and fence stale cloud writes.

create table if not exists public.creator_cloud_lifecycle (
  user_id uuid primary key references auth.users(id) on delete cascade,
  phase text not null default 'active' check (phase in ('active','deleted')),
  generation bigint not null default 0 check (generation >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.creator_cloud_lifecycle enable row level security;

drop policy if exists creator_cloud_lifecycle_select_own on public.creator_cloud_lifecycle;
create policy creator_cloud_lifecycle_select_own
  on public.creator_cloud_lifecycle
  for select
  to authenticated
  using ((select auth.uid()) = user_id);

revoke all on table public.creator_cloud_lifecycle from anon, authenticated;
grant select on table public.creator_cloud_lifecycle to authenticated;

insert into public.creator_cloud_lifecycle (user_id, phase, generation)
select id, 'active', 0
from auth.users
on conflict (user_id) do nothing;

create or replace function public.creator_cloud_status()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_phase text;
  v_generation bigint;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  insert into public.creator_cloud_lifecycle (user_id, phase, generation)
  values (v_user, 'active', 0)
  on conflict (user_id) do nothing;

  select phase, generation
    into v_phase, v_generation
  from public.creator_cloud_lifecycle
  where user_id = v_user;

  return jsonb_build_object('phase', v_phase, 'generation', v_generation);
end;
$$;

create or replace function public.creator_enforce_write_epoch()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_headers text;
  v_epoch_text text;
  v_epoch bigint;
  v_phase text;
  v_generation bigint;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if new.user_id is distinct from v_user then
    raise exception 'Cloud write owner mismatch' using errcode = '42501';
  end if;

  v_headers := current_setting('request.headers', true);
  v_epoch_text := (coalesce(nullif(v_headers, ''), '{}')::jsonb ->> 'x-creator-write-epoch');
  if v_epoch_text is null or v_epoch_text !~ '^(0|[1-9][0-9]{0,17})$' then
    raise exception 'Missing or invalid cloud write generation' using errcode = '22023';
  end if;
  v_epoch := v_epoch_text::bigint;

  select phase, generation
    into v_phase, v_generation
  from public.creator_cloud_lifecycle
  where user_id = v_user
  for share;

  if not found then
    raise exception 'Cloud lifecycle is not initialized' using errcode = '55000';
  end if;
  if v_phase <> 'active' then
    raise exception 'Cloud history is not active' using errcode = '55000';
  end if;
  if v_generation <> v_epoch then
    raise exception 'Cloud lifecycle generation changed' using errcode = '40001';
  end if;

  return new;
end;
$$;

drop trigger if exists creator_profiles_write_epoch on public.creator_profiles;
create trigger creator_profiles_write_epoch
before insert or update on public.creator_profiles
for each row execute function public.creator_enforce_write_epoch();

drop trigger if exists creator_devices_write_epoch on public.creator_devices;
create trigger creator_devices_write_epoch
before insert or update on public.creator_devices
for each row execute function public.creator_enforce_write_epoch();

drop trigger if exists creator_backups_write_epoch on public.creator_backups;
create trigger creator_backups_write_epoch
before insert or update on public.creator_backups
for each row execute function public.creator_enforce_write_epoch();

create or replace function public.creator_delete_owned_data(p_expected_generation bigint)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_phase text;
  v_generation bigint;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;
  if p_expected_generation is null or p_expected_generation < 0 then
    raise exception 'Invalid expected generation' using errcode = '22023';
  end if;

  insert into public.creator_cloud_lifecycle (user_id, phase, generation)
  values (v_user, 'active', 0)
  on conflict (user_id) do nothing;

  select phase, generation
    into v_phase, v_generation
  from public.creator_cloud_lifecycle
  where user_id = v_user
  for update;

  if v_phase = 'deleted' then
    if v_generation = p_expected_generation + 1 then
      return jsonb_build_object('phase', v_phase, 'generation', v_generation);
    end if;
    raise exception 'Cloud lifecycle generation changed' using errcode = '40001';
  end if;

  if v_generation <> p_expected_generation then
    raise exception 'Cloud lifecycle generation changed' using errcode = '40001';
  end if;

  delete from public.creator_backups where user_id = v_user;
  delete from public.creator_devices where user_id = v_user;
  delete from public.creator_profiles where user_id = v_user;

  update public.creator_cloud_lifecycle
     set phase = 'deleted', generation = v_generation + 1, updated_at = now()
   where user_id = v_user
   returning phase, generation into v_phase, v_generation;

  return jsonb_build_object('phase', v_phase, 'generation', v_generation);
end;
$$;

create or replace function public.creator_resume_owned_data(p_expected_generation bigint)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_phase text;
  v_generation bigint;
begin
  if v_user is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;
  if p_expected_generation is null or p_expected_generation < 0 then
    raise exception 'Invalid expected generation' using errcode = '22023';
  end if;

  select phase, generation
    into v_phase, v_generation
  from public.creator_cloud_lifecycle
  where user_id = v_user
  for update;

  if not found then
    raise exception 'Cloud lifecycle is not initialized' using errcode = '55000';
  end if;
  if v_phase <> 'deleted' or v_generation <> p_expected_generation then
    raise exception 'Cloud lifecycle generation changed' using errcode = '40001';
  end if;

  if exists (select 1 from public.creator_backups where user_id = v_user)
     or exists (select 1 from public.creator_devices where user_id = v_user)
     or exists (select 1 from public.creator_profiles where user_id = v_user) then
    raise exception 'Cloud creator data still exists' using errcode = '55000';
  end if;

  update public.creator_cloud_lifecycle
     set phase = 'active', generation = v_generation + 1, updated_at = now()
   where user_id = v_user
   returning phase, generation into v_phase, v_generation;

  return jsonb_build_object('phase', v_phase, 'generation', v_generation);
end;
$$;

revoke all on function public.creator_cloud_status() from public;
revoke all on function public.creator_delete_owned_data(bigint) from public;
revoke all on function public.creator_resume_owned_data(bigint) from public;
grant execute on function public.creator_cloud_status() to authenticated;
grant execute on function public.creator_delete_owned_data(bigint) to authenticated;
grant execute on function public.creator_resume_owned_data(bigint) to authenticated;

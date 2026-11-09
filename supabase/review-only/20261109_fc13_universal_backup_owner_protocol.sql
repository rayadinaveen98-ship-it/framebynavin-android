-- Backlot FC13 source-only review: NOT applied to the live Supabase project.
-- Migrate only after one final staging/Android/two-device acceptance run.
-- No old Creator or Personal sync table is modified by this draft.
--
-- Threat model: verified Supabase auth.uid() owns all metadata and immutable
-- ciphertext objects. Normal Android clients never receive a service key.
-- Public RPC functions are SECURITY DEFINER solely for narrow, transactional
-- CAS and deletion fencing; PUBLIC/anon EXECUTE is explicitly revoked.

create table public.trust_backup_owner_state_v1 (
  user_id uuid primary key references auth.users(id) on delete cascade,
  phase text not null default 'active'
    check (phase in ('active','deleting_backup')),
  updated_at timestamptz not null default now()
);
create table public.trust_backup_heads_v1 (
  user_id uuid primary key references auth.users(id) on delete cascade,
  revision bigint not null check (revision > 0),
  object_path text not null,
  ciphertext_sha256 text not null check (ciphertext_sha256 ~ '^[a-f0-9]{64}$'),
  ciphertext_bytes bigint not null check (ciphertext_bytes between 1 and 55000000),
  updated_at timestamptz not null default now()
);
create table public.trust_backup_history_v1 (
  user_id uuid not null references auth.users(id) on delete cascade,
  revision bigint not null check (revision > 0),
  object_path text not null unique,
  ciphertext_sha256 text not null check (ciphertext_sha256 ~ '^[a-f0-9]{64}$'),
  ciphertext_bytes bigint not null check (ciphertext_bytes between 1 and 55000000),
  created_at timestamptz not null default now(),
  primary key (user_id, revision)
);
alter table public.trust_backup_owner_state_v1 enable row level security;
alter table public.trust_backup_heads_v1 enable row level security;
alter table public.trust_backup_history_v1 enable row level security;
revoke all on public.trust_backup_owner_state_v1,
  public.trust_backup_heads_v1, public.trust_backup_history_v1 from public, anon, authenticated;
grant select on public.trust_backup_owner_state_v1,
  public.trust_backup_heads_v1, public.trust_backup_history_v1 to authenticated;

create policy trust_owner_state_read_v1 on public.trust_backup_owner_state_v1
  for select to authenticated using (user_id = (select auth.uid()));
create policy trust_head_read_v1 on public.trust_backup_heads_v1
  for select to authenticated using (user_id = (select auth.uid()));
create policy trust_history_read_v1 on public.trust_backup_history_v1
  for select to authenticated using (user_id = (select auth.uid()));

-- Explicit private bucket, with a conservative zero-spend 50 MB object cap.
-- If existing assets exceed the cap, the Android UI must refuse a "complete"
-- claim. Larger archives require a separately reviewed chunked transport.
insert into storage.buckets (id, name, public, file_size_limit)
values ('backlot-trust-universal-v1', 'backlot-trust-universal-v1', false, 52428800)
on conflict (id) do nothing;

-- Authenticated owners can read/list only their own private ciphertext.
create policy trust_ciphertext_owner_read_v1 on storage.objects
  for select to authenticated using (
    bucket_id = 'backlot-trust-universal-v1' and
    (storage.foldername(name))[1] = (select auth.uid())::text
  );
-- Immutable create-only: no authenticated UPDATE policy is installed.
create policy trust_ciphertext_owner_create_v1 on storage.objects
  for insert to authenticated with check (
    bucket_id = 'backlot-trust-universal-v1' and
    name ~ ('^' || (select auth.uid())::text ||
      '/revisions/v1-[0-9a-f-]{36}\\.enc$') and
    exists (
      select 1 from public.trust_backup_owner_state_v1 o
      where o.user_id = (select auth.uid()) and o.phase = 'active'
    )
  );
-- Deletion of objects is permitted only within an EXPLICIT owner-confirmed
-- server-side delete-cloud-backup transaction state.
create policy trust_ciphertext_owner_delete_v1 on storage.objects
  for delete to authenticated using (
    bucket_id = 'backlot-trust-universal-v1' and
    (storage.foldername(name))[1] = (select auth.uid())::text and
    exists (
      select 1 from public.trust_backup_owner_state_v1 o
      where o.user_id = (select auth.uid()) and o.phase = 'deleting_backup'
    )
  );

create or replace function public.trust_begin_upload_v1()
returns jsonb language plpgsql security definer set search_path = ''
as $f$
declare
  owner_id uuid := (select auth.uid());
  phase_now text;
begin
  if owner_id is null then
    raise exception 'Authenticated owner required' using errcode='28000';
  end if;
  insert into public.trust_backup_owner_state_v1 (user_id)
    values (owner_id) on conflict (user_id) do nothing;
  select phase into phase_now from public.trust_backup_owner_state_v1
    where user_id = owner_id for update;
  if phase_now <> 'active' then
    raise exception 'Backup deletion is in progress; resolve it first'
      using errcode='42501';
  end if;
  return jsonb_build_object('status','ready');
end $f$;

create or replace function public.trust_claim_backup_head_v1(
  p_expected_revision bigint, p_expected_path text, p_expected_sha text,
  p_new_path text, p_new_sha text, p_new_bytes bigint
) returns jsonb language plpgsql security definer set search_path = ''
as $f$
declare
  owner_id uuid := (select auth.uid());
  phase_now text;
  prior public.trust_backup_heads_v1%rowtype;
  next_revision bigint;
begin
  if owner_id is null then
    raise exception 'Authenticated owner required' using errcode='28000';
  end if;
  if p_new_path !~ ('^' || owner_id::text ||
      '/revisions/v1-[0-9a-f-]{36}\\.enc$') or
      p_new_sha !~ '^[a-f0-9]{64}$' or
      p_new_bytes not between 1 and 52428800 then
    raise exception 'Invalid encrypted owner backup claim' using errcode='22023';
  end if;
  -- Serialize competing devices and owner-triggered delete-cloud-backup.
  select phase into phase_now from public.trust_backup_owner_state_v1
    where user_id=owner_id for update;
  if phase_now is distinct from 'active' then
    raise exception 'Backup owner is not in active state' using errcode='42501';
  end if;
  select * into prior from public.trust_backup_heads_v1
    where user_id=owner_id for update;
  if (prior.user_id is null and
      (p_expected_revision is not null or p_expected_path is not null or
       p_expected_sha is not null)) or
     (prior.user_id is not null and
      (p_expected_revision is distinct from prior.revision or
       p_expected_path is distinct from prior.object_path or
       p_expected_sha is distinct from prior.ciphertext_sha256)) then
    return jsonb_build_object('status','conflict');
  end if;
  if not exists (
    select 1 from storage.objects
    where bucket_id='backlot-trust-universal-v1' and name=p_new_path
  ) then
    raise exception 'Authenticated encrypted object is absent'
      using errcode='22023';
  end if;
  next_revision := coalesce(prior.revision,0) + 1;
  insert into public.trust_backup_history_v1 (
    user_id, revision, object_path, ciphertext_sha256, ciphertext_bytes
  ) values (owner_id, next_revision, p_new_path, p_new_sha, p_new_bytes);
  insert into public.trust_backup_heads_v1 (
    user_id, revision, object_path, ciphertext_sha256, ciphertext_bytes
  ) values (owner_id, next_revision, p_new_path, p_new_sha, p_new_bytes)
  on conflict (user_id) do update set
    revision=excluded.revision, object_path=excluded.object_path,
    ciphertext_sha256=excluded.ciphertext_sha256,
    ciphertext_bytes=excluded.ciphertext_bytes, updated_at=now();
  return jsonb_build_object('status','accepted','revision',next_revision);
end $f$;

create or replace function public.trust_begin_delete_backup_v1(
  p_expected_revision bigint, p_expected_path text, p_expected_sha text,
  p_confirmation text
) returns jsonb language plpgsql security definer set search_path = ''
as $f$
declare
  owner_id uuid := (select auth.uid());
  current_head public.trust_backup_heads_v1%rowtype;
  current_phase text;
begin
  if owner_id is null then
    raise exception 'Authenticated owner required' using errcode='28000';
  end if;
  if p_confirmation <> 'DELETE CLOUD BACKUP' then
    raise exception 'Owner must explicitly confirm cloud backup deletion'
      using errcode='22023';
  end if;
  insert into public.trust_backup_owner_state_v1 (user_id)
    values (owner_id) on conflict (user_id) do nothing;
  select phase into current_phase from public.trust_backup_owner_state_v1
    where user_id=owner_id for update;
  select * into current_head from public.trust_backup_heads_v1
    where user_id=owner_id for update;
  if (current_head.user_id is null and
     (p_expected_revision is not null or p_expected_path is not null or
      p_expected_sha is not null)) or
     (current_head.user_id is not null and
     (p_expected_revision is distinct from current_head.revision or
      p_expected_path is distinct from current_head.object_path or
      p_expected_sha is distinct from current_head.ciphertext_sha256)) then
    return jsonb_build_object('status','conflict');
  end if;
  if current_phase not in ('active','deleting_backup') then
    raise exception 'Unexpected owner state' using errcode='42501';
  end if;
  update public.trust_backup_owner_state_v1
    set phase='deleting_backup',updated_at=now()
    where user_id=owner_id;
  return jsonb_build_object('status','deletion_fenced');
end $f$;

create or replace function public.trust_finish_delete_backup_v1()
returns jsonb language plpgsql security definer set search_path = ''
as $f$
declare
  owner_id uuid := (select auth.uid());
  phase_now text;
begin
  if owner_id is null then
    raise exception 'Authenticated owner required' using errcode='28000';
  end if;
  select phase into phase_now from public.trust_backup_owner_state_v1
    where user_id=owner_id for update;
  if phase_now is distinct from 'deleting_backup' then
    raise exception 'No verified cloud backup deletion in progress'
      using errcode='42501';
  end if;
  if exists (
    select 1 from storage.objects
    where bucket_id='backlot-trust-universal-v1'
      and name like (owner_id::text || '/%')
  ) then
    return jsonb_build_object('status','objects_remaining');
  end if;
  delete from public.trust_backup_heads_v1 where user_id=owner_id;
  delete from public.trust_backup_history_v1 where user_id=owner_id;
  update public.trust_backup_owner_state_v1
    set phase='active',updated_at=now() where user_id=owner_id;
  return jsonb_build_object('status','deleted');
end $f$;

-- No direct UPDATE privilege to bypass CAS or delete fences.
revoke all on function public.trust_begin_upload_v1() from public, anon;
revoke all on function public.trust_claim_backup_head_v1(
  bigint,text,text,text,text,bigint) from public, anon;
revoke all on function public.trust_begin_delete_backup_v1(
  bigint,text,text,text) from public, anon;
revoke all on function public.trust_finish_delete_backup_v1() from public, anon;
grant execute on function public.trust_begin_upload_v1() to authenticated;
grant execute on function public.trust_claim_backup_head_v1(
  bigint,text,text,text,text,bigint) to authenticated;
grant execute on function public.trust_begin_delete_backup_v1(
  bigint,text,text,text) to authenticated;
grant execute on function public.trust_finish_delete_backup_v1() to authenticated;

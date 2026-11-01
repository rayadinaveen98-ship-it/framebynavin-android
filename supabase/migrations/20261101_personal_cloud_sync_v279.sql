-- Backlot unified Personal sync, isolated from existing Creator tables.
-- No existing Creator or Personal data is modified by this migration.
create table if not exists public.personal_sync_heads (
  user_id uuid primary key references auth.users(id) on delete cascade,
  revision bigint not null default 1 check (revision > 0),
  payload text not null check (octet_length(payload) between 1 and 8000000),
  payload_sha256 text not null check (payload_sha256 ~ '^[a-f0-9]{64}$'),
  updated_at timestamptz not null default now()
);
alter table public.personal_sync_heads enable row level security;
revoke all on table public.personal_sync_heads from public, anon;
grant select, insert, update on table public.personal_sync_heads to authenticated;

create policy "personal_sync_read_own" on public.personal_sync_heads
 for select to authenticated using (user_id = (select auth.uid()));
create policy "personal_sync_insert_own" on public.personal_sync_heads
 for insert to authenticated with check (user_id = (select auth.uid()));
create policy "personal_sync_update_own" on public.personal_sync_heads
 for update to authenticated using (user_id = (select auth.uid()))
 with check (user_id = (select auth.uid()));

-- Compare-and-swap: never silently overwrite a different device's revision.
create or replace function public.push_personal_sync_snapshot(
 p_expected_revision bigint,
 p_payload text,
 p_payload_sha256 text
) returns jsonb
language plpgsql security invoker set search_path = ''
as $$
declare
 v_user uuid := (select auth.uid());
 v_revision bigint;
begin
 if v_user is null then
   raise exception 'Authenticated account required' using errcode = '28000';
 end if;
 if p_payload is null or octet_length(p_payload) not between 1 and 8000000 or
    p_payload_sha256 is null or p_payload_sha256 !~ '^[a-f0-9]{64}$' then
   raise exception 'Invalid Personal snapshot' using errcode = '22023';
 end if;
 if p_expected_revision is null then
   insert into public.personal_sync_heads(user_id,revision,payload,payload_sha256)
   values(v_user,1,p_payload,p_payload_sha256)
   on conflict (user_id) do nothing
   returning revision into v_revision;
 else
   update public.personal_sync_heads
   set revision = revision + 1,
       payload = p_payload, payload_sha256 = p_payload_sha256,
       updated_at = now()
   where user_id = v_user and revision = p_expected_revision
   returning revision into v_revision;
 end if;
 if v_revision is null then
   return jsonb_build_object('status','conflict');
 end if;
 return jsonb_build_object('status','accepted','revision',v_revision);
end;
$$;
revoke all on function public.push_personal_sync_snapshot(bigint,text,text) from public, anon;
grant execute on function public.push_personal_sync_snapshot(bigint,text,text) to authenticated;

-- Backlot Lab V337: fresh development-only install, no data migration.
-- Source: V337 stable + original Alpha23 Creator ID SQL + V123/V124 Creator sync + V279 Personal sync.
-- Only for fresh, empty project vasleafrmsovdbdlrcsw.
-- Never apply to the original Backlot backend or an existing user-data database.
-- Google OAuth configuration and private Storage bucket/policies are separate steps.
-- Restrict callable public automatic-RLS event helper.
REVOKE ALL ON FUNCTION public.rls_auto_enable() FROM PUBLIC, anon, authenticated;
CREATE TABLE public.creator_profiles (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name text,
  avatar_url text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.creator_profiles ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.creator_profiles FROM PUBLIC, anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.creator_profiles TO authenticated;
CREATE POLICY creator_profiles_select_own ON public.creator_profiles
 FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()));
CREATE POLICY creator_profiles_insert_own ON public.creator_profiles
 FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY creator_profiles_update_own ON public.creator_profiles
 FOR UPDATE TO authenticated USING (user_id = (SELECT auth.uid()))
 WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY creator_profiles_delete_own ON public.creator_profiles
 FOR DELETE TO authenticated USING (user_id = (SELECT auth.uid()));
CREATE FUNCTION public.touch_updated_at() RETURNS trigger
LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END
$fn$;
REVOKE ALL ON FUNCTION public.touch_updated_at() FROM PUBLIC, anon, authenticated;
CREATE TRIGGER creator_profiles_touch_updated_at
 BEFORE UPDATE ON public.creator_profiles
 FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

alter table public.creator_profiles
  add column if not exists username text,
  add column if not exists username_normalized text;

create unique index if not exists creator_profiles_username_normalized_uq
  on public.creator_profiles (username_normalized)
  where username_normalized is not null;

create or replace function public.claim_creator_username(p_username text, p_display_name text default null)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_user_id uuid := auth.uid();
  v_username text := lower(trim(coalesce(p_username, '')));
  v_display_name text := nullif(trim(coalesce(p_display_name, '')), '');
  v_row public.creator_profiles%rowtype;
begin
  if v_user_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;
  if length(v_username) < 3 or length(v_username) > 24 or v_username !~ '^[a-z0-9_]+$' then
    raise exception 'Username must be 3-24 characters using letters, numbers or underscore' using errcode = '22023';
  end if;
  if v_username in ('admin','administrator','framebynavin','support','help','official','system','moderator','null','undefined') then
    raise exception 'That username is reserved' using errcode = '22023';
  end if;
  begin
    insert into public.creator_profiles (user_id, display_name, username, username_normalized, updated_at)
    values (v_user_id, v_display_name, v_username, v_username, now())
    on conflict (user_id) do update
      set display_name = coalesce(v_display_name, public.creator_profiles.display_name),
          username = v_username,
          username_normalized = v_username,
          updated_at = now()
    returning * into v_row;
  exception when unique_violation then
    raise exception 'Username is already taken' using errcode = '23505';
  end;
  return jsonb_build_object(
    'user_id', v_row.user_id,
    'display_name', coalesce(v_row.display_name, ''),
    'username', coalesce(v_row.username, ''),
    'avatar_url', coalesce(v_row.avatar_url, ''),
    'created_at', v_row.created_at,
    'updated_at', v_row.updated_at
  );
end;
$$;

revoke all on function public.claim_creator_username(text, text) from public;
revoke all on function public.claim_creator_username(text, text) from anon;
grant execute on function public.claim_creator_username(text, text) to authenticated;


-- Table constraints prevent bypassing claim_creator_username via direct REST writes.
ALTER TABLE public.creator_profiles ADD CONSTRAINT creator_profiles_username_integrity CHECK (
  (username IS NULL AND username_normalized IS NULL)
  OR (
    username IS NOT NULL AND username_normalized IS NOT NULL
    AND username ~ '^[a-z0-9_]{3,24}$' AND username_normalized = username
    AND username NOT IN ('admin','administrator','framebynavin','support','help','official','system','moderator','null','undefined')
  )
);

-- Canonical Creator snapshot sync v123

create table public.creator_sync_snapshots (
    user_id uuid not null references auth.users(id) on delete cascade,
    revision bigint not null check (revision > 0),
    base_content_sha256 text,
    content_sha256 text not null check (content_sha256 ~ '^[0-9a-f]{64}$'),
    payload_sha256 text not null check (payload_sha256 ~ '^[0-9a-f]{64}$'),
    schema_version integer not null check (schema_version > 0 and schema_version < 1000),
    payload text not null check (octet_length(payload) <= 5242880),
    captured_at timestamptz not null,
    app_version text not null check (char_length(app_version) between 1 and 96),
    project_count integer not null default 0 check (project_count >= 0),
    idea_count integer not null default 0 check (idea_count >= 0),
    device_id text not null check (char_length(device_id) between 1 and 128),
    created_at timestamptz not null default now(),
    primary key (user_id, revision)
);

create table public.creator_sync_heads (
    user_id uuid primary key references auth.users(id) on delete cascade,
    revision bigint not null check (revision > 0),
    content_sha256 text not null check (content_sha256 ~ '^[0-9a-f]{64}$'),
    updated_at timestamptz not null default now(),
    constraint creator_sync_heads_snapshot_fk
        foreign key (user_id, revision)
        references public.creator_sync_snapshots(user_id, revision)
        on delete restrict
);

create index creator_sync_snapshots_user_created_idx
    on public.creator_sync_snapshots (user_id, created_at desc);

alter table public.creator_sync_snapshots enable row level security;
alter table public.creator_sync_heads enable row level security;

create policy creator_sync_snapshots_select_own
    on public.creator_sync_snapshots for select
    to authenticated
    using ((select auth.uid()) = user_id);

create policy creator_sync_snapshots_insert_own
    on public.creator_sync_snapshots for insert
    to authenticated
    with check ((select auth.uid()) = user_id);

create policy creator_sync_snapshots_delete_own
    on public.creator_sync_snapshots for delete
    to authenticated
    using ((select auth.uid()) = user_id);

create policy creator_sync_heads_select_own
    on public.creator_sync_heads for select
    to authenticated
    using ((select auth.uid()) = user_id);

create policy creator_sync_heads_insert_own
    on public.creator_sync_heads for insert
    to authenticated
    with check ((select auth.uid()) = user_id);

create policy creator_sync_heads_update_own
    on public.creator_sync_heads for update
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

create policy creator_sync_heads_delete_own
    on public.creator_sync_heads for delete
    to authenticated
    using ((select auth.uid()) = user_id);

revoke all on public.creator_sync_snapshots from anon;
revoke all on public.creator_sync_heads from anon;
grant select, insert, delete on public.creator_sync_snapshots to authenticated;
grant select, insert, update, delete on public.creator_sync_heads to authenticated;

create or replace function public.get_creator_sync_head()
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '42501';
    end if;

    select jsonb_build_object(
        'revision', s.revision,
        'content_sha256', s.content_sha256,
        'payload_sha256', s.payload_sha256,
        'schema_version', s.schema_version,
        'payload', s.payload,
        'captured_at', s.captured_at,
        'app_version', s.app_version,
        'project_count', s.project_count,
        'idea_count', s.idea_count,
        'device_id', s.device_id,
        'updated_at', h.updated_at
    )
    into v_result
    from public.creator_sync_heads h
    join public.creator_sync_snapshots s
      on s.user_id = h.user_id and s.revision = h.revision
    where h.user_id = v_uid;

    return v_result;
end;
$$;

create or replace function public.push_creator_sync_snapshot(
    p_expected_content_sha256 text,
    p_content_sha256 text,
    p_payload_sha256 text,
    p_schema_version integer,
    p_payload text,
    p_captured_at timestamptz,
    p_app_version text,
    p_project_count integer,
    p_idea_count integer,
    p_device_id text
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
    v_current_revision bigint;
    v_current_sha text;
    v_new_revision bigint;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '42501';
    end if;
    if p_content_sha256 is null or p_content_sha256 !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid content hash' using errcode = '22023';
    end if;
    if p_payload_sha256 is null or p_payload_sha256 !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid payload hash' using errcode = '22023';
    end if;
    if p_expected_content_sha256 is not null and p_expected_content_sha256 !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid expected hash' using errcode = '22023';
    end if;
    if p_schema_version <= 0 or p_schema_version >= 1000 then
        raise exception 'invalid schema version' using errcode = '22023';
    end if;
    if p_payload is null or octet_length(p_payload) > 5242880 then
        raise exception 'payload too large' using errcode = '22023';
    end if;
    if p_app_version is null or char_length(p_app_version) not between 1 and 96 then
        raise exception 'invalid app version' using errcode = '22023';
    end if;
    if p_project_count < 0 or p_idea_count < 0 then
        raise exception 'invalid counts' using errcode = '22023';
    end if;
    if p_device_id is null or char_length(p_device_id) not between 1 and 128 then
        raise exception 'invalid device id' using errcode = '22023';
    end if;

    select revision, content_sha256
      into v_current_revision, v_current_sha
      from public.creator_sync_heads
     where user_id = v_uid
     for update;

    if found then
        if v_current_sha = p_content_sha256 then
            return jsonb_build_object(
                'status', 'unchanged',
                'revision', v_current_revision,
                'content_sha256', v_current_sha
            );
        end if;

        if p_expected_content_sha256 is null or p_expected_content_sha256 <> v_current_sha then
            return jsonb_build_object(
                'status', 'conflict',
                'revision', v_current_revision,
                'content_sha256', v_current_sha
            );
        end if;

        v_new_revision := v_current_revision + 1;
    else
        if p_expected_content_sha256 is not null then
            return jsonb_build_object(
                'status', 'conflict',
                'revision', 0,
                'content_sha256', null
            );
        end if;
        v_new_revision := 1;
    end if;

    insert into public.creator_sync_snapshots (
        user_id, revision, base_content_sha256, content_sha256, payload_sha256,
        schema_version, payload, captured_at, app_version, project_count,
        idea_count, device_id
    ) values (
        v_uid, v_new_revision, v_current_sha, p_content_sha256, p_payload_sha256,
        p_schema_version, p_payload, p_captured_at, p_app_version, p_project_count,
        p_idea_count, p_device_id
    );

    insert into public.creator_sync_heads (user_id, revision, content_sha256, updated_at)
    values (v_uid, v_new_revision, p_content_sha256, now())
    on conflict (user_id) do update
       set revision = excluded.revision,
           content_sha256 = excluded.content_sha256,
           updated_at = excluded.updated_at;

    return jsonb_build_object(
        'status', 'accepted',
        'revision', v_new_revision,
        'content_sha256', p_content_sha256
    );
end;
$$;

create or replace function public.delete_creator_sync_data()
returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '42501';
    end if;
    delete from public.creator_sync_heads where user_id = v_uid;
    delete from public.creator_sync_snapshots where user_id = v_uid;
end;
$$;

revoke all on function public.get_creator_sync_head() from public, anon;
revoke all on function public.push_creator_sync_snapshot(text, text, text, integer, text, timestamptz, text, integer, integer, text) from public, anon;
revoke all on function public.delete_creator_sync_data() from public, anon;
grant execute on function public.get_creator_sync_head() to authenticated;
grant execute on function public.push_creator_sync_snapshot(text, text, text, integer, text, timestamptz, text, integer, integer, text) to authenticated;
grant execute on function public.delete_creator_sync_data() to authenticated;

-- Canonical Creator sync FK index v124

-- Stage H release-readiness hardening.
-- Supabase Performance Advisor 0001 flagged the composite foreign key from
-- creator_sync_heads(user_id, revision) to creator_sync_snapshots(user_id, revision)
-- as lacking a covering child-side index. The table is new/empty at migration time,
-- and this changes no row ownership, RLS policy, RPC, or sync semantics.

create index if not exists creator_sync_heads_snapshot_idx
    on public.creator_sync_heads (user_id, revision);

-- Canonical Personal sync v279

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

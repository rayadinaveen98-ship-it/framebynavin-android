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

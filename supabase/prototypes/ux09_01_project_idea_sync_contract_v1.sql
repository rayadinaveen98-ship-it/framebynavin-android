-- Backlot UX09-01: DISPOSABLE-ONLY Postgres/Supabase SQL prototype.
-- NOT a committed migration in supabase/migrations; NOT deployed to Backlot Lab.
-- Requires an isolated, disposable Supabase LOCAL stack with auth.users/auth.uid.
-- Important: do not expose backlot_sync_internal through Supabase Data API.
-- No Creator/Personal snapshot tables, live records, storage, Auth sessions,
-- backups, restoration or account deletion are modified.
-- ON DELETE CASCADE on the internal FK chain only supports FUTURE verified
-- Auth account removal; no client DELETE privilege or account deletion RPC.
-- Tombstones are retained for offline device convergence until explicit
-- retention and full-account-deletion policy is implemented.
--
-- Server contract v1 deliberately supports Creator Projects and Ideas ONLY.
-- This must not be used to advertise complete sync, backup or recovery.

begin;

create schema if not exists backlot_sync_internal;
revoke all on schema backlot_sync_internal from public, anon;
grant usage on schema backlot_sync_internal to authenticated;

create table backlot_sync_internal.account_sequences (
    user_id uuid primary key references auth.users(id) on delete cascade,
    next_sequence bigint not null default 1 check (next_sequence > 0)
);
create table backlot_sync_internal.entities (
    user_id uuid not null references auth.users(id) on delete cascade,
    domain text not null check (domain in ('creator_project', 'creator_idea')),
    entity_id uuid not null,
    revision bigint not null check (revision > 0),
    payload jsonb not null check (
        jsonb_typeof(payload) = 'object'
        and octet_length(payload::text) <= 16384
    ),
    deleted_at timestamptz,
    updated_at timestamptz not null default now(),
    last_operation_id uuid not null,
    last_sequence bigint not null check (last_sequence > 0),
    primary key (user_id, domain, entity_id),
    check (deleted_at is null or payload = '{}'::jsonb)
);
create index entities_by_account_sequence
    on backlot_sync_internal.entities (user_id, last_sequence);

create table backlot_sync_internal.changes (
    user_id uuid not null references auth.users(id) on delete cascade,
    sequence bigint not null check (sequence > 0),
    domain text not null check (domain in ('creator_project', 'creator_idea')),
    entity_id uuid not null,
    revision bigint not null check (revision > 0),
    payload jsonb not null check (
        jsonb_typeof(payload) = 'object'
        and octet_length(payload::text) <= 16384
    ),
    deleted_at timestamptz,
    operation_id uuid not null,
    created_at timestamptz not null default now(),
    primary key (user_id, sequence),
    foreign key (user_id, domain, entity_id)
      references backlot_sync_internal.entities(user_id, domain, entity_id)
      on delete cascade,
    check (deleted_at is null or payload = '{}'::jsonb)
);
create index changes_by_account_entity
    on backlot_sync_internal.changes (user_id, domain, entity_id, sequence);

create table backlot_sync_internal.applied_operations (
    user_id uuid not null references auth.users(id) on delete cascade,
    operation_id uuid not null,
    request jsonb not null check (
        jsonb_typeof(request) = 'object'
        and octet_length(request::text) <= 20000
    ),
    result jsonb not null,
    sequence bigint not null,
    created_at timestamptz not null default now(),
    primary key (user_id, operation_id),
    foreign key (user_id, sequence)
      references backlot_sync_internal.changes(user_id, sequence)
      on delete cascade
);
create index applied_operations_by_account_sequence
    on backlot_sync_internal.applied_operations(user_id, sequence);

-- Defense in depth: each internal table denies all anonymous reads/writes.
-- Authenticated RPCs run SECURITY INVOKER, so permissions and RLS apply.
alter table backlot_sync_internal.account_sequences enable row level security;
alter table backlot_sync_internal.entities enable row level security;
alter table backlot_sync_internal.changes enable row level security;
alter table backlot_sync_internal.applied_operations enable row level security;

revoke all on all tables in schema backlot_sync_internal from public, anon, authenticated;
grant select, insert, update on backlot_sync_internal.account_sequences to authenticated;
grant select, insert, update on backlot_sync_internal.entities to authenticated;
grant select, insert on backlot_sync_internal.changes to authenticated;
grant select, insert on backlot_sync_internal.applied_operations to authenticated;

create policy account_sequences_own_select
  on backlot_sync_internal.account_sequences for select to authenticated
  using (user_id = (select auth.uid()));
create policy account_sequences_own_insert
  on backlot_sync_internal.account_sequences for insert to authenticated
  with check (user_id = (select auth.uid()));
create policy account_sequences_own_update
  on backlot_sync_internal.account_sequences for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy entities_own_select
  on backlot_sync_internal.entities for select to authenticated
  using (user_id = (select auth.uid()));
create policy entities_own_insert
  on backlot_sync_internal.entities for insert to authenticated
  with check (user_id = (select auth.uid()));
create policy entities_own_update
  on backlot_sync_internal.entities for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy changes_own_select
  on backlot_sync_internal.changes for select to authenticated
  using (user_id = (select auth.uid()));
create policy changes_own_insert
  on backlot_sync_internal.changes for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy applied_operations_own_select
  on backlot_sync_internal.applied_operations for select to authenticated
  using (user_id = (select auth.uid()));
create policy applied_operations_own_insert
  on backlot_sync_internal.applied_operations for insert to authenticated
  with check (user_id = (select auth.uid()));

-- One transaction covers: authentication, account-serial lock, idempotency,
-- optimistic concurrency check, entity/tombstone, change-log and reply.
-- No SECURITY DEFINER privilege bypass. Cannot work without caller's RLS
-- rights, and JWT account identity never comes from JSON payload.
create or replace function public.backlot_ux09_apply_v1(
    p_operation_id uuid,
    p_domain text,
    p_entity_id uuid,
    p_expected_revision bigint,
    p_payload jsonb,
    p_deleted boolean
) returns jsonb
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_user uuid := auth.uid();
    v_seq bigint;
    v_prev record;
    v_exists boolean;
    v_revision bigint;
    v_request jsonb;
    v_prior_request jsonb;
    v_prior_result jsonb;
    v_payload jsonb;
    v_deleted_at timestamptz;
    v_result jsonb;
begin
    if v_user is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'Authenticated non-anonymous account required' using errcode = '42501';
    end if;
    if p_operation_id is null or p_entity_id is null
       or p_operation_id = '00000000-0000-0000-0000-000000000000'::uuid
       or p_entity_id = '00000000-0000-0000-0000-000000000000'::uuid then
        raise exception 'Operation and entity UUID required' using errcode = '22023';
    end if;
    if p_domain is null or p_domain not in ('creator_project', 'creator_idea') then
        raise exception 'Unsupported UX09-01 domain' using errcode = '22023';
    end if;
    if p_expected_revision is null or p_expected_revision < 0
       or p_expected_revision >= 9223372036854775806 then
        raise exception 'Invalid expected revision' using errcode = '22023';
    end if;
    if p_deleted is null then
        raise exception 'Deletion flag must be explicit' using errcode = '22023';
    end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object'
       or octet_length(p_payload::text) > 16384 then
        raise exception 'Invalid or oversized object payload' using errcode = '22023';
    end if;
    if p_payload ?| array['user_id', 'account_id', 'service_role', 'alarm_lease',
                            'delivery_token', 'refresh_token', 'access_token'] then
        raise exception 'Reserved owner or device-specific field in payload' using errcode = '22023';
    end if;
    if p_deleted and p_payload <> '{}'::jsonb then
        raise exception 'Deletion must use an empty payload tombstone' using errcode = '22023';
    end if;

    v_payload := case when p_deleted then '{}'::jsonb else p_payload end;
    v_request := jsonb_build_object(
        'domain', p_domain, 'entity_id', p_entity_id,
        'expected_revision', p_expected_revision,
        'payload', v_payload, 'deleted', p_deleted
    );

    -- Serialize all writes within this account. Locking account row first
    -- ensures sequence order cannot commit out of order and retry races see
    -- the operation ID committed by the first request.
    insert into backlot_sync_internal.account_sequences (user_id, next_sequence)
        values (v_user, 1) on conflict (user_id) do nothing;
    select next_sequence into strict v_seq
      from backlot_sync_internal.account_sequences
     where user_id = v_user for update;

    select request, result into v_prior_request, v_prior_result
      from backlot_sync_internal.applied_operations
     where user_id = v_user and operation_id = p_operation_id;
    if found then
        if v_prior_request <> v_request then
            raise exception 'Operation UUID reused with different request'
              using errcode = '23505';
        end if;
        return v_prior_result;
    end if;

    select revision, deleted_at into v_prev
      from backlot_sync_internal.entities
     where user_id = v_user and domain = p_domain and entity_id = p_entity_id
     for update;
    v_exists := found;
    if (not v_exists and p_expected_revision <> 0)
       or (v_exists and v_prev.revision <> p_expected_revision)
       or (v_exists and v_prev.deleted_at is not null)
       or (not v_exists and p_deleted) then
        return jsonb_build_object(
            'status', 'conflict',
            'current_revision', case when v_exists then v_prev.revision else 0 end,
            'deleted', case when v_exists then v_prev.deleted_at is not null else false end,
            'domain', p_domain, 'entity_id', p_entity_id
        );
    end if;

    v_revision := case when v_exists then v_prev.revision + 1 else 1 end;
    v_deleted_at := case when p_deleted then now() else null end;
    if v_exists then
        update backlot_sync_internal.entities
           set revision = v_revision, payload = v_payload,
               deleted_at = v_deleted_at, updated_at = now(),
               last_operation_id = p_operation_id, last_sequence = v_seq
         where user_id = v_user and domain = p_domain and entity_id = p_entity_id;
    else
        insert into backlot_sync_internal.entities
            (user_id, domain, entity_id, revision, payload, deleted_at,
             last_operation_id, last_sequence)
        values
            (v_user, p_domain, p_entity_id, v_revision, v_payload, v_deleted_at,
             p_operation_id, v_seq);
    end if;

    insert into backlot_sync_internal.changes
        (user_id, sequence, domain, entity_id, revision, payload,
         deleted_at, operation_id)
    values
        (v_user, v_seq, p_domain, p_entity_id, v_revision, v_payload,
         v_deleted_at, p_operation_id);

    v_result := jsonb_build_object(
        'status', 'applied', 'sequence', v_seq, 'revision', v_revision,
        'domain', p_domain, 'entity_id', p_entity_id, 'deleted', p_deleted
    );
    insert into backlot_sync_internal.applied_operations
        (user_id, operation_id, request, result, sequence)
    values (v_user, p_operation_id, v_request, v_result, v_seq);

    update backlot_sync_internal.account_sequences
       set next_sequence = v_seq + 1 where user_id = v_user;
    return v_result;
end;
$function$;

-- Read-only, bounded, stable cursor source for later Android outbox.
-- Payload is the version AS COMMITTED at each change event (not current
-- latest entity state), so a cursor never silently skips a change.
create or replace function public.backlot_ux09_pull_v1(
    p_after_sequence bigint,
    p_limit integer
) returns jsonb
language plpgsql
stable
security invoker
set search_path = ''
as $function$
declare
    v_user uuid := auth.uid();
    v_changes jsonb;
    v_cursor bigint;
    v_has_more boolean;
begin
    if v_user is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'Authenticated non-anonymous account required' using errcode = '42501';
    end if;
    if p_after_sequence is null or p_after_sequence < 0
       or p_limit is null or p_limit < 1 or p_limit > 100 then
        raise exception 'Invalid sync page request' using errcode = '22023';
    end if;

    select coalesce(jsonb_agg(
            jsonb_build_object(
                'sequence', x.sequence, 'domain', x.domain,
                'entity_id', x.entity_id, 'revision', x.revision,
                'payload', x.payload, 'deleted', x.deleted_at is not null
            ) order by x.sequence
        ), '[]'::jsonb), coalesce(max(x.sequence), p_after_sequence)
      into v_changes, v_cursor
      from (
        select sequence, domain, entity_id, revision, payload, deleted_at
          from backlot_sync_internal.changes
         where user_id = v_user and sequence > p_after_sequence
         order by sequence
         limit p_limit
      ) as x;

    select exists(
        select 1 from backlot_sync_internal.changes
         where user_id = v_user and sequence > v_cursor
    ) into v_has_more;
    return jsonb_build_object(
        'changes', v_changes, 'next_cursor', v_cursor, 'has_more', v_has_more
    );
end;
$function$;

-- PostgreSQL grants EXECUTE on new functions to PUBLIC by default.
-- This entire source prototype is one transaction to avoid any privilege gap.
revoke all on function public.backlot_ux09_apply_v1(
    uuid, text, uuid, bigint, jsonb, boolean
) from public, anon;
revoke all on function public.backlot_ux09_pull_v1(bigint, integer)
    from public, anon;
grant execute on function public.backlot_ux09_apply_v1(
    uuid, text, uuid, bigint, jsonb, boolean
) to authenticated;
grant execute on function public.backlot_ux09_pull_v1(bigint, integer)
    to authenticated;

commit;

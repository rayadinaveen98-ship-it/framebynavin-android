-- UX09-01: synthetic SQL behavior tests ONLY against a throwaway PG16 DB.
-- Apply mock auth fixture and UX09 prototype first, then security assertions.
-- Uses Postgres ROLE authenticated + stub auth.uid() but NOT signed JWTs.
\set ON_ERROR_STOP on
set role authenticated;

do $cases$
declare
  v_a uuid := '11111111-1111-4111-8111-111111111111';
  v_b uuid := '22222222-2222-4222-8222-222222222222';
  v_entity uuid := '33333333-3333-4333-8333-333333333333';
  v_idea uuid := '44444444-4444-4444-8444-444444444444';
  v_op_create uuid := '55555555-5555-4555-8555-555555555555';
  v_r jsonb;
  v_r2 jsonb;
  v_page jsonb;
  v_count bigint;
begin
  perform set_config('request.jwt.is_anonymous','false',true);
  perform set_config('request.jwt.claim.sub',v_a::text,true);

  v_r := public.backlot_ux09_apply_v1(v_op_create,'creator_project',
    v_entity,0,'{"title":"Synthetic A"}'::jsonb,false);
  if v_r->>'status' <> 'applied' or (v_r->>'revision')::bigint <> 1 then
    raise exception 'A create revision 1 failed: %',v_r;
  end if;

  v_r2 := public.backlot_ux09_apply_v1(v_op_create,'creator_project',
    v_entity,0,'{"title":"Synthetic A"}'::jsonb,false);
  if v_r2 <> v_r then
    raise exception 'Idempotent retry did not return exactly original acknowledgement';
  end if;
  begin
    perform public.backlot_ux09_apply_v1(v_op_create,'creator_project',
      v_entity,0,'{"title":"Tampered retry"}'::jsonb,false);
    raise exception 'An operation UUID accepted a different request';
  exception when unique_violation then
    null; -- deliberate: request digest mismatch rejected
  end;

  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,0,'{"title":"Stale"}'::jsonb,false);
  if v_r->>'status' <> 'conflict' or (v_r->>'current_revision')::bigint <> 1 then
    raise exception 'Stale revision did not conflict';
  end if;

  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,1,'{"title":"A edit"}'::jsonb,false);
  if v_r->>'status' <> 'applied' or (v_r->>'revision')::bigint <> 2 then
    raise exception 'Revision 2 edit failed';
  end if;

  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,2,'{}'::jsonb,true);
  if v_r->>'status' <> 'applied' or (v_r->>'revision')::bigint <> 3
     or (v_r->>'deleted')::boolean is not true then
    raise exception 'Delete tombstone / revision 3 failed';
  end if;
  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,0,'{"title":"Resurrect"}'::jsonb,false);
  if v_r->>'status' <> 'conflict' or (v_r->>'deleted')::boolean is not true
     or (v_r->>'current_revision')::bigint <> 3 then
    raise exception 'Offline old create resurrected a tombstoned record';
  end if;

  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_idea',
    v_idea,0,'{"title":"Synthetic Idea"}'::jsonb,false);
  if v_r->>'status' <> 'applied' then
    raise exception 'Second Creator Idea domain failed';
  end if;

  v_page := public.backlot_ux09_pull_v1(0,1);
  if jsonb_array_length(v_page->'changes') <> 1
     or (v_page->>'has_more')::boolean is not true then
    raise exception 'Bounded page does not report remaining changes';
  end if;
  v_page := public.backlot_ux09_pull_v1(0,100);
  if jsonb_array_length(v_page->'changes') <> 4 or
     not exists (
       select 1 from jsonb_array_elements(v_page->'changes') as x
        where (x->>'revision')::bigint = 3
        and (x->>'deleted')::boolean is true
        and x->'payload' = '{}'::jsonb
     ) then
    raise exception 'Change log should contain exactly 4 immutable A events';
  end if;

  -- Switch to B using synthetic caller claim; direct table access must remain
  -- scoped by actual auth.uid and cannot see account A even with its UUID.
  perform set_config('request.jwt.claim.sub',v_b::text,true);
  select count(*) into v_count from backlot_sync_internal.entities
    where user_id = v_a;
  if v_count <> 0 then raise exception 'B read A entities across RLS'; end if;

  v_page := public.backlot_ux09_pull_v1(0,100);
  if jsonb_array_length(v_page->'changes') <> 0 then
    raise exception 'B pulled A events';
  end if;
  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,1,'{"title":"Cross owner edit"}'::jsonb,false);
  if v_r->>'status' <> 'conflict' or (v_r->>'current_revision')::bigint <> 0 then
    raise exception 'B read A project revision';
  end if;
  begin
    insert into backlot_sync_internal.entities (
      user_id, domain, entity_id, revision, payload,
      last_operation_id,last_sequence
    ) values (
      v_a,'creator_idea',gen_random_uuid(),1,'{}'::jsonb,
      gen_random_uuid(),99
    );
    raise exception 'B inserted an A-owned row';
  exception when insufficient_privilege then
    null; -- original RLS WITH CHECK rejected
  end;

  -- Foreign-owner UPDATE is invisible to tenant B due to USING + SELECT
  -- RLS, and foreign-owner hard DELETE must lack table privileges entirely.
  update backlot_sync_internal.entities
     set payload = '{"title":"B overwrite attempt"}'::jsonb
   where user_id = v_a and domain = 'creator_project' and entity_id = v_entity;
  get diagnostics v_count = row_count;
  if v_count <> 0 then
    raise exception 'B modified an A-owned entity through direct UPDATE';
  end if;
  begin
    delete from backlot_sync_internal.entities
     where user_id = v_a and domain = 'creator_project' and entity_id = v_entity;
    raise exception 'B was granted direct DELETE on private entity table';
  exception when insufficient_privilege then
    null;
  end;

  -- Same entity UUID under B is a different account-owned record.
  v_r := public.backlot_ux09_apply_v1(gen_random_uuid(),'creator_project',
    v_entity,0,'{"title":"Synthetic B"}'::jsonb,false);
  if v_r->>'status' <> 'applied' then
    raise exception 'B could not own a separate record with same UUID';
  end if;

  perform set_config('request.jwt.claim.sub',v_a::text,true);
  v_page := public.backlot_ux09_pull_v1(0,100);
  if exists (
    select 1 from jsonb_array_elements(v_page->'changes') as x
    where x->'payload'->>'title' = 'Synthetic B'
  ) then raise exception 'A observed B data after account switch'; end if;

  -- Deliberately reject unsupported private Personal data in v1.
  begin
    perform public.backlot_ux09_apply_v1(gen_random_uuid(),'personal_photo',
      gen_random_uuid(),0,'{"title":"Private"}'::jsonb,false);
    raise exception 'Unsupported private domain was accepted';
  exception when invalid_parameter_value then
    null;
  end;

  raise notice 'PASS: disposable SQL scenarios for CAS, idempotency, RLS, 2 accounts, tombstones, cursor and two domains';
end;
$cases$;
reset role;

-- Backlot UX09-01: read-only schema/security assertions.
-- Run ONLY after applying the corresponding prototype to a disposable LOCAL
-- Supabase PostgreSQL. Does not create, update or delete application records.
-- It cannot validate true A/B JWT behavior: use local-only QA shell harness.

do $audit$
declare
  v_table text;
  v_fn text;
  v_count integer;
begin
  if to_regnamespace('backlot_sync_internal') is null then
    raise exception 'UX09-01 internal schema missing';
  end if;
  -- The anon effective role check includes grants inherited from PUBLIC.
  if has_schema_privilege('anon','backlot_sync_internal','USAGE') then
    raise exception 'Anonymous users can reach the private sync schema';
  end if;

  foreach v_table in array array[
    'account_sequences', 'entities', 'changes', 'applied_operations'
  ] loop
    if to_regclass('backlot_sync_internal.' || v_table) is null then
      raise exception 'Missing internal sync table %', v_table;
    end if;
    if exists (
      select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
      where n.nspname='backlot_sync_internal' and c.relname=v_table
        and not c.relrowsecurity
    ) then
      raise exception 'RLS disabled for internal sync table %', v_table;
    end if;
    if has_table_privilege('anon','backlot_sync_internal.' || v_table,'SELECT')
       or has_table_privilege('anon','backlot_sync_internal.' || v_table,'INSERT')
       or has_table_privilege('anon','backlot_sync_internal.' || v_table,'UPDATE')
       or has_table_privilege('anon','backlot_sync_internal.' || v_table,'DELETE') then
      raise exception 'Anonymous direct table access detected: %', v_table;
    end if;
    if has_table_privilege('authenticated','backlot_sync_internal.' || v_table,'DELETE') then
      raise exception 'Direct authenticated DELETE would bypass tombstones: %', v_table;
    end if;
    select count(*) into v_count from pg_policies
      where schemaname='backlot_sync_internal' and tablename=v_table
        and (qual like '%auth.uid()%' or with_check like '%auth.uid()%');
    if v_count < 2 then
      raise exception 'Not enough owner-bound RLS policies for %', v_table;
    end if;
  end loop;

  foreach v_fn in array array[
    'backlot_ux09_apply_v1', 'backlot_ux09_pull_v1'
  ] loop
    if not exists (
      select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
      where n.nspname='public' and p.proname=v_fn and not p.prosecdef
    ) then
      raise exception 'Missing or SECURITY DEFINER UX09 RPC: %', v_fn;
    end if;
  end loop;

  if has_function_privilege(
      'anon','public.backlot_ux09_apply_v1(uuid,text,uuid,bigint,jsonb,boolean)',
      'EXECUTE'
  ) or has_function_privilege(
      'anon','public.backlot_ux09_pull_v1(bigint,integer)','EXECUTE'
  ) then
      raise exception 'Anonymous RPC execution enabled';
  end if;
  if not has_function_privilege(
      'authenticated','public.backlot_ux09_apply_v1(uuid,text,uuid,bigint,jsonb,boolean)',
      'EXECUTE'
  ) or not has_function_privilege(
      'authenticated','public.backlot_ux09_pull_v1(bigint,integer)','EXECUTE'
  ) then
      raise exception 'Authenticated invocation missing';
  end if;
  raise notice 'UX09-01 static disposable role/RLS/RPC assertions passed';
end;
$audit$;

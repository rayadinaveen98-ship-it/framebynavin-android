-- Backlot UX09-01 hosted-Lab READ-ONLY migration preflight.
-- NO INSERT/UPDATE/DELETE/ALTER/CREATE, no access to user payloads.
-- Intended for owner-reviewed Backlot Lab only. Inspect results before DDL.
-- Data API "Exposed schemas" is a control-plane setting; SQL alone cannot
-- certify it. Verify Dashboard > Integrations > Data API separately.
select
  current_setting('server_version') as server_version,
  (to_regnamespace('backlot_sync_internal') is not null) as sync_schema_exists,
  (select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace
   where n.nspname='public' and p.proname in
    ('backlot_ux09_apply_v1','backlot_ux09_pull_v1')) as sync_rpc_collisions,
  (select count(*) from information_schema.tables
   where table_schema='public' and table_name in
    ('creator_profiles','creator_sync_snapshots',
     'creator_sync_heads','personal_sync_heads')) as existing_app_table_count,
  (select count(*) from supabase_migrations.schema_migrations)
     as existing_migration_count,
  has_schema_privilege('anon','public','CREATE') as anon_can_create_public,
  has_schema_privilege('authenticated','public','CREATE')
    as authenticated_can_create_public;

select
  c.relname as existing_app_table,
  c.relrowsecurity as rls_enabled,
  (select count(*) from pg_policies p
   where p.schemaname='public' and p.tablename=c.relname) as policy_count
from pg_class c join pg_namespace n on n.oid=c.relnamespace
where n.nspname='public' and c.relname in
 ('creator_profiles','creator_sync_snapshots',
  'creator_sync_heads','personal_sync_heads')
order by c.relname;

-- A schema named backlot_sync_internal must NOT preexist. If it exists,
-- inspect and stop; rerunning a CREATE-only SQL prototype is not idempotent.
-- Before applying a vetted CLI migration, also verify that the private
-- schema is NOT on PostgREST's Exposed Schemas list in the Dashboard.

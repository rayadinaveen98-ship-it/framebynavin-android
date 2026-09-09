#!/usr/bin/env python3
"""Generate a rollback-only PostgreSQL test from the exact RC8 migration.

Never points at production creator/Auth schemas. Runs only in disposable PostgreSQL.
"""
from pathlib import Path
import hashlib
import sys

root = Path(__file__).resolve().parents[1]
migration = (root / 'migrations/20260909000000_creator_deletion_barrier.sql').read_text()
assert 'DROP TABLE' not in migration.upper()
assert 'CREATE OR REPLACE FUNCTION public.creator_delete_owned_data(p_expected_generation bigint)' in migration
isolated = migration.replace('creator_guard_v183', 'rc8_guard_test')
isolated = isolated.replace('public.creator_', 'rc8_public_test.creator_')
isolated = isolated.replace('public.%I', 'rc8_public_test.%I')
isolated = isolated.replace('auth.users', 'rc8_auth_test.users').replace('auth.uid()', 'rc8_auth_test.uid()')
# Test-only schemas, identities and tables; no real Supabase objects are touched.
setup = r'''
BEGIN;
CREATE SCHEMA rc8_auth_test;
CREATE SCHEMA rc8_public_test;
CREATE FUNCTION rc8_auth_test.uid() RETURNS uuid LANGUAGE sql STABLE AS $f$
  SELECT nullif(current_setting('request.jwt.claims', true), '')::jsonb->>'sub'
$f$;
'''.replace("SELECT nullif(current_setting('request.jwt.claims', true), '')::jsonb->>'sub'", "SELECT (nullif(current_setting('request.jwt.claims', true), '')::jsonb->>'sub')::uuid") + r'''
CREATE TABLE rc8_auth_test.users(id uuid PRIMARY KEY);
CREATE TABLE rc8_public_test.creator_backups(id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES rc8_auth_test.users(id), payload text);
CREATE TABLE rc8_public_test.creator_devices(id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES rc8_auth_test.users(id));
CREATE TABLE rc8_public_test.creator_profiles(user_id uuid PRIMARY KEY REFERENCES rc8_auth_test.users(id), username text);
'''
post = r'''
-- Test users are synthetic and exist only inside the rolled-back transaction.
INSERT INTO rc8_auth_test.users VALUES
 ('00000000-0000-4000-8000-0000000000a1'),('00000000-0000-4000-8000-0000000000b2');
GRANT USAGE ON SCHEMA rc8_auth_test, rc8_public_test TO authenticated;
GRANT EXECUTE ON FUNCTION rc8_auth_test.uid() TO authenticated;
GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA rc8_public_test TO authenticated;
DO $p$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['creator_backups','creator_devices','creator_profiles'] LOOP
  EXECUTE format('ALTER TABLE rc8_public_test.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY owner_access ON rc8_public_test.%I FOR ALL TO authenticated USING (rc8_auth_test.uid()=user_id) WITH CHECK (rc8_auth_test.uid()=user_id)',t);
 END LOOP;
END $p$;
CREATE FUNCTION rc8_public_test.synthetic_delete_failure() RETURNS trigger LANGUAGE plpgsql AS $f$
BEGIN
 IF current_setting('rc8.inject_delete_failure',true)='yes' AND OLD.user_id='00000000-0000-4000-8000-0000000000a1'::uuid THEN
  RAISE EXCEPTION 'Synthetic deletion failure';
 END IF;
 RETURN OLD;
END $f$;
CREATE TRIGGER synthetic_delete_failure BEFORE DELETE ON rc8_public_test.creator_devices FOR EACH ROW EXECUTE FUNCTION rc8_public_test.synthetic_delete_failure();
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims','{"sub":"00000000-0000-4000-8000-0000000000a1","role":"authenticated"}',true);
SELECT set_config('request.headers','{}',true);
DO $test$
DECLARE a uuid:='00000000-0000-4000-8000-0000000000a1'; b uuid:='00000000-0000-4000-8000-0000000000b2'; s jsonb;
BEGIN
 IF has_schema_privilege('authenticated','rc8_guard_test','USAGE') THEN RAISE EXCEPTION 'Private schema exposed'; END IF;
 IF has_function_privilege('anon','rc8_public_test.creator_delete_owned_data(bigint)','EXECUTE') THEN RAISE EXCEPTION 'Anonymous deletion allowed'; END IF;
 IF has_function_privilege('authenticated','rc8_guard_test.lock_state(uuid)','EXECUTE') THEN RAISE EXCEPTION 'Private helper exposed'; END IF;
 IF has_table_privilege('authenticated','rc8_guard_test.lifecycle','SELECT') THEN RAISE EXCEPTION 'Lifecycle table exposed'; END IF;
 INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(a,'synthetic-A');
 INSERT INTO rc8_public_test.creator_devices(user_id) VALUES(a);
 INSERT INTO rc8_public_test.creator_profiles(user_id,username) VALUES(a,'synthetic-a');
 PERFORM set_config('request.jwt.claims',jsonb_build_object('sub',b,'role','authenticated')::text,true);
 INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(b,'synthetic-B');
 INSERT INTO rc8_public_test.creator_profiles(user_id,username) VALUES(b,'synthetic-b');
 PERFORM set_config('request.jwt.claims',jsonb_build_object('sub',a,'role','authenticated')::text,true);
 -- An injected failure must roll back the tombstone and all preceding deletes.
 PERFORM set_config('rc8.inject_delete_failure','yes',true);
 BEGIN
  PERFORM rc8_public_test.creator_delete_owned_data(0);
  RAISE EXCEPTION 'Expected deletion failure was not raised';
 EXCEPTION WHEN raise_exception THEN
  IF SQLERRM='Expected deletion failure was not raised' THEN RAISE; END IF;
 END;
 PERFORM set_config('rc8.inject_delete_failure','no',true);
 IF rc8_public_test.creator_cloud_status()<>jsonb_build_object('phase','active','generation',0) THEN RAISE EXCEPTION 'Failed deletion changed state'; END IF;
 IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=a)<>1 OR (SELECT count(*) FROM rc8_public_test.creator_devices WHERE user_id=a)<>1 THEN RAISE EXCEPTION 'Failed deletion lost rows'; END IF;
 s:=rc8_public_test.creator_delete_owned_data(0);
 IF s<>jsonb_build_object('phase','deleted','generation',1) THEN RAISE EXCEPTION 'Deletion state failed'; END IF;
 IF EXISTS(SELECT 1 FROM rc8_public_test.creator_backups WHERE user_id=a) OR EXISTS(SELECT 1 FROM rc8_public_test.creator_devices WHERE user_id=a) OR EXISTS(SELECT 1 FROM rc8_public_test.creator_profiles WHERE user_id=a) THEN RAISE EXCEPTION 'Owner data survived'; END IF;
 IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner data changed'; END IF;
 BEGIN INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(a,'resurrection'); RAISE EXCEPTION 'Deleted write accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 BEGIN UPDATE rc8_public_test.creator_profiles SET username='resurrection' WHERE user_id=a; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 IF rc8_public_test.creator_delete_owned_data(0)<>s OR rc8_public_test.creator_delete_owned_data(1)<>s THEN RAISE EXCEPTION 'Delete retry not idempotent'; END IF;
 BEGIN PERFORM rc8_public_test.creator_resume_owned_data(0); RAISE EXCEPTION 'Stale resume accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 s:=rc8_public_test.creator_resume_owned_data(1);
 IF s<>jsonb_build_object('phase','active','generation',2) THEN RAISE EXCEPTION 'Reactivation did not advance epoch'; END IF;
 BEGIN PERFORM rc8_public_test.creator_delete_owned_data(0); RAISE EXCEPTION 'Old deletion replay accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 BEGIN PERFORM rc8_public_test.creator_delete_owned_data(1); RAISE EXCEPTION 'Previous deletion retry accepted after reactivation'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 BEGIN INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(a,'stale'); RAISE EXCEPTION 'Old writer accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 PERFORM set_config('request.headers','{"x-creator-write-epoch":"2"}',true);
 INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(a,'fresh');
 BEGIN INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES(b,'cross-owner'); RAISE EXCEPTION 'Cross-owner write accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner changed'; END IF;
 PERFORM set_config('request.headers','{}',true);
 BEGIN DELETE FROM rc8_public_test.creator_backups WHERE user_id=a; RAISE EXCEPTION 'Legacy stale delete accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 PERFORM set_config('request.headers','{"x-creator-write-epoch":"2"}',true);
 IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=a)<>1 THEN RAISE EXCEPTION 'Stale delete removed new data'; END IF;
 IF rc8_public_test.creator_resume_owned_data(2)<>s THEN RAISE EXCEPTION 'Repeated resume changed epoch'; END IF;
 PERFORM set_config('request.headers','{"x-creator-write-epoch":"bad"}',true);
 BEGIN INSERT INTO rc8_public_test.creator_devices(user_id) VALUES(a); RAISE EXCEPTION 'Malformed epoch accepted'; EXCEPTION WHEN invalid_parameter_value THEN NULL; END;
 PERFORM set_config('request.headers','{"x-creator-write-epoch":"2"}',true);
 s:=rc8_public_test.creator_delete_owned_data(2);
 IF s<>jsonb_build_object('phase','deleted','generation',3) THEN RAISE EXCEPTION 'Second deletion failed'; END IF;
 PERFORM set_config('request.jwt.claims',jsonb_build_object('sub',b,'role','authenticated')::text,true);
 PERFORM set_config('request.headers','{}',true);
 IF (rc8_public_test.creator_cloud_status()->>'generation')::bigint<>0 THEN RAISE EXCEPTION 'Other owner epoch changed'; END IF;
 IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner lost data'; END IF;
 BEGIN PERFORM rc8_public_test.creator_resume_owned_data(3); RAISE EXCEPTION 'Cross-owner resume accepted'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $test$;
SELECT jsonb_build_object('synthetic_tests','passed','transaction','rollback','real_creator_rows_touched',false) AS result;
ROLLBACK;
'''
# A local disposable Postgres can supply the two no-login API roles. Supabase already has them.
bootstrap = '''DO $roles$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='anon') THEN CREATE ROLE anon NOLOGIN; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN CREATE ROLE authenticated NOLOGIN; END IF;
END $roles$;
'''
# Role creation is inside the rollback transaction and cannot survive a test.
out = setup + bootstrap + isolated + post
(root / 'tests/isolated_acceptance.sql').write_text(out)
print('source_sha256',hashlib.sha256(migration.encode()).hexdigest())
print('test_sha256',hashlib.sha256(out.encode()).hexdigest())
print('test_bytes',len(out.encode()))

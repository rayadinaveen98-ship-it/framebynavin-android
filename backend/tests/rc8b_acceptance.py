#!/usr/bin/env python3
"""RC8b: exact-source SQL, Auth FK cascade and separate-session race tests.

DISPOSABLE GITHUB CI ONLY. No remote database, deployment, or real users.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
MIGRATION = ROOT / 'migrations/20260909000000_creator_deletion_barrier.sql'
ORIGINAL_SHA = 'be3f2ac7b1eca945c443f370e90cad5398a974d53648d91c83bb9400196e0f71'
REVISED_SHA = '61425ebedad2ff4eaaf85b0c58dbe2382e0c35360f9b2416ab207c35139be45a'
A = '00000000-0000-4000-8000-0000000000a1'
B = '00000000-0000-4000-8000-0000000000b2'
C = '00000000-0000-4000-8000-0000000000c3'
D = '00000000-0000-4000-8000-0000000000d4'


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def guard_environment():
    assert os.environ.get('CI') == 'true', 'This test is only permitted in CI.'
    assert os.environ.get('PGHOST') == '127.0.0.1'
    assert os.environ.get('PGPORT', '5432') == '5432'
    assert os.environ.get('PGDATABASE') == 'rc8_synthetic'
    assert os.environ.get('PGUSER') == 'postgres'
    assert not os.environ.get('DATABASE_URL')
    assert not os.environ.get('SUPABASE_DB_URL')


def sql(source: str, *, role: str | None = None, claims: str | None = None,
        epoch: int | None = None) -> str:
    prefix = ''
    if role is not None:
        prefix += f'SET SESSION AUTHORIZATION {role};\n'
        if role == 'authenticator':
            prefix += 'SET ROLE authenticated;\n'
    if claims is not None:
        prefix += "SELECT set_config('request.jwt.claims', '" + claims + "', false);\n"
    if epoch is not None:
        prefix += f"SELECT set_config('request.headers','{{\"x-creator-write-epoch\":\"{epoch}\"}}',false);\n"
    else:
        prefix += "SELECT set_config('request.headers','{}',false);\n"
    return prefix + source


def api(source: str, user: str = A, epoch: int | None = None) -> str:
    return sql(source, role='authenticator',
               claims=json.dumps({'sub': user, 'role': 'authenticated'}), epoch=epoch)


def run(source: str, *, expected: str | None = None, timeout: int = 25) -> str:
    proc = subprocess.run(['psql','-X','-A','-t','-v','ON_ERROR_STOP=1',
                           '-v','VERBOSITY=verbose','-P','pager=off'],
                          input=source, text=True, capture_output=True, timeout=timeout)
    output = proc.stdout + proc.stderr
    if expected is None:
        assert proc.returncode == 0, output
    else:
        assert proc.returncode != 0 and expected in output, output
    return proc.stdout


def isolated(source: str) -> str:
    return (source.replace('creator_guard_v183','rc8_guard_test')
                  .replace('public.creator_','rc8_public_test.creator_')
                  .replace('public.%I','rc8_public_test.%I')
                  .replace('auth.users','rc8_auth_test.users')
                  .replace('auth.uid()','rc8_auth_test.uid()'))


def source_check() -> tuple[str,str]:
    original = subprocess.check_output(['git','show',
        'c52cafd1deeaa6957bdd376bc9617fa49d5a0b60:backend/migrations/20260909000000_creator_deletion_barrier.sql'],
        cwd=ROOT.parent).decode()
    assert digest(original.encode()) == ORIGINAL_SHA
    patch = ROOT / 'tests/rc8b-auth-cascade.patch'
    assert digest(patch.read_bytes()) == '1857b6d7ff6aeea3e96010a7b84353338ee6c4c2761cf1b706849290b5ba14a6'
    revised = MIGRATION
    assert digest(revised.read_bytes()) == REVISED_SHA
    return original, revised.read_text()


def core_tests(original: str, revised: str):
    fixture = (ROOT / 'tests/isolated_acceptance_v3.sql').read_text()
    assert digest(fixture.encode()) == '8471413e65a1507b51b78e713276635c611e0732a692a21d6fec0f04265ba18c'
    old = isolated(original)
    assert fixture.count(old) == 1
    fixture = fixture.replace(old, isolated(revised))
    assert fixture.lstrip().startswith('BEGIN;') and fixture.rstrip().endswith('ROLLBACK;')
    assert 'DELETE FROM auth.users' not in fixture
    result = run(fixture)
    assert '"synthetic_tests": "passed"' in result
    print('PASS: original rollback-only lifecycle and RLS suite')


def setup(revised: str):
    # All objects belong to this disposable database. Commit enables independent
    # transactions and sessions; the database/container itself is discarded by CI.
    source = '''BEGIN;
CREATE SCHEMA rc8_auth_test;
CREATE SCHEMA rc8_public_test;
CREATE ROLE anon NOLOGIN;
CREATE ROLE authenticated NOLOGIN;
CREATE ROLE authenticator LOGIN NOINHERIT;
GRANT authenticated TO authenticator;
CREATE ROLE supabase_auth_admin LOGIN NOINHERIT;
CREATE FUNCTION rc8_auth_test.uid() RETURNS uuid LANGUAGE sql STABLE AS $f$
 SELECT (nullif(current_setting('request.jwt.claims',true),'')::jsonb->>'sub')::uuid
$f$;
CREATE TABLE rc8_auth_test.users(id uuid PRIMARY KEY);
CREATE TABLE rc8_public_test.creator_backups(id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES rc8_auth_test.users(id) ON DELETE CASCADE, payload text);
CREATE TABLE rc8_public_test.creator_devices(id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES rc8_auth_test.users(id) ON DELETE CASCADE);
CREATE TABLE rc8_public_test.creator_profiles(user_id uuid PRIMARY KEY REFERENCES rc8_auth_test.users(id) ON DELETE CASCADE, username text);
'''
    source += isolated(revised) + f'''
GRANT USAGE ON SCHEMA rc8_auth_test, rc8_public_test TO authenticated, supabase_auth_admin;
GRANT EXECUTE ON FUNCTION rc8_auth_test.uid() TO authenticated;
GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA rc8_public_test TO authenticated;
GRANT SELECT,DELETE ON rc8_auth_test.users TO supabase_auth_admin;
DO $p$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['creator_backups','creator_devices','creator_profiles'] LOOP
  EXECUTE format('ALTER TABLE rc8_public_test.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY owner_access ON rc8_public_test.%I FOR ALL TO authenticated USING (rc8_auth_test.uid()=user_id) WITH CHECK (rc8_auth_test.uid()=user_id)',t);
 END LOOP;
END $p$;
INSERT INTO rc8_auth_test.users VALUES
 ('{A}'),('{B}'),('{C}'),('{D}');
COMMIT;
'''
    run(source)
    # Only the synthetic API login can assume authenticated. The admin role
    # cannot assume it and the public API cannot assume an admin login.
    checks = run("SELECT pg_has_role('authenticator','supabase_auth_admin','MEMBER');")
    assert checks.strip() == 'f'
    print('PASS: disposable schema, cascade FKs, roles and RLS installed')


def assert_count(table: str, user: str, count: int):
    result = run(f"SELECT count(*) FROM rc8_public_test.{table} WHERE user_id='{user}'::uuid;")
    assert result.strip() == str(count), (table,user,count,result)


def assert_status(user: str, phase: str, generation: int):
    result = run(api('SELECT rc8_public_test.creator_cloud_status();',user))
    assert json.loads(result.strip().splitlines()[-1]) == {'phase':phase,'generation':generation},result


def all_deleted(user: str):
    for table in ('creator_backups','creator_devices','creator_profiles'):
        assert_count(table,user,0)


def insert_backup(user: str, value: str, epoch: int | None = None):
    run(api(f"INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES('{user}','{value}');",user,epoch))


def admin(source: str) -> str:
    return sql(source,role='supabase_auth_admin')


def auth_cascade_tests():
    # A deletion performed by the real Auth database login has no user JWT.
    # Direct table deletes remain forbidden; an FK cascade may remove rows.
    for user in (A,B):
        insert_backup(user, 'original')
    run(api(f"INSERT INTO rc8_public_test.creator_devices(user_id) VALUES('{A}');\n"
            f"INSERT INTO rc8_public_test.creator_profiles(user_id,username) VALUES('{A}','synthetic');"))
    run(admin(f"DELETE FROM rc8_public_test.creator_backups WHERE user_id='{A}';"),expected='42501')
    run(sql(f"DELETE FROM rc8_public_test.creator_backups WHERE user_id='{A}';"),expected='42501')
    run(sql(f"DELETE FROM rc8_public_test.creator_backups WHERE user_id='{A}';",role='authenticator'),expected='42501')
    run(admin(f"DELETE FROM rc8_auth_test.users WHERE id='{A}';"))
    all_deleted(A)
    assert run(f"SELECT count(*) FROM rc8_guard_test.lifecycle WHERE user_id='{A}';").strip() == '0'
    assert_count('creator_backups',B,1)
    # An expired identity cannot be recreated through a still-valid JWT.
    run(api(f"INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES('{A}','resurrection');"),expected='42501')
    print('PASS: trusted Auth cascade removes active creator rows, retains other owner, and prevents resurrection')


def start(source: str) -> subprocess.Popen:
    proc = subprocess.Popen(['psql','-X','-A','-t','-v','ON_ERROR_STOP=1',
                             '-v','VERBOSITY=verbose','-P','pager=off'],
                            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    proc.stdin.write(source)
    proc.stdin.close()
    proc.stdin = None
    return proc


def active_lock(user: str, timeout: float = 8):
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        result=run(f"BEGIN; SELECT pg_try_advisory_xact_lock(183,hashtext('{user}')); ROLLBACK;")
        if result.strip().splitlines()[-2] == 'f':
            return
        time.sleep(.05)
    raise AssertionError('Writer did not obtain lifecycle lock')


def finish(proc: subprocess.Popen, timeout: int = 15) -> str:
    out,err=proc.communicate(timeout=timeout)
    assert proc.returncode==0,out+err
    return out


def concurrent_write_delete():
    # Writer holds the parent KEY SHARE and lifecycle lock while a second
    # connection attempts deletion. The latter must wait, then delete its write.
    writer=start(api(f"BEGIN; INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES('{C}','inflight'); SELECT pg_sleep(2); COMMIT;",C))
    active_lock(C)
    deleter=start(api('SELECT rc8_public_test.creator_delete_owned_data(0);',C))
    time.sleep(.15)
    assert deleter.poll() is None, 'Deletion escaped the writer lock'
    finish(writer)
    finish(deleter)
    assert_status(C,'deleted',1)
    all_deleted(C)
    run(api(f"INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES('{C}','stale');",C),expected='42501')
    print('PASS: concurrent writer commits before deletion; no stale resurrection')


def concurrent_auth_delete():
    # A parent row lock is taken before the lifecycle lock. The Auth deletion
    # must wait for the writer, avoiding the opposite-lock-order deadlock.
    writer=start(api(f"BEGIN; INSERT INTO rc8_public_test.creator_backups(user_id,payload) VALUES('{D}','inflight'); SELECT pg_sleep(2); COMMIT;",D))
    active_lock(D)
    deleter=start(admin(f"DELETE FROM rc8_auth_test.users WHERE id='{D}';"))
    time.sleep(.15)
    assert deleter.poll() is None, 'Auth deletion escaped the writer lock'
    finish(writer)
    finish(deleter)
    all_deleted(D)
    assert run(f"SELECT count(*) FROM rc8_guard_test.lifecycle WHERE user_id='{D}';").strip()=='0'
    print('PASS: Auth deletion waits for in-flight creator writer without deadlock')


def reactivation_tests():
    result = run(api('SELECT rc8_public_test.creator_resume_owned_data(1);',C))
    assert json.loads(result.strip().splitlines()[-1]) == {'phase':'active','generation':2}
    run(api('SELECT rc8_public_test.creator_delete_owned_data(0);',C),expected='42501')
    run(api('SELECT rc8_public_test.creator_delete_owned_data(1);',C),expected='42501')
    insert_backup(C,'fresh',2)
    run(api(f"DELETE FROM rc8_public_test.creator_backups WHERE user_id='{C}';",C),expected='42501')
    assert_count('creator_backups',C,1)
    run(api('SELECT rc8_public_test.creator_delete_owned_data(2);',C,2))
    assert_status(C,'deleted',3)
    all_deleted(C)
    assert_count('creator_backups',B,1)
    print('PASS: reactivation fences old writes and deletion requests, preserving other owner')


def main():
    guard_environment()
    original,revised=source_check()
    core_tests(original,revised)
    setup(revised)
    auth_cascade_tests()
    concurrent_write_delete()
    concurrent_auth_delete()
    reactivation_tests()
    print(json.dumps({'rc8b':'passed','core':'passed','auth_cascade':'passed',
                      'separate_session_concurrency':'passed','production_changes':False,
                      'migration_sha256':REVISED_SHA},sort_keys=True))

if __name__=='__main__':
    main()

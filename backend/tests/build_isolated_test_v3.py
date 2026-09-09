#!/usr/bin/env python3
"""Correct the RC8 RLS visibility fixture without changing the migration."""
from pathlib import Path
import hashlib
import runpy

root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / 'tests/build_isolated_test_v2.py'), run_name='__main__')
source = root / 'tests/isolated_acceptance_v2.sql'
text = source.read_text()
assert hashlib.sha256(text.encode()).hexdigest() == '25a69ac78e24eacf869acaa519e2eb2cf7b5865cff3d755c87ce8cb6bf4fd4a7'
inspector = '''-- Test-only privileged snapshot, installed before impersonating an API role.
-- This observes both synthetic owners without weakening their RLS policies.
CREATE FUNCTION rc8_public_test.audit_rows() RETURNS jsonb
LANGUAGE sql SECURITY DEFINER SET search_path = '' AS $audit$
 SELECT pg_catalog.jsonb_build_object(
   'a_backups',(SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id='00000000-0000-4000-8000-0000000000a1'::uuid),
   'b_backups',(SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id='00000000-0000-4000-8000-0000000000b2'::uuid),
   'a_devices',(SELECT count(*) FROM rc8_public_test.creator_devices WHERE user_id='00000000-0000-4000-8000-0000000000a1'::uuid),
   'a_profiles',(SELECT count(*) FROM rc8_public_test.creator_profiles WHERE user_id='00000000-0000-4000-8000-0000000000a1'::uuid),
   'b_profiles',(SELECT count(*) FROM rc8_public_test.creator_profiles WHERE user_id='00000000-0000-4000-8000-0000000000b2'::uuid)
 );
$audit$;
REVOKE ALL ON FUNCTION rc8_public_test.audit_rows() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION rc8_public_test.audit_rows() TO authenticated;
'''
needle = 'SET LOCAL ROLE authenticated;'
assert text.count(needle) == 1
text = text.replace(needle, inspector + needle)
replacements = {
    "IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner data changed'; END IF;": "IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>0 THEN RAISE EXCEPTION 'RLS exposed another owner'; END IF;\n IF (rc8_public_test.audit_rows()->>'b_backups')::bigint<>1 THEN RAISE EXCEPTION 'Other owner data changed'; END IF;",
    "IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner changed'; END IF;": "IF (rc8_public_test.audit_rows()->>'b_backups')::bigint<>1 THEN RAISE EXCEPTION 'Other owner changed'; END IF;",
    "IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner lost data'; END IF;": "IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=b)<>1 THEN RAISE EXCEPTION 'Other owner lost data'; END IF;\n IF (SELECT count(*) FROM rc8_public_test.creator_backups WHERE user_id=a)<>0 THEN RAISE EXCEPTION 'RLS exposed owner A to B'; END IF;\n IF (rc8_public_test.audit_rows()->>'a_backups')::bigint<>0 THEN RAISE EXCEPTION 'Owner A data resurrected'; END IF;",
}
for old, new in replacements.items():
    assert text.count(old) == 1, old
    text = text.replace(old, new)
assert hashlib.sha256(text.encode()).hexdigest() == '8471413e65a1507b51b78e713276635c611e0732a692a21d6fec0f04265ba18c'
(root / 'tests/isolated_acceptance_v3.sql').write_text(text)
print('RC8 v3 fixture SHA-256:', hashlib.sha256(text.encode()).hexdigest())

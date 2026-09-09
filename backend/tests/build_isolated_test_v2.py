#!/usr/bin/env python3
"""Correct the isolated privilege fixture without changing the original migration."""
from pathlib import Path
import hashlib
import runpy

root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / 'tests/build_isolated_test.py'), run_name='__main__')
source = root / 'tests/isolated_acceptance.sql'
text = source.read_text()
assert hashlib.sha256(text.encode()).hexdigest() == 'ddca1e0321c05435db38a3af51608a8320b19fe762ef558622cb10dfc0df77ad'
replacements = {
    "has_function_privilege('authenticated','rc8_guard_test.lock_state(uuid)','EXECUTE')": "has_function_privilege('authenticated', (SELECT p.oid FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='rc8_guard_test' AND p.proname='lock_state'), 'EXECUTE')",
    "has_table_privilege('authenticated','rc8_guard_test.lifecycle','SELECT')": "has_table_privilege('authenticated', (SELECT c.oid FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='rc8_guard_test' AND c.relname='lifecycle'), 'SELECT')",
}
for old, new in replacements.items():
    assert text.count(old) == 1, f'Expected exactly one fixture location: {old}'
    text = text.replace(old, new)
assert hashlib.sha256(text.encode()).hexdigest() == '25a69ac78e24eacf869acaa519e2eb2cf7b5865cff3d755c87ce8cb6bf4fd4a7'
(root / 'tests/isolated_acceptance_v2.sql').write_text(text)
print('RC8 v2 fixture SHA-256:', hashlib.sha256(text.encode()).hexdigest())

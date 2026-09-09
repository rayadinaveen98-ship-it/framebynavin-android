#!/usr/bin/env python3
"""Verify and unpack the immutable RC8 source package. No network or secrets."""
import base64
import hashlib
import json
import pathlib
import sys
import zlib

EXPECTED_ARCHIVE = '00f6ea2aac790b580ed5ef9a800f71bf1e301d57b56980b5de0c576f1e3d2670'
EXPECTED_MIGRATION = 'be3f2ac7b1eca945c443f370e90cad5398a974d53648d91c83bb9400196e0f71'
EXPECTED_GENERATOR = '1470521334b103149176861faa92dd4538189fd4795ea96cd59c0204ecb3a298'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def unpack(package, destination):
    data = pathlib.Path(package).read_bytes()
    assert digest(data) == EXPECTED_ARCHIVE, 'Source archive SHA-256 mismatch'
    root = pathlib.Path(destination).resolve()
    import zipfile
    with zipfile.ZipFile(package) as archive:
        assert sorted(archive.namelist()) == sorted([
            'backend/manifest.json',
            'backend/migrations/20260909000000_creator_deletion_barrier.sql',
            'backend/tests/README.md',
            'backend/tests/build_isolated_test.py',
            'backend/tests/isolated_acceptance.sql',
        ]), 'Unexpected source archive contents'
        for name in archive.namelist():
            target = (root / name).resolve()
            assert target.is_relative_to(root), 'Unsafe archive path'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
    migration = root / 'backend/migrations/20260909000000_creator_deletion_barrier.sql'
    generator = root / 'backend/tests/build_isolated_test.py'
    assert digest(migration.read_bytes()) == EXPECTED_MIGRATION
    assert digest(generator.read_bytes()) == EXPECTED_GENERATOR
    manifest = json.loads((root / 'backend/manifest.json').read_text())
    assert manifest['base_commit'] == '7e6913321726f2221054cd03b3e4df2483e4a7cb'
    print('Exact RC8 candidate verified. No production changes performed.')

if __name__ == '__main__':
    unpack(sys.argv[1], sys.argv[2])

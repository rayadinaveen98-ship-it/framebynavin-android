#!/usr/bin/env python3
"""Apply the RC2.1 locking and emulator regression patch to the exact RC2 source."""
import base64
import hashlib
import lzma
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / 'hardening/rc2'
XZ_SHA = '5e5c6a9390958e224352041ad2a0bceae146e9566907215fd3c1f33833c2cd30'
RAW_SHA = '0557fd6c52a950b712d0ed8eb80c431faf4dd781f9d50ebe98e965cb9f07fb11'

def verified(data, expected, label):
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f'{label} SHA256 mismatch: {actual}')
    print(f'{label}: {actual}')
    return data

compressed = verified(base64.b64decode((PARTS / 'locking.patch.xz.b64').read_text().strip(), validate=True), XZ_SHA, 'Locking patch XZ')
patch = verified(lzma.decompress(compressed), RAW_SHA, 'Locking patch')
path = PARTS / 'locking.patch'
path.write_bytes(patch)
subprocess.run(['git', 'apply', '--check', str(path)], cwd=ROOT, check=True)
subprocess.run(['git', 'apply', str(path)], cwd=ROOT, check=True)
for name in [
    'app/src/test/java/com/framebynavin/app/data/CreatorDataGateV181Test.kt',
    'app/src/androidTest/java/com/framebynavin/app/data/HardeningTestEnvironment.kt',
]:
    if not (ROOT / name).is_file():
        raise RuntimeError('Missing locking regression test: ' + name)
source = (ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorDataGate.kt').read_text()
if 'marker.owner = currentCoroutineContext()[Job]' not in source or 'mutex.holdsLock(inherited.state.token)' not in source:
    raise RuntimeError('Coroutine lock ownership fix is missing')
print('RC2.1 locking and instrumentation patch applied')

from pathlib import Path
import base64
import gzip
import hashlib

ROOT = Path(__file__).resolve().parent.parent
CHUNKS = [
    ('v18_identity_personalization_alpha23_payload_01.txt', '9e5f3761c510519b4b9a783339ea55be60e91293fbd3ef7e9118f1246eed70b1'),
    ('v18_identity_personalization_alpha23_payload_02.txt', '7d2ea26f5ad6c55d4a0e6a2e4ffc8f60dc86e6e904fc953f8d9a2e9f62c9a0a1'),
    ('v18_identity_personalization_alpha23_payload_03.txt', '2b5cc76964ee3e3f1b072279567d3bb686a6d68194c76303689a226f66f8d4e2'),
    ('v18_identity_personalization_alpha23_payload_04.txt', 'd35c3b5b3bce90a95643f4061e291b546ca570b3df8cfd6b35c612f0e0e13ad0'),
    ('v18_identity_personalization_alpha23_payload_05.txt', 'c2f78ed540aaf668a28dbcdd81b72a3933c558420b792e784118d7abfc8a8277'),
    ('v18_identity_personalization_alpha23_payload_06.txt', 'a3847c61463dcd5c445ffeb14765eebc45aff98e52410bccce969eea07382797'),
    ('v18_identity_personalization_alpha23_payload_07.txt', 'c08d89a0761b210b4ae044b8bfe6f4987e692e6e8c90e317bad7065c861cfc32'),
    ('v18_identity_personalization_alpha23_payload_prefix08_7000.txt', '40a4516c4715018d9a193b2090e257f5ae70edcd098b858e329f3213dbcb0dd9'),
    ('v18_identity_personalization_alpha23_payload_03_01.txt', 'a9ade65b9aa966c2df4e2a2253fcbd3dfb8d50834f250f8119b8610115cd8f26'),
    ('v18_identity_personalization_alpha23_payload_03_02.txt', '58ecd0a2ce7e52e1e0e3834359014bb4534cffe40063d8ee2714adbf4c2e7bcb'),
    ('v18_identity_personalization_alpha23_payload_03_03.txt', 'b23c74654e7d37af2e178655079c9e3cdf44cc6b7221dbf780299b48bf70bbb5'),
    ('v18_identity_personalization_alpha23_payload_03_04.txt', '4b74046f3dab3111545b8cf372185adcb8bfa90ed7dc74ec9b2e51ce2c2186e6'),
    ('v18_identity_personalization_alpha23_payload_03_05.txt', '2909abccbfa54e09ca1fa17b07eaa9c29874cd93e7319ca93d0ae0683553f25c'),
    ('v18_identity_personalization_alpha23_payload_03_06_correct.txt', '451b017a3fa3cc73bd629301e98f04ff7324d3ae188405865b249c8e8a47b4f3'),
    ('v18_identity_personalization_alpha23_payload_03_08.txt', '5ce6618e8e14ce4553389c55d610e51614bdcac02761293575b1e90ba82c52ad'),
    ('v18_identity_personalization_alpha23_payload_03_07.txt', 'fb7eba72eccb43275563eb0f800595fea8042b610ba1c8a181b316e422992538'),
    ('v18_identity_personalization_alpha23_payload_03_09.txt', '6cc2406701dacccd0bec51cffe97d5384413f8afbdd1164673970a4ac418b3cf'),
]
EXPECTED_SCRIPT_SHA = 'd8e0439001c1684145ddbad1fbfcfaab8aadcef28dbfb3057c368665d2c7ce11'

parts = []
for name, expected in CHUNKS:
    path = ROOT / 'tools' / name
    text = path.read_text().strip()
    actual = hashlib.sha256(text.encode()).hexdigest()
    if actual != expected:
        raise SystemExit(f'Alpha23 payload chunk checksum mismatch: {name}: {actual} != {expected}')
    parts.append(text)

encoded = ''.join(parts)
try:
    script = gzip.decompress(base64.b64decode(encoded)).decode('utf-8')
except Exception as exc:
    raise SystemExit(f'Alpha23 payload decode failed: {exc}') from exc

actual_script_sha = hashlib.sha256(script.encode()).hexdigest()
if actual_script_sha != EXPECTED_SCRIPT_SHA:
    raise SystemExit(f'Alpha23 decoded script checksum mismatch: {actual_script_sha} != {EXPECTED_SCRIPT_SHA}')

exec(compile(script, '<alpha23-payload>', 'exec'), {'__name__': '__main__'})

compat_path = ROOT / 'tools' / 'v18_identity_personalization_alpha23_test_fix.py'
exec(compile(compat_path.read_text(), str(compat_path), 'exec'), {'__name__': '__main__'})

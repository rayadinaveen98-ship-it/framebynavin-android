from pathlib import Path
import base64
import gzip
import hashlib

ROOT = Path(__file__).resolve().parent.parent
CHUNKS = [
    ('v18_identity_personalization_alpha23_payload_01.txt', '50935482a85889b6d29dab9937a0185675654f82e155dd72eba7c58e33a4da84'),
    ('v18_identity_personalization_alpha23_payload_02.txt', '294323293cc05c45dad31ebcdf94bb5b631a8a2b5f1ae4e016aecb64c885cffa'),
    ('v18_identity_personalization_alpha23_payload_03_01.txt', 'a9ade65b9aa966c2df4e2a2253fcbd3dfb8d50834f250f8119b8610115cd8f26'),
    ('v18_identity_personalization_alpha23_payload_03_02.txt', '58ecd0a2ce7e52e1e0e3834359014bb4534cffe40063d8ee2714adbf4c2e7bcb'),
    ('v18_identity_personalization_alpha23_payload_03_03.txt', 'b23c74654e7d37af2e178655079c9e3cdf44cc6b7221dbf780299b48bf70bbb5'),
    ('v18_identity_personalization_alpha23_payload_03_04.txt', '4b74046f3dab3111545b8cf372185adcb8bfa90ed7dc74ec9b2e51ce2c2186e6'),
    ('v18_identity_personalization_alpha23_payload_03_05.txt', '2909abccbfa54e09ca1fa17b07eaa9c29874cd93e7319ca93d0ae0683553f25c'),
    ('v18_identity_personalization_alpha23_payload_03_06.txt', '451b017a3fa3cc73bd629301e98f04ff7324d3ae188405865b249c8e8a47b4f3'),
    # The final transport pair was uploaded under swapped filenames. Ordering here
    # is deliberate; hashes + decoded script SHA are the source of truth.
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

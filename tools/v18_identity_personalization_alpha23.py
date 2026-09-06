from pathlib import Path
import base64
import gzip
import hashlib

ROOT = Path(__file__).resolve().parent.parent
CHUNKS = [('v18_identity_personalization_alpha23_payload_01.txt', '50935482a85889b6d29dab9937a0185675654f82e155dd72eba7c58e33a4da84'), ('v18_identity_personalization_alpha23_payload_02.txt', '294323293cc05c45dad31ebcdf94bb5b631a8a2b5f1ae4e016aecb64c885cffa'), ('v18_identity_personalization_alpha23_payload_03.txt', 'b01f99229716bca30c1e8a6762b3bf4a6a55aec2f47afc36684026916dc2a97c'), ('v18_identity_personalization_alpha23_payload_04.txt', 'd7599ac1e06b98ec9f4f02af30bb884936f8792d5b157dbf672d3fbb9e47cf42')]
EXPECTED_SCRIPT_SHA = 'd8e0439001c1684145ddbad1fbfcfaab8aadcef28dbfb3057c368665d2c7ce11'

parts = []
for name, expected in CHUNKS:
    path = ROOT / "tools" / name
    text = path.read_text().strip()
    actual = hashlib.sha256(text.encode()).hexdigest()
    if actual != expected:
        raise SystemExit(f"Alpha23 payload chunk checksum mismatch: {name}: {actual} != {expected}")
    parts.append(text)
encoded = "".join(parts)
try:
    script = gzip.decompress(base64.b64decode(encoded)).decode("utf-8")
except Exception as exc:
    raise SystemExit(f"Alpha23 payload decode failed: {exc}") from exc
actual_script_sha = hashlib.sha256(script.encode()).hexdigest()
if actual_script_sha != EXPECTED_SCRIPT_SHA:
    raise SystemExit(f"Alpha23 decoded script checksum mismatch: {actual_script_sha} != {EXPECTED_SCRIPT_SHA}")
exec(compile(script, "<alpha23-payload>", "exec"), {"__name__": "__main__"})

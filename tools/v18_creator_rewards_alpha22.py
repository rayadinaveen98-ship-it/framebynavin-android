from pathlib import Path
import base64
import gzip
import hashlib

ROOT = Path(__file__).resolve().parent.parent
CHUNKS = [
    ("v18_creator_rewards_alpha22_payload_01.txt", "14bd55dfd055b98d734e70eb7f798a97f9927c0adeed67e76e0fa3521b542538"),
    ("v18_creator_rewards_alpha22_payload_02.txt", "cd79e1bdebbe7bd2f16ddd4356e505062558498770a6720175603a45c4218a6e"),
    ("v18_creator_rewards_alpha22_payload_03.txt", "a0af1703d3658d3ee21316251dafbcd7693af5335a4837f992438d0966124791"),
    ("v18_creator_rewards_alpha22_payload_04.txt", "aaa49eec57de23d612a92c8156643cdd9b3ada2147ddfb6827ab7edcefd37c20"),
]
EXPECTED_SCRIPT_SHA = "dd70f070fba22e24d84ffb006e38fb2ea015c5aab464ea384a22a1b915c73a2b"

parts = []
for name, expected in CHUNKS:
    path = ROOT / "tools" / name
    text = path.read_text().strip()
    actual = hashlib.sha256(text.encode()).hexdigest()
    if actual != expected:
        raise SystemExit(f"Alpha22 payload chunk checksum mismatch: {name}: {actual} != {expected}")
    parts.append(text)

encoded = "".join(parts)
try:
    script = gzip.decompress(base64.b64decode(encoded)).decode("utf-8")
except Exception as exc:
    raise SystemExit(f"Alpha22 payload decode failed: {exc}") from exc

actual_script_sha = hashlib.sha256(script.encode()).hexdigest()
if actual_script_sha != EXPECTED_SCRIPT_SHA:
    raise SystemExit(f"Alpha22 decoded script checksum mismatch: {actual_script_sha} != {EXPECTED_SCRIPT_SHA}")

exec(compile(script, "<alpha22-payload>", "exec"), {"__name__": "__main__"})

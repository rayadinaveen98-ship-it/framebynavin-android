from pathlib import Path
import base64
import hashlib
import json
import zlib

SOURCES = [
    ("tools/v18_post_publish_rewards_alpha21_payload_1.txt", "764acf1b57976ea3b439c47e2a4c3cfdbade419e4e52696b3340aa81c88a673b"),
    ("tools/v18_post_publish_rewards_alpha21_payload_2.txt", "33ff413f0d4f23e775f36ba1b528acc320f9bd1c5f93293d7efac99ad5c52fd5"),
    ("tools/v18_post_publish_rewards_alpha21_payload_3.txt", "45f7be93fd827102655545a04f90d8142c36c55184941fadb1e75d44e38a3461"),
    ("tools/v18_post_publish_rewards_alpha21_payload_4.txt", "f43898b4170e96183254803d942baa49436b2b59c3752ae6a49590c1cdb81d18"),
    ("tools/v18_post_publish_rewards_alpha21_payload_v3_09.txt", "c942628841c49d865e60258cf18d7db61ca05f24c539a6f48bccbfc28a9aa0cc"),
    ("tools/v18_post_publish_rewards_alpha21_payload_v3_10.txt", "d4a31e5bd235d246d6af6a896652a44d697d45e06949572d3201aedbf60f21dc"),
    ("tools/v18_post_publish_rewards_alpha21_payload_6.txt", "837757f1d6adbff110d6e538c2a1893e4e71e17442b6596eb7511cced6ca4ec4"),
]
EXPECTED_JOINED_SHA256 = "21b50270a2216fffecaf4287696e0a7df10ec29270c3e89a1922cd4dee7cad78"
EXPECTED_RAW_SHA256 = "fd58c881a4e3077f86257bc8b889a0597150d5990548098cdb698fb259da4ffe"

parts = []
for rel, expected in SOURCES:
    text = Path(rel).read_text().strip()
    actual = hashlib.sha256(text.encode("utf-8")).hexdigest()
    if actual != expected:
        raise RuntimeError(f"Alpha21 payload integrity failure for {rel}: {actual} != {expected}")
    parts.append(text)

joined = "".join(parts)
joined_sha = hashlib.sha256(joined.encode("utf-8")).hexdigest()
if joined_sha != EXPECTED_JOINED_SHA256:
    raise RuntimeError(f"Alpha21 joined payload integrity failure: {joined_sha}")

raw = zlib.decompress(base64.b64decode(joined, validate=True))
raw_sha = hashlib.sha256(raw).hexdigest()
if raw_sha != EXPECTED_RAW_SHA256:
    raise RuntimeError(f"Alpha21 decoded payload integrity failure: {raw_sha}")

payload = json.loads(raw.decode("utf-8"))
for rel, content in payload.items():
    path = Path(rel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)

print(f"Applied Alpha21 post-publish + reward foundation to {len(payload)} files with verified payload integrity")

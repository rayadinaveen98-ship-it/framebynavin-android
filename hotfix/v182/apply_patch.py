#!/usr/bin/env python3
"""Apply the complete v1.8.2 source delta to the verified v1.8.1 RC1 tree."""
import base64
import hashlib
import lzma
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / "hotfix" / "v182"
COMPRESSED_SHA256 = "86058c8f0c467617b2e8e8edf93d6cc13e0a0c9547ac0aa720064341ecba313a"
PATCH_SHA256 = "ad815c059dfa9e385feca94d7c84806fbf12a1c854d4081d30339dec74fa548b"


def verified(data: bytes, expected: str, label: str) -> bytes:
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f"{label} SHA256 mismatch: {actual}")
    print(f"{label}: {actual}")
    return data


def main():
    encoded = (PARTS / "reminder-safety.patch.xz.b64").read_text().strip()
    compressed = verified(base64.b64decode(encoded, validate=True), COMPRESSED_SHA256, "v1.8.2 patch XZ")
    patch = verified(lzma.decompress(compressed), PATCH_SHA256, "v1.8.2 source patch")
    patch_path = PARTS / "reminder-safety.patch"
    patch_path.write_bytes(patch)
    subprocess.run(["git", "apply", "--check", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "apply", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "diff", "--check"], cwd=ROOT, check=True)
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    if 'versionCode = 65' not in gradle or 'versionName = "1.8.2-reminder-safety"' not in gradle:
        raise RuntimeError("v1.8.2 version gate failed")
    subprocess.run(["git", "diff", "--stat"], cwd=ROOT, check=True)
    print("Complete v1.8.2 reminder-safety patch applied cleanly")


if __name__ == "__main__":
    main()

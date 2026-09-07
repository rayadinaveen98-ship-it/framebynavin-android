#!/usr/bin/env python3
"""Verify and apply the v1.8.2 delta to the reconstructed v1.8.1 RC1 source."""
import argparse
import base64
import hashlib
import lzma
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / "hotfix" / "v182"
CHUNK_SHAS = (
    "54f500373394618b7b51ae5119b216af016053c63f5150ea4eafc71467e3a2da",
    "1eaff81a3d264b458d3aee4f11e1f822256e2527e318592db02f3faabacda37b",
    "769ddf0d51f4487fcdd43fb3b649cd496ce9b3e820a30b07d5e870a1df2ed629",
    "4197f5e1cffabe189b4c2ac5b7c3010c166ec4337febf40bf586c3f2a63933c8",
)
COMPRESSED_SHA256 = "86058c8f0c467617b2e8e8edf93d6cc13e0a0c9547ac0aa720064341ecba313a"
PATCH_SHA256 = "ad815c059dfa9e385feca94d7c84806fbf12a1c854d4081d30339dec74fa548b"


def verified(data: bytes, expected: str, label: str) -> bytes:
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f"{label} SHA256 mismatch: {actual}")
    print(f"{label}: {actual}")
    return data


def load_patch() -> bytes:
    chunks = []
    for index, expected in enumerate(CHUNK_SHAS, 1):
        text = (PARTS / f"payload-{index:02d}.b64").read_text(encoding="ascii").strip()
        if len(text) != 4144:
            raise RuntimeError(f"Payload chunk {index} has invalid length: {len(text)}")
        verified(text.encode("ascii"), expected, f"Payload chunk {index}")
        chunks.append(base64.b64decode(text, validate=True))
    compressed = verified(b"".join(chunks), COMPRESSED_SHA256, "v1.8.2 patch XZ")
    if len(compressed) != 12432:
        raise RuntimeError("Unexpected compressed payload size")
    return verified(lzma.decompress(compressed), PATCH_SHA256, "v1.8.2 source patch")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify-payload", action="store_true")
    args = parser.parse_args()
    patch = load_patch()
    if args.verify_payload:
        print("Complete reminder-safety payload verified")
        return
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

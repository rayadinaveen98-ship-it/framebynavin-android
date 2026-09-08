#!/usr/bin/env python3
"""One-time, hash-verified v1.8.3 backup-safety source materialization.

The exact source is committed after successful CI; this transport is not a
runtime dependency or a replacement for the canonical production source.
"""
import argparse
import base64
import hashlib
import lzma
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / "foundation" / "v183"
CHUNKS = (
    ("01", 1840, "14ac1372948a8ff36a8e5f2e80b6e6e5308b5c2d4ae9f969e1df84c05e6c96ab"),
    ("02", 1840, "38ab4c7c0bc91e44eadb3ce95579567592e6bdabb81ee3632b254b190e233168"),
    ("03a", 460, "2f94244e18112beeeb3eff56f13045c3aac5dfd5dc557f0a27b05018b9f50fb1"),
    ("03b", 460, "8f6f6c77480ffb216b7cc69400e76028179140061165d5c532584c50e11fe2b0"),
    ("03c", 460, "909c3b55715c76cc6e59106de04c1511874a660259a259765f4e041705bef719"),
    ("03d", 460, "5608aec8bed6ae403ed61ec7bfefea1b317cc9ed359478dbedf7060968fa07ad"),
    ("04", 1840, "e66ff0573c1edaf49a155a1626ea00f7dca51715b4057b54cd6719a4001223be"),
)
COMPRESSED_SHA = "fa52b06e10c612a6315909ef04bc8e4ba13a7acd7a791e88b4316d7086b7bfaf"
PATCH_SHA = "0f2feb64ebae6ac90f515a2fe219f1a66b9a0cafcd8a59d6051f0197358e1a42"
EXPECTED_BASE = "d533d5d28d8808b36e174b956104ef123bc6cd64"


def verified(data: bytes, expected: str, label: str) -> bytes:
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f"{label} SHA-256 mismatch: {actual}")
    print(f"{label}: {actual}")
    return data


def load_patch() -> bytes:
    chunks = []
    for name, length, expected in CHUNKS:
        text = (PARTS / f"backup-payload-{name}.b64").read_text(encoding="ascii").strip()
        if len(text) != length:
            raise RuntimeError(f"Invalid payload segment {name} length: {len(text)}")
        chunks.append(verified(text.encode("ascii"), expected, f"Segment {name}").decode("ascii"))
    encoded = "".join(chunks)
    compressed = verified(base64.b64decode(encoded, validate=True), COMPRESSED_SHA, "Compressed backup patch")
    if len(compressed) != 5520:
        raise RuntimeError("Unexpected compressed patch size")
    return verified(lzma.decompress(compressed), PATCH_SHA, "Backup source patch")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify-payload", action="store_true")
    args = parser.parse_args()
    patch = load_patch()
    if args.verify_payload:
        print("Complete backup-safety payload verified")
        return
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip() not in (EXPECTED_BASE,):
        raise RuntimeError("Unexpected source baseline; refusing to apply backup patch")
    patch_path = PARTS / "backup-safety.patch"
    patch_path.write_bytes(patch)
    subprocess.run(["git", "apply", "--check", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "apply", str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(["git", "diff", "--check"], cwd=ROOT, check=True)
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    if 'versionCode = 66' not in gradle or 'versionName = "1.8.3-foundation-rc2"' not in gradle:
        raise RuntimeError("RC2 version gate failed")
    source = (ROOT / "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt").read_text()
    if 'const val SCHEMA_VERSION = 5' not in source:
        raise RuntimeError("Backup schema gate failed")
    print("Reviewed backup-safety source update applied cleanly")


if __name__ == "__main__":
    main()

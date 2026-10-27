#!/usr/bin/env python3
"""Backlot V217 artwork gate. Stdlib-only; does not mutate images.

Concept mode verifies the three independent transparent review images and pins
all five ORIGINAL Spark art blobs. Final mode additionally requires nine
independent transparent expression assets per NEW guide. Do not ship an
all-states-use-welcome fallback in place of distinct premium artwork.
"""
import argparse
import hashlib
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res/drawable-nodpi"

# Git blob object IDs of the original approved Spark resources, verified from
# design/backlot-v217-companion-art-direction before the V217 implementation.
SPARK = {
    "welcome": "413866c546bd70b38d0a47861270492ad8187bc3",
    "point": "afa28b77a653d85e218cff23d175d82f615ba708",
    "present": "f13283b4b0c72e15ffe9e3220da38da1c09c16da",
    "thinking": "327871f41cbbd3d4c5100bc973e7f1f35bef3832",
    "celebrate": "4504a832337ae1e28780de8018dfc60efdcc41dc",
}
GUIDES = ("frame", "navi", "cute")
REQUIRED_POSES = (
    "idle", "listen", "think", "present", "point",
    "success", "celebrate", "rest", "walk",
)


def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\\0" + data).hexdigest()


def load_webp(path: Path):
    if not path.is_file():
        raise ValueError(f"Missing: {path.relative_to(ROOT)}")
    data = path.read_bytes()
    if not (data.startswith(b"RIFF") and data[8:12] == b"WEBP"):
        raise ValueError(f"Not RIFF/WebP: {path.name}")
    if len(data) < 30 or data[12:16] != b"VP8X":
        raise ValueError(f"Extended lossless alpha WebP required: {path.name}")
    if not data[20] & 0x10:
        raise ValueError(f"Asset is not transparent: {path.name}")
    width = int.from_bytes(data[24:27], "little") + 1
    height = int.from_bytes(data[27:30], "little") + 1
    if not (128 <= width <= 1600 and 128 <= height <= 1600):
        raise ValueError(f"Invalid dimensions {width}x{height}: {path.name}")
    if not 1500 <= len(data) <= 350000:
        raise ValueError(f"Unreasonable compressed size: {path.name} ({len(data)})")
    return data, width, height


def verify_spark():
    for pose, expected in SPARK.items():
        name = f"guide_funny_{pose}.webp"
        data = (RES / name).read_bytes()
        actual = git_blob_sha(data)
        if actual != expected:
            raise ValueError(f"SPARK PRESERVATION FAILED: {name} {actual} != {expected}")
    print("PASS: all five real Spark image blobs unchanged")


def verify_concepts():
    digests = []
    for guide in GUIDES:
        filename = f"guide_v217_{guide}_concept.webp"
        data, width, height = load_webp(RES / filename)
        digests.append(git_blob_sha(data))
        print(f"PASS: {filename} {width}x{height}, {len(data)} bytes, alpha")
    if len(set(digests)) != len(GUIDES):
        raise ValueError("Multiple guide concept images have identical content")


def verify_final_pose_pack():
    all_images = set()
    for guide in GUIDES:
        unique = set()
        dims = []
        for pose in REQUIRED_POSES:
            filename = f"guide_v217_{guide}_{pose}.webp"
            data, width, height = load_webp(RES / filename)
            digest = git_blob_sha(data)
            if digest in unique:
                raise ValueError(f"{guide} reuses identical artwork for multiple required poses")
            unique.add(digest)
            all_images.add(digest)
            dims.append((width, height))
        if len(set(dims)) != 1:
            raise ValueError(f"Unmatched atlas bounds for {guide}: {dims}")
        print(f"PASS: {guide}: {len(REQUIRED_POSES)} distinct, consistent alpha poses")
    if len(all_images) != len(GUIDES) * len(REQUIRED_POSES):
        raise ValueError("Identical pose artwork appears across different identities")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("concept", "production"), default="concept")
    args = parser.parse_args()
    try:
        verify_spark()
        verify_concepts()
        if args.mode == "production":
            verify_final_pose_pack()
        else:
            print("CONCEPT-ONLY: no production pose approval claimed")
    except (ValueError, OSError) as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

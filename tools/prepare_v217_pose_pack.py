#!/usr/bin/env python3
"""Normalize independently drawn V217 transparent poses into review-only WebPs.

Requires Pillow on an artist machine: `python -m pip install Pillow`.
Input: <input_dir>/<guide>/<pose>.png (or .webp) for all nine poses.
Output defaults to `artwork/v217/staging` and NEVER writes app resources.

A composite UI contact sheet or nontransparent render is NOT a valid input.
Run `check_v217_companion_assets.py --mode production` separately only after
visual acceptance and an explicit copy of reviewed assets to drawable-nodpi.
"""
from argparse import ArgumentParser
import hashlib
from pathlib import Path
import sys

GUIDES = ("frame", "navi", "cute")
POSES = ("idle", "listen", "think", "present", "point",
         "success", "celebrate", "rest", "walk")
TARGET = (740, 760)


def load_pillow():
    try:
        from PIL import Image
        return Image
    except ImportError as exc:
        raise RuntimeError("Install Pillow only for artist-side processing") from exc


def get_source(folder, pose):
    for extension in (".png", ".webp"):
        p = folder / (pose + extension)
        if p.is_file():
            return p
    raise ValueError(f"Missing independent illustration: {folder}/{pose}.png|webp")


def process(guide, inputs, output, Image):
    source_dir = inputs / guide
    source_fingerprints = set()
    result_fingerprints = set()
    images = []
    for pose in POSES:
        p = get_source(source_dir, pose)
        source_bytes = p.read_bytes()
        source_hash = hashlib.sha256(source_bytes).hexdigest()
        if source_hash in source_fingerprints:
            raise ValueError(f"{guide}/{pose}: duplicated source illustration")
        source_fingerprints.add(source_hash)
        with Image.open(p) as original:
            if original.mode not in ("RGBA", "LA", "P"):
                raise ValueError(f"{guide}/{pose}: missing transparent image mode")
            rgba = original.convert("RGBA")
            alpha = rgba.getchannel("A")
            lo, hi = alpha.getextrema()
            if lo != 0 or hi != 255:
                raise ValueError(f"{guide}/{pose}: need both transparent and opaque pixels")
            bbox = alpha.getbbox()
            if not bbox:
                raise ValueError(f"{guide}/{pose}: completely transparent input")
            x0, y0, x1, y1 = bbox
            if min(x0, y0, rgba.width - x1, rgba.height - y1) < 2:
                raise ValueError(f"{guide}/{pose}: character clips input canvas edge")
            rgba = rgba.crop(bbox)
            rgba.thumbnail((int(TARGET[0] * .84), int(TARGET[1] * .88)), Image.Resampling.LANCZOS)
            stage = Image.new("RGBA", TARGET, (0, 0, 0, 0))
            x = (TARGET[0] - rgba.width) // 2
            baseline_y = int(TARGET[1] * .94)
            stage.alpha_composite(rgba, (x, baseline_y - rgba.height))
            images.append((pose, stage))
    output.mkdir(parents=True, exist_ok=True)
    for pose, stage in images:
        p = output / f"guide_v217_{guide}_{pose}.webp"
        if p.exists():
            raise ValueError(f"Will not overwrite existing reviewed output: {p}")
        stage.save(p, format="WEBP", quality=92, method=6, exact=True)
        blob = p.read_bytes()
        fingerprint = hashlib.sha256(blob).hexdigest()
        if fingerprint in result_fingerprints:
            raise ValueError(f"{guide}/{pose}: visually redundant output file")
        result_fingerprints.add(fingerprint)
    return len(images)


def main():
    parser = ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("artwork/v217/staging"))
    parser.add_argument("--guide", choices=GUIDES + ("all",), default="all")
    args = parser.parse_args()
    guide_set = GUIDES if args.guide == "all" else (args.guide,)
    output = args.output.resolve()
    forbidden = (Path(__file__).resolve().parents[1] / "app/src/main/res").resolve()
    if output == forbidden or forbidden in output.parents:
        parser.error("Safety gate: this tool never writes Android resources directly")
    try:
        Image = load_pillow()
        for guide in guide_set:
            count = process(guide, args.input, output, Image)
            print(f"STAGED: {guide}, {count} independently authored transparent poses")
    except (ValueError, RuntimeError, OSError) as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""V222: richer color in the EXACT approved Backlot WebP, no design redraw.

The real launcher foreground (also Settings preview and all launcher aliases)
is res/drawable-nodpi/backlot_v198_approved_icon.webp. This one-time edit
preserves dimensions, every source pixel location, and the original dark
background while increasing the saturation/value only on lit logo pixels.
Designed for a single audited GitHub Actions run, not a build-time dependency.
"""
from __future__ import annotations

import colorsys
from hashlib import sha256
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "app/src/main/res/drawable-nodpi/backlot_v198_approved_icon.webp"
EXPECTED_ORIGINAL_SHA256 = "10c94a38783a18237264eeb5108b7e71d1556671864f12f04288d0b5309a4cbc"


def main() -> None:
    original = PATH.read_bytes()
    digest = sha256(original).hexdigest()
    if digest != EXPECTED_ORIGINAL_SHA256:
        raise SystemExit(
            f"Refusing to recolor an unapproved or previously changed logo: {digest}"
        )
    image = Image.open(PATH).convert("RGB")
    if image.size != (256, 256):
        raise SystemExit(f"Approved icon dimensions changed: {image.size}")
    improved = []
    for red, green, blue in image.getdata():
        r, g, b = red / 255, green / 255, blue / 255
        h, s, v = colorsys.rgb_to_hsv(r, g, b)
        alpha = max(0.0, min(1.0, (max(r, g, b) - 0.18) / 0.34))
        alpha = alpha * alpha * (3.0 - 2.0 * alpha)
        h2, s2, v2 = h, min(1.0, s * (1.0 + .23 * alpha)), min(1.0, v * (1.0 + .065 * alpha))
        nr, ng, nb = colorsys.hsv_to_rgb(h2, s2, v2)
        improved.append(tuple(round(255 * (old * (1-alpha) + new * alpha))
                              for old, new in zip((r, g, b), (nr, ng, nb))))
    output = Image.new("RGB", image.size)
    output.putdata(improved)
    output.save(PATH, "WEBP", quality=96, method=6)
    verify = Image.open(PATH)
    assert verify.size == image.size
    assert image.getpixel((0, 0)) == verify.getpixel((0, 0)), "Logo background should stay unchanged"
    print(f"Original approved B silhouette preserved; new WebP SHA-256 {sha256(PATH.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()

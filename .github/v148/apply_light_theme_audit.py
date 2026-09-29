from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

REPLACEMENTS = {
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt": [
        (
            "Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), Color(0xFF15110F), border = BorderStroke(1.dp, Color(0xFF3B2521)))",
            "Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), BacklotSurfaceSelected, border = BorderStroke(1.dp, RecRed.copy(alpha = .24f)))",
        ),
        (
            "if (autoPlanEnabled) Color(0xFF17130F) else CinemaSurface",
            "if (autoPlanEnabled) BacklotSurfaceSelected else CinemaSurface",
        ),
        (
            "if (VisualExperiencePrefs.isLumen) CinemaSurface.copy(alpha = .88f) else Color(0xF2161618)",
            "BacklotNavigationSurface.copy(alpha = .96f)",
        ),
        (
            "done || i < current -> SuccessGreen.copy(alpha = .75f); i == current -> RecRed; else -> Color(0xFF34312E)",
            "done || i < current -> SuccessGreen.copy(alpha = .75f); i == current -> RecRed; else -> CinemaLine",
        ),
        (
            "background(if (i == index) RecRed else Color(0xFF44413D), RoundedCornerShape(100.dp))",
            "background(if (i == index) RecRed else MutedText.copy(alpha = .42f), RoundedCornerShape(100.dp))",
        ),
        (
            "Box(Modifier.size(34.dp).background(Color(0xFF1F1F21), RoundedCornerShape(11.dp))",
            "Box(Modifier.size(34.dp).background(CinemaSurfaceRaised, RoundedCornerShape(11.dp))",
        ),
    ],
    "app/build.gradle.kts": [
        ("// Backlot V147 idea reminders release candidate.\n        versionCode = 147\n        versionName = \"2.0.0-rc22-device-stability\"",
         "// Backlot V148 dark/light appearance release candidate.\n        versionCode = 148\n        versionName = \"2.0.0-rc23-light-appearance\""),
    ],
}

RETIRED_WIDGET_BACKGROUNDS = [
    "app/src/main/res/drawable/widget_bg_studio_gold.xml",
    "app/src/main/res/drawable/widget_bg_midnight.xml",
    "app/src/main/res/drawable/widget_bg_lumen.xml",
    "app/src/main/res/drawable/widget_bg_paper.xml",
    "app/src/main/res/drawable/widget_bg_moss.xml",
    "app/src/main/res/drawable/widget_bg_ember.xml",
    "app/src/main/res/drawable/widget_bg_violet.xml",
    "app/src/main/res/drawable/widget_bg_aurora.xml",
    "app/src/main/res/drawable/widget_bg_storyboard.xml",
]

# Files where raw colors are intentionally drawing artwork/media rather than app surfaces.
AUDIT_EXCLUSIONS = {
    "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt",
    "app/src/main/java/com/framebynavin/app/ui/V133BacklotAppIcon.kt",
    "app/src/main/java/com/framebynavin/app/ui/V140CinematicWelcome.kt",
    "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt",
    "app/src/main/java/com/framebynavin/app/ui/WelcomeSonicIdent.kt",
    "app/src/main/java/com/framebynavin/app/ui/V137CharacterSystem.kt",
    "app/src/main/java/com/framebynavin/app/ui/V144RasterGuideCharacter.kt",
}

DARK_LITERAL = re.compile(r"Color\((?:0x(?:FF|F[0-9A-Fa-f])[0-2][0-9A-Fa-f]{5})\)|Color\.Black")
SURFACE_HINTS = (
    "Surface(",
    ".background(",
    "containerColor",
    "scrimColor",
    "trackColor",
    "indicatorColor",
)


def apply_replacements() -> None:
    for rel, replacements in REPLACEMENTS.items():
        path = ROOT / rel
        text = path.read_text(encoding="utf-8")
        for old, new in replacements:
            if old not in text:
                raise SystemExit(f"Expected V148 patch anchor not found in {rel}: {old[:100]!r}")
            text = text.replace(old, new)
        path.write_text(text, encoding="utf-8")

    for rel in RETIRED_WIDGET_BACKGROUNDS:
        path = ROOT / rel
        if path.exists():
            path.unlink()


def audit() -> list[str]:
    findings: list[str] = []
    roots = [
        ROOT / "app/src/main/java/com/framebynavin/app/ui",
        ROOT / "app/src/main/java/com/framebynavin/app/widget",
    ]
    for source_root in roots:
        for path in sorted(source_root.rglob("*.kt")):
            rel = path.relative_to(ROOT).as_posix()
            if rel in AUDIT_EXCLUSIONS:
                continue
            for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                if DARK_LITERAL.search(line) and any(hint in line for hint in SURFACE_HINTS):
                    findings.append(f"{rel}:{line_no}: {line.strip()}")
    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    if args.apply:
        apply_replacements()

    findings = audit()
    print("V148_THEME_AUDIT_BEGIN")
    if findings:
        for item in findings:
            print(item)
    else:
        print("No suspicious dark-only surface literals found outside the explicit artwork whitelist.")
    print("V148_THEME_AUDIT_END")
    return 1 if args.strict and findings else 0


if __name__ == "__main__":
    sys.exit(main())

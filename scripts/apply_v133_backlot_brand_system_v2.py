from pathlib import Path
import runpy

ROOT = Path(__file__).resolve().parents[1]

# Run the full brand migration first.
runpy.run_path(str(ROOT / "scripts/apply_v133_backlot_brand_system.py"), run_name="__main__")


def patch(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text and new in text:
        return
    if old not in text:
        raise RuntimeError(f"Could not protect compatibility value in {path}: {old}")
    target.write_text(text.replace(old, new), encoding="utf-8")


# These values are serialization/protocol identifiers, not brand copy. Changing them would
# make existing backups/cloud snapshots fail validation or need needless compatibility code.
patch(
    "app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt",
    'const val CLOUD_FORMAT = "BacklotCloudBackup"',
    'const val CLOUD_FORMAT = "FrameByNavinCloudBackup"',
)
patch(
    "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt",
    'const val FORMAT = "BacklotBackup"',
    'const val FORMAT = "FrameByNavinBackup"',
)
patch(
    "app/src/main/java/com/framebynavin/app/cloud/DriveVaultApi.kt",
    'val boundary = "Backlot-${UUID.randomUUID()}"',
    'val boundary = "FrameByNavin-${UUID.randomUUID()}"',
)

# This one is actual copy shown to a user, so it must use the new product name even though
# it lives next to the technical format check.
patch(
    "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt",
    '"This is not a FrameByNavin backup."',
    '"This is not a Backlot backup."',
)

# Final allowlist audit. Old brand is allowed only in frozen protocol/resource identifiers.
# Class names, package names and Theme.FrameByNavin remain source identifiers and never render
# as the product name to a user.
allowed = {
    ("app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt", '"FrameByNavinCloudBackup"'),
    ("app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt", '"FrameByNavinBackup"'),
    ("app/src/main/java/com/framebynavin/app/cloud/DriveVaultApi.kt", '"FrameByNavin-${UUID.randomUUID()}"'),
}
violations = []
remaining_quoted = []
for path in sorted((ROOT / "app/src/main").rglob("*")):
    if not path.is_file() or path.suffix not in {".kt", ".xml", ".java"}:
        continue
    rel = str(path.relative_to(ROOT))
    for line_no, line in enumerate(path.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
        if not any(old in line for old in ("FrameByNavin", "Frame by Navin", "FRAME BY NAVIN")):
            continue
        literals = []
        in_string = False
        escaped = False
        start = 0
        for idx, ch in enumerate(line):
            if escaped:
                escaped = False
                continue
            if ch == "\\" and in_string:
                escaped = True
                continue
            if ch == '"':
                if not in_string:
                    in_string = True
                    start = idx
                else:
                    literals.append(line[start:idx + 1])
                    in_string = False
        for literal in literals:
            if not any(old in literal for old in ("FrameByNavin", "Frame by Navin", "FRAME BY NAVIN")):
                continue
            remaining_quoted.append(f"{rel}:{line_no}: {literal}")
            is_theme_resource = "Theme.FrameByNavin" in literal
            if (rel, literal) not in allowed and not is_theme_resource:
                violations.append(f"{rel}:{line_no}: {literal}")

report = ROOT / "build/backlot-brand-audit.txt"
with report.open("a", encoding="utf-8") as fh:
    fh.write("\nFINAL COMPATIBILITY AUDIT\n")
    fh.write("=========================\n")
    fh.write(f"Allowed technical literals: {len(remaining_quoted)}\n")
    fh.write(f"Final user-facing violations: {len(violations)}\n")
    for item in remaining_quoted:
        fh.write(f"ALLOWED/REVIEWED: {item}\n")
    for item in violations:
        fh.write(f"VIOLATION: {item}\n")

if violations:
    raise RuntimeError("Old brand remains in non-allowlisted string literals:\n" + "\n".join(violations))

print("v133 compatibility pass complete")
print(f"technical literals preserved/reviewed: {len(remaining_quoted)}")

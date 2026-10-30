#!/usr/bin/env python3
"""V209 P1: non-functional release and repository safety contract.

This deliberately does not modify, untrack or delete anything. V209 P1 is a
documentation/repository-hygiene package, NOT a new Android build or release.
Run with --base <git ref> to assert a stacked branch changed no app sources.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ALLOWED = {
    ".gitignore",
    "README.md",
    "docs/release/CURRENT_STABLE.md",
    "docs/qa/V209_TRUST_RELEASE_HYGIENE_P1.md",
    "tools/check_v209_trust_hygiene.py",
    ".github/workflows/backlot-v209-trust-hygiene-p1.yml",
}
PROTOTYPE_CERT = "dev-signing/framebynavin-public-debug.jks"


def git(*args: str) -> str:
    return subprocess.check_output(
        ["git", *args], cwd=ROOT, text=True, stderr=subprocess.PIPE
    ).strip()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def file_text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", help="Existing git branch/ref to compare with HEAD")
    args = parser.parse_args()
    build = file_text("app/build.gradle.kts")
    manifest = file_text("app/src/main/AndroidManifest.xml")
    readme = file_text("README.md")
    stable = file_text("docs/release/CURRENT_STABLE.md")
    onboarding = file_text("app/src/main/java/com/framebynavin/app/MainActivity.kt")
    backup = file_text("app/src/main/java/com/framebynavin/app/data/PortableCreatorBackupManager.kt")

    # The existing personal-install identity MUST NOT change in this package.
    require('applicationId = "com.framebynavin.app"' in build, "Package identity changed")
    require(re.search(r"\bversionCode\s*=\s*232\b", build) is not None, "V208 build code changed")
    require('versionName = "2.8.2-v208-setup-backup-preview"' in build,
            "V209 P1 must not imply a newer app version")
    require('create("personalStable")' in build and
            'signingConfig = signingConfigs.getByName("prototypeStable")' in build,
            "Preview signer compatibility changed")
    require('android:label="Backlot"' in manifest, "Launcher brand changed")

    # A maintenance-only branch should preserve the verified V208 safeguards.
    require('V144GuidePrefs.initialize(applicationContext)' in onboarding,
            "Returning creators may lose their companion identity")
    require("PortableBackupArchive.stage(input, appContext.cacheDir)" in backup,
            "Full-archive backup validation was lost")
    require("verifyArchiveVoiceReferences(staged)" in backup,
            "Portable voice media reference check was lost")

    require(all(token in stable for token in (
                "V195.5", "stable/backlot-v195-5", "V183", "V208", "preview")),
            "Release ledger must identify V195.5 stable, historical V183 and V208 preview")
    require(all(token in readme for token in ("V195.5", "V208")) and
            "personal" in readme.lower(),
            "README must identify current stable and preview boundaries")

    # check-ignore works even on previously tracked files when --no-index is used.
    for path in (
        "app/build/intermediates/v209-check.tmp",
        "app/build/outputs/apk/debug/v209-check.apk",
        "some-module/build/generated/out.txt",
        ".gradle/caches/v209-check.bin",
        ".idea/workspace.xml",
        "local.properties",
        "test-private.jks",
        ".env.local",
    ):
        ignored = subprocess.run(
            ["git", "check-ignore", "--quiet", "--no-index", path],
            cwd=ROOT, check=False,
        ).returncode == 0
        require(ignored, f"New generated/secret file is not ignored: {path}")

    tracked = git("ls-files", "-z").split("\x00")
    tracked = [path for path in tracked if path]
    new_credentials = [
        path for path in tracked
        if path.lower().endswith((".jks", ".keystore", ".p12", ".pfx"))
        and path != PROTOTYPE_CERT
    ]
    require(not new_credentials,
            f"Unexpected tracked signing material: {new_credentials[:5]}")
    inherited_build_files = sum(path.startswith("app/build/") for path in tracked)
    print(f"KNOWN BACKLOG: {inherited_build_files} previously tracked app/build files; "
          "new additions ignored, old entries not removed or rewritten.")

    if args.base:
        changed = git("diff", "--name-only", f"{args.base}...HEAD").splitlines()
        outside = sorted(path for path in changed if path not in ALLOWED)
        require(not outside, f"V209 P1 includes unexpected changes: {outside}")
        print(f"Checked {len(changed)} allowed changes since {args.base}")

    print("PASS: V209 P1 release identity, backup/companion guard, "
          "ignore rules and non-functional scope")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, subprocess.CalledProcessError, OSError) as error:
        print(f"FAIL: V209 P1 contract: {error}", file=sys.stderr)
        raise SystemExit(1)

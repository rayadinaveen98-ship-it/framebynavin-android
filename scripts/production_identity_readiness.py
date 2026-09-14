#!/usr/bin/env python3
"""Static production identity/App Check readiness audit.

This intentionally does not claim remote Google/Firebase registration. It verifies the
repository-side contract and reports whether the downloaded google-services.json already
contains an Android OAuth client (client_type=1), which normally appears after a signing
SHA-1 is registered. Use --require-android-oauth-client only after final release
certificate registration and config refresh.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.framebynavin.app"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")
    print(f"PASS: {message}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--require-android-oauth-client",
        action="store_true",
        help="Fail unless google-services.json contains an Android OAuth client for the final signing certificate.",
    )
    args = parser.parse_args()

    google_path = ROOT / "app/google-services.json"
    cloud_path = ROOT / "app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt"
    gradle_path = ROOT / "app/build.gradle.kts"
    release_appcheck_path = ROOT / "app/src/release/java/com/framebynavin/app/CreatorAppCheck.kt"
    debug_appcheck_path = ROOT / "app/src/debug/java/com/framebynavin/app/CreatorAppCheck.kt"

    config = json.loads(google_path.read_text(encoding="utf-8"))
    clients = config.get("client", [])
    package_clients = [
        client
        for client in clients
        if client.get("client_info", {}).get("android_client_info", {}).get("package_name") == PACKAGE
    ]
    require(bool(package_clients), f"Firebase config contains Android package {PACKAGE}")

    oauth_clients = [oauth for client in package_clients for oauth in client.get("oauth_client", [])]
    web_client_ids = [oauth.get("client_id") for oauth in oauth_clients if oauth.get("client_type") == 3]
    require(bool(web_client_ids), "Firebase config contains a Web OAuth client for Google ID tokens")

    cloud_text = cloud_path.read_text(encoding="utf-8")
    match = re.search(r'GOOGLE_WEB_CLIENT_ID\s*=\s*"([^"]+)"', cloud_text)
    require(match is not None, "CloudConfig declares GOOGLE_WEB_CLIENT_ID")
    require(match.group(1) in web_client_ids, "CloudConfig Web client ID matches google-services.json")

    gradle_text = gradle_path.read_text(encoding="utf-8")
    for name in (
        "FRAMEBYNAVIN_RELEASE_STORE_FILE",
        "FRAMEBYNAVIN_RELEASE_STORE_PASSWORD",
        "FRAMEBYNAVIN_RELEASE_KEY_ALIAS",
        "FRAMEBYNAVIN_RELEASE_KEY_PASSWORD",
    ):
        require(name in gradle_text, f"production signing input {name} remains externalized")
    require("productionSigningConfigured" in gradle_text, "release signing is conditional on the complete production key set")

    release_appcheck = release_appcheck_path.read_text(encoding="utf-8")
    debug_appcheck = debug_appcheck_path.read_text(encoding="utf-8")
    require("PlayIntegrityAppCheckProviderFactory" in release_appcheck, "release App Check uses Play Integrity")
    require("DebugAppCheckProviderFactory" in debug_appcheck, "debug App Check remains isolated to the debug source set")

    android_oauth = [oauth for oauth in oauth_clients if oauth.get("client_type") == 1]
    if android_oauth:
        print(f"PASS: google-services.json contains {len(android_oauth)} Android OAuth client(s)")
    else:
        message = (
            "PENDING: google-services.json has no Android OAuth client (client_type=1). "
            "After the final production/upload certificate is chosen, register its SHA-1 with Google/Firebase, "
            "refresh google-services.json, then rerun this audit with --require-android-oauth-client."
        )
        if args.require_android_oauth_client:
            raise SystemExit(f"FAIL: {message}")
        print(message)

    print(
        "PENDING RUNTIME GATE: production SHA-256 must be registered for Firebase App Check / Play Integrity, "
        "then Google sign-in, YouTube authorization and Firebase AI must be exercised from the production-signed build."
    )
    print("Repository-side production identity readiness audit passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

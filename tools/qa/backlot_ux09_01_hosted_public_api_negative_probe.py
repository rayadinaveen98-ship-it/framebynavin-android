#!/usr/bin/env python3
"""Backlot Lab live PUBLIC / anonymous NEGATIVE-only Data API smoke.

No signup, JWT issuance, authenticated records, writes, SQL, hosted config edits,
service-role use, or production Backlot host. Nothing here certifies two-account
signed JWT flows, authenticated RPC exposure, or Android synchronization.
"""
from __future__ import annotations

import json
from pathlib import Path
import re
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

LAB_REF = "vasleafrmsovdbdlrcsw"
LAB_ORIGIN = f"https://{LAB_REF}.supabase.co"
CONFIG = Path(__file__).resolve().parents[2] / "app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt"

def public_key_from_checked_in_lab_client() -> str:
    source = CONFIG.read_text(encoding="utf-8")
    url_pattern = r'val SUPABASE_URL: String = if \(BuildConfig\.BACKLOT_LAB\) \{\s*"(https://[^"]+)"'
    key_pattern = r'val SUPABASE_PUBLISHABLE_KEY: String = if \(BuildConfig\.BACKLOT_LAB\) \{\s*"(sb_publishable_[^"]+)"'
    urls = re.findall(url_pattern, source)
    keys = re.findall(key_pattern, source)
    if urls != [LAB_ORIGIN] or len(keys) != 1:
        raise AssertionError("REFUSED: Lab endpoint/key missing or changed; production endpoint must not be probed")
    return keys[0]

def call(path: str, key: str, *, headers: dict[str, str] | None = None) -> tuple[int, str]:
    assert path.startswith("/rest/v1/")
    h = {"apikey": key, "Accept": "application/json"}
    if headers:
        h.update(headers)
    request = Request(LAB_ORIGIN + path, headers=h, method="GET")
    try:
        with urlopen(request, timeout=18) as response:
            status = response.status
            raw = response.read(1536)
    except HTTPError as exc:
        status = exc.code
        raw = exc.read(1536)
    # Intentionally NEVER log user data or response payload.
    try:
        obj = json.loads(raw)
        code = str(obj.get("code", ""))[:64] if isinstance(obj, dict) else ""
    except (json.JSONDecodeError, UnicodeDecodeError):
        code = ""
    return status, code

def main() -> None:
    key = public_key_from_checked_in_lab_client()
    checks = [
        (
            "private schema blocked through Accept-Profile",
            "/rest/v1/entities?" + urlencode({"select": "user_id", "limit": 1}),
            {"Accept-Profile": "backlot_sync_internal"},
            {404, 406},
        ),
        (
            "anonymous Project/Idea pull not granted",
            "/rest/v1/rpc/backlot_ux09_pull_v1?" + urlencode({"p_after_sequence": 0, "p_limit": 1}),
            {},
            {401, 403, 404},
        ),
        (
            "forged Bearer JWT rejected",
            "/rest/v1/rpc/backlot_ux09_pull_v1?" + urlencode({"p_after_sequence": 0, "p_limit": 1}),
            {"Authorization": "Bearer definitely-invalid.backlot.jwt"},
            {401, 403},
        ),
    ]
    failures: list[str] = []
    for label, path, headers, expected in checks:
        status, code = call(path, key, headers=headers)
        print(f"{label}: HTTP {status} (PostgREST code {code or 'none'})")
        if status not in expected:
            failures.append(f"{label}: unexpected status {status}")
        if "private schema" in label and status == 404:
            print("NOTE: 404 is deny; a signed-JWT 406 test is still mandatory.")
        if "anonymous Project/Idea" in label and status == 404:
            print("NOTE: 404 could be hidden RPC or missing anon EXECUTE; authenticated exposure NOT certified.")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        raise SystemExit(2)
    print("PASS: limited live anonymous/invalid-Bearer negative checks; no hosted writes.")
    print("NOT CERTIFIED: saved function-toggle config, signed hosted users, cross-account RLS,")
    print("conflict/idempotency via real hosted JWTs, or Android two-device sync.")

if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, URLError) as exc:
        print(f"STOP: non-certified live probe: {exc.__class__.__name__}: {exc}", file=sys.stderr)
        raise SystemExit(3)

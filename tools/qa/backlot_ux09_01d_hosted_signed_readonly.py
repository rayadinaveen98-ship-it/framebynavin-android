#!/usr/bin/env python3
"""Backlot Lab UX09-01D: signed two-QA-user READ-ONLY app-data acceptance.

Requires TWO *pre-provisioned* and explicitly marked disposable Lab-only Auth
identities. Never creates accounts, never changes Creator/Personal entities,
and never touches the original Backlot backend. Login/logout necessarily
create/revoke temporary Auth sessions. The users themselves are NOT deleted;
a separately reviewed admin cleanup remains mandatory.
No JWT, password, email or response payload is printed.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
import re
import sys
import uuid
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, Request, build_opener

HOST = "https://vasleafrmsovdbdlrcsw.supabase.co"
SOURCE = Path(__file__).resolve().parents[2] / "app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt"
EMAIL_RE = re.compile(r"^ux09-qa-([ab])-([a-f0-9]{8,16})@example\.invalid$")
EXACT_MODE = "BACKLOT_LAB_TWO_DISPOSABLE_USERS_READONLY"
AUTH_PATH = "/auth/v1/token?grant_type=password"
USER_PATH = "/auth/v1/user"
PULL_PATH = "/rest/v1/rpc/backlot_ux09_pull_v1?p_after_sequence=0&p_limit=100"
PRIVATE_PATH = "/rest/v1/entities?select=user_id&limit=1"
LOGOUT_PATH = "/auth/v1/logout"
ALLOWED = {(AUTH_PATH, "POST"), (USER_PATH, "GET"), (PULL_PATH, "GET"),
           (PRIVATE_PATH, "GET"), (LOGOUT_PATH, "POST")}

class StopCheck(Exception):
    pass

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise StopCheck("unexpected HTTP redirect (refusing to forward credentials)")

def key_from_checked_in_lab_client() -> str:
    source = SOURCE.read_text(encoding="utf-8")
    url = re.findall(r'val SUPABASE_URL: String = if \(BuildConfig\.BACKLOT_LAB\) \{\s*"(https://[^"]+)"', source)
    keys = re.findall(r'val SUPABASE_PUBLISHABLE_KEY: String = if \(BuildConfig\.BACKLOT_LAB\) \{\s*"(sb_publishable_[^"]+)"', source)
    if url != [HOST] or len(keys) != 1:
        raise StopCheck("Lab hostname/key guard failed: production endpoint must not be touched")
    return keys[0]

def validate_qa_emails(a: str, b: str) -> None:
    ma, mb = EMAIL_RE.fullmatch(a), EMAIL_RE.fullmatch(b)
    if not ma or not mb or ma.group(1) != "a" or mb.group(1) != "b" or ma.group(2) != mb.group(2):
        raise StopCheck("Both emails must be dedicated ux09-qa-a/b-<same 8-16 lowercase hex>@example.invalid")

def validate_auth_user(profile: object, expected_email: str) -> str:
    if not isinstance(profile, dict) or profile.get("email", "").lower() != expected_email:
        raise StopCheck("Supabase Auth identity did not match exact disposable QA email")
    if profile.get("is_anonymous") is True:
        raise StopCheck("Anonymous user cannot certify non-anonymous sync")
    try:
        identifier = str(uuid.UUID(str(profile["id"])))
    except (KeyError, TypeError, ValueError) as exc:
        raise StopCheck("Supabase Auth returned no valid user ID") from exc
    if identifier == str(uuid.UUID(int=0)):
        raise StopCheck("Zero Auth user UUID rejected")
    return identifier

def validate_empty_pull(data: object) -> None:
    if not isinstance(data, dict) or data.get("changes") != [] or data.get("next_cursor") != 0 or data.get("has_more") is not False:
        raise StopCheck("QA user already has sync data or pull contract differs; stop without writes")

def validate_private_denial(code: int) -> None:
    if code not in {404, 406}:
        raise StopCheck("Private schema not reliably denied to signed user")

def request_json(path: str, method: str, key: str, token: str | None = None,
                 body: dict | None = None, extra_headers: dict | None = None) -> tuple[int, object]:
    if (path, method) not in ALLOWED:
        raise StopCheck("Non-allowlisted request rejected")
    headers = {"apikey": key, "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if extra_headers:
        headers.update(extra_headers)
    raw_body = None
    if body is not None:
        raw_body = json.dumps(body, separators=(",", ":")).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = Request(HOST + path, headers=headers, data=raw_body, method=method)
    try:
        with build_opener(NoRedirect()).open(req, timeout=18) as response:
            status, raw = response.status, response.read(65537)
    except HTTPError as error:
        status, raw = error.code, error.read(65537)
    if len(raw) > 65536:
        raise StopCheck("Unexpectedly large API response")
    if not raw:
        return status, None
    try:
        return status, json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise StopCheck(f"Non-JSON HTTP response (status {status})") from exc

def assert_success(status: int, detail: str) -> None:
    if status != 200:
        raise StopCheck(f"{detail} failed with HTTP {status}; no payload logged")

def main() -> int:
    if os.environ.get("UX09_QA_MODE") != EXACT_MODE:
        raise StopCheck("Explicit UX09_QA_MODE acknowledgement missing")
    a_email = os.environ.get("UX09_QA_A_EMAIL", "").lower()
    b_email = os.environ.get("UX09_QA_B_EMAIL", "").lower()
    a_password = os.environ.get("UX09_QA_A_PASSWORD", "")
    b_password = os.environ.get("UX09_QA_B_PASSWORD", "")
    validate_qa_emails(a_email, b_email)
    if not a_password or not b_password or a_password == b_password:
        raise StopCheck("Two distinct QA-only passwords required; never supply real account credentials")
    key = key_from_checked_in_lab_client()
    sessions: list[tuple[str, str]] = []
    cleanup_ok = True
    result = 1
    try:
        identities: list[str] = []
        for label, email, password in (("A", a_email, a_password), ("B", b_email, b_password)):
            status, auth = request_json(AUTH_PATH, "POST", key, body={"email": email, "password": password})
            assert_success(status, f"Test {label} password sign-in")
            token = auth.get("access_token") if isinstance(auth, dict) else None
            if not isinstance(token, str) or not token:
                raise StopCheck(f"Test {label} did not receive a signed Auth access token")
            sessions.append((label, token))
            status, user = request_json(USER_PATH, "GET", key, token=token)
            assert_success(status, f"Test {label} Auth user verification")
            identities.append(validate_auth_user(user, email))
            status, data = request_json(PULL_PATH, "GET", key, token=token)
            assert_success(status, f"Test {label} authenticated Creator sync pull")
            validate_empty_pull(data)
            code, _ = request_json(PRIVATE_PATH, "GET", key, token=token,
                                   extra_headers={"Accept-Profile": "backlot_sync_internal"})
            validate_private_denial(code)
            print(f"PASS: signed, non-anonymous QA {label} Auth/pull/private-schema isolation")
        if identities[0] == identities[1] or sessions[0][1] == sessions[1][1]:
            raise StopCheck("QA A and B must be separate Auth identities and tokens")
        print("PASS: distinct hosted signed QA identities; no Creator/Personal application data written")
        result = 0
    finally:
        for label, token in sessions:
            try:
                code, _ = request_json(LOGOUT_PATH, "POST", key, token=token)
                if code not in {200, 204}:
                    print(f"STOP: Auth session logout for {label} returned HTTP {code}; revoke manually", file=sys.stderr)
                    cleanup_ok = False
                else:
                    print(f"PASS: temporary QA {label} Auth session logout submitted")
            except (StopCheck, HTTPError, URLError, TimeoutError):
                print(f"STOP: Auth session logout for {label} not confirmed; revoke manually", file=sys.stderr)
                cleanup_ok = False
        if not cleanup_ok:
            result = 3
    if result == 0:
        print("NOT CERTIFIED: cross-account CAS/writes, tombstones, deletion, Android or full Backup/Restore")
        print("QA Auth users STILL EXIST until a separate supervised admin deletion; do not assume removal.")
    return result

if __name__ == "__main__":
    try:
        sys.exit(main())
    except (StopCheck, OSError, URLError, TimeoutError) as err:
        # All StopCheck exceptions carry non-sensitive fixed descriptions.
        print(f"STOP: {err}", file=sys.stderr)
        sys.exit(2)

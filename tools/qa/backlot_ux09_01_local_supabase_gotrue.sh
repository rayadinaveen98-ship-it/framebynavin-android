#!/usr/bin/env bash
# Backlot UX09-01: REAL local Supabase GoTrue signed-JWT + PostgREST check.
# Safety: LOCALHOST:54321 ONLY. Never hosted, never read old account tokens.
# No JWT, email, password, public key or private content printed.
set -euo pipefail
set +x
umask 077

BASE="${UX09_LOCAL_SUPABASE_URL:-}"
KEY="${UX09_LOCAL_PUBLISHABLE_KEY:-}"
DB="${UX09_LOCAL_DB_URL:-}"
case "$BASE" in http://127.0.0.1:54321|http://localhost:54321) ;; *)
 echo "REFUSED: this test only runs against localhost:54321" >&2; exit 2 ;; esac
# The DB URL can contain a password. Never log it, use only localhost targets.
case "$DB" in
 postgresql://*@127.0.0.1:54322/*|postgresql://*@localhost:54322/*|postgres://*@127.0.0.1:54322/*|postgres://*@localhost:54322/*) ;;
 *) echo "REFUSED: only the local Supabase database at port 54322 is allowed" >&2; exit 2 ;;
esac
[[ -n "$KEY" ]] || { echo "Local publishable/anon key missing" >&2; exit 2; }
for command in curl jq psql python3; do
 command -v "$command" >/dev/null || { echo "Missing required $command" >&2; exit 2; }
done
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# The actual candidate SQL is applied only to a freshly initialized LOCAL stack.
# Local schema and function auth.uid()/auth.jwt() belong to Supabase GoTrue;
# the synthetic Postgres mock fixture is deliberately NOT used here.
psql "$DB" -X -v ON_ERROR_STOP=1 \
 -f "$ROOT/supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql" \
 > "$TMP/schema_log" 2>&1 || {
 echo "Local Supabase schema failed to compile; redacted logs kept internal" >&2
 exit 1
}
psql "$DB" -X -v ON_ERROR_STOP=1 \
 -f "$ROOT/supabase/prototypes/ux09_01_security_assertions_v1.sql" \
 > "$TMP/security_log" 2>&1 || {
 echo "Local Supabase security checks failed; no hosted DB touched" >&2
 exit 1
}
echo "PASS: real local Supabase schema and static privilege assertions"

# Real Auth tokens from a local GoTrue instance (no hand-crafted JWT).
ID="$(python3 -c 'import uuid; print(uuid.uuid4().hex[:18])')"
PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(28))')"
signup() {
 local label="$1" out="$2"
 local email="ux09-${label}-${ID}@example.invalid" status
 local body; body="$(jq -cn --arg email "$email" --arg pass "$PASSWORD" \
  '{email:$email,password:$pass}')"
 status="$(curl --silent --show-error --max-time 20 --connect-timeout 5 \
  -o "$out" -w '%{http_code}' \
  -H "apikey: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE/auth/v1/signup" --data-binary "$body")"
 [[ "$status" == 200 || "$status" == 201 ]] || {
  echo "GoTrue local signup $label failed (HTTP $status), no response exposed" >&2
  exit 1
 }
 local token
 token="$(jq -er '.access_token // empty' "$out" 2>/dev/null || true)"
 if [[ -z "$token" ]]; then
  # Auth config might require confirmation; signed-JWT tests must STOP,
  # not forge a JWT or use a privileged service_role workaround.
  echo "Local Auth issued no access token; confirm email must be disabled in disposable config" >&2
  exit 1
 fi
 printf '%s' "$token"
}
A="$(signup A "$TMP/signup_a")"
B="$(signup B "$TMP/signup_b")"
[[ "$A" != "$B" ]] || { echo "Different GoTrue accounts returned identical JWT" >&2; exit 1; }
echo "PASS: two distinct locally signed GoTrue access tokens issued"

# Test actual Auth JWT validation, CAS, isolation, tombstone and delta through
# PostgREST as the Android app will do. The existing fixture validates the
# distinct account user IDs with /auth/v1/user.
export UX09_LOCAL_SUPABASE_URL="$BASE"
export UX09_LOCAL_PUBLISHABLE_KEY="$KEY"
export UX09_TEST_A_JWT="$A"
export UX09_TEST_B_JWT="$B"
bash "$ROOT/tools/qa/backlot_ux09_01_disposable_auth_matrix.sh"
unset UX09_TEST_A_JWT UX09_TEST_B_JWT
echo "PASS: actual local GoTrue JWTs used for two-account PostgREST test"

# Exposed-schema bypass check: internal schema must NOT be exposed to REST.
# A valid user must be unable to bypass expected_revision via direct table
# endpoint. A 404/406 means Kong/PostgREST refuses requested schema/route.
code="$(curl --silent --show-error --max-time 20 --connect-timeout 5 \
  -o "$TMP/private_schema" -w '%{http_code}' \
  -H "apikey: $KEY" -H "Authorization: Bearer $A" \
  -H "Accept-Profile: backlot_sync_internal" \
  "$BASE/rest/v1/entities?select=domain&limit=1")"
[[ "$code" == 404 || "$code" == 406 ]] || {
 echo "STOP: private schema may be exposed via REST (HTTP $code)" >&2
 exit 1
}
echo "PASS: private sync tables NOT exposed to REST API"

# The caller must not be able to invoke RPCs unauthenticated or with a
# syntactically invalid bearer token. No Authorization header is supplied.
REQ='{"p_after_sequence":0,"p_limit":1}'
for auth_mode in missing invalid; do
 auth_args=()
 if [[ "$auth_mode" == invalid ]]; then
  auth_args=(-H "Authorization: Bearer deliberately-invalid-JWT")
 fi
 code="$(curl --silent --show-error --max-time 20 --connect-timeout 5 \
  -o "$TMP/denied_$auth_mode" -w '%{http_code}' \
  -H "apikey: $KEY" -H "Content-Type: application/json" \
  "${auth_args[@]}" \
  -X POST "$BASE/rest/v1/rpc/backlot_ux09_pull_v1" --data-binary "$REQ")"
 [[ "$code" == 401 || "$code" == 403 || "$code" == 404 ]] || {
  echo "STOP: $auth_mode caller did not receive an auth refusal (HTTP $code)" >&2
  exit 1
 }
done
echo "PASS: missing/invalid Supabase access tokens cannot call sync RPCs"
echo "UX09-01 SIGNED LOCAL GOTRUE + POSTGREST PASSED; NO HOSTED LAB CHANGES"

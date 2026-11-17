#!/usr/bin/env bash
# Backlot UX09-01. LOCAL/DISPOSABLE PostgreSQL 16 ONLY; NEVER hosted.
# Real Supabase Auth JWT/REST tests are separate: this mocks auth.uid and uses
# PostgreSQL SET ROLE to verify SQL compilation, RLS and optimistic locks.
set -euo pipefail
set +x
umask 077

: "${PGHOST:=127.0.0.1}"
: "${PGPORT:=5432}"
: "${PGDATABASE:=backlot_ux09_disposable}"
: "${PGUSER:=postgres}"
case "${PGHOST}" in localhost|127.0.0.1) ;; *)
  echo "REFUSED: PostgreSQL tests require loopback host" >&2; exit 2 ;;
esac
[[ "$PGPORT" == 5432 && "$PGDATABASE" == backlot_ux09_disposable ]] || {
  echo "REFUSED: use port 5432 and database backlot_ux09_disposable" >&2; exit 2;
}
command -v psql >/dev/null || { echo "Missing PostgreSQL psql CLI" >&2; exit 2; }
export PGHOST PGPORT PGDATABASE PGUSER
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FIX="$ROOT/supabase/prototypes/ux09_01_disposable_pg_auth_fixture.sql"
PROTO="$ROOT/supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql"
SEC="$ROOT/supabase/prototypes/ux09_01_security_assertions_v1.sql"
CASES="$ROOT/supabase/prototypes/ux09_01_disposable_pg_scenarios.sql"

# First static guard: refuse an existing non-empty database. A disposable
# PostgreSQL database with pre-existing tables must not be modified.
preexisting="$(psql -XAt -v ON_ERROR_STOP=1 -c  "select count(*) from pg_tables where schemaname not in ('pg_catalog','information_schema')")"
if [[ "$preexisting" != "0" ]]; then
  echo "REFUSED: disposable DB already has tables, preserving all existing data" >&2
  exit 2
fi

psql -X -v ON_ERROR_STOP=1 -f "$FIX" > /dev/null
psql -X -v ON_ERROR_STOP=1 -f "$PROTO" > /dev/null
psql -X -v ON_ERROR_STOP=1 -f "$SEC" > /dev/null
psql -X -v ON_ERROR_STOP=1 -f "$CASES" > /dev/null
echo "PASS: disposable PG16 schema compilation + mock-A/B SQL/RLS/CAS scenarios"

# Two independent concurrent authenticated sessions mutate the SAME new
# entity at expected revision 0. Exactly one may APPLIED; one must CONFLICT.
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
run_race() {
  local op="$1" title="$2" out="$3"
  psql -XAt -v ON_ERROR_STOP=1 > "$out" <<SQL
set role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',false);
select public.backlot_ux09_apply_v1(
  '$op'::uuid,'creator_idea',
  '99999999-9999-4999-8999-999999999999'::uuid,0,
  jsonb_build_object('title','$title'),false
);
SQL
}
run_race "66666666-6666-4666-8666-666666666666" "Race A" "$tmp/a" &
pa=$!
run_race "77777777-7777-4777-8777-777777777777" "Race B" "$tmp/b" &
pb=$!
wait "$pa"; wait "$pb"
extract() { grep -E '^\{' "$1" | tail -n 1; }
ra="$(extract "$tmp/a")"; rb="$(extract "$tmp/b")"
command -v jq >/dev/null || { echo "Missing jq" >&2; exit 2; }
ja="$(printf '%s' "$ra" | jq -er '.status')"
jb="$(printf '%s' "$rb" | jq -er '.status')"
if ! { [[ "$ja" == applied && "$jb" == conflict ]] ||
        [[ "$ja" == conflict && "$jb" == applied ]]; }; then
  echo "FAILED: two concurrent creates were not one APPLIED and one CONFLICT" >&2
  exit 1
fi
echo "PASS: two actual concurrent PostgreSQL sessions serialize same-account CAS"
echo "IMPORTANT: Real Supabase Auth JWT and REST RLS A/B tests remain pending"

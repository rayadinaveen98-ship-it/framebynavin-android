#!/usr/bin/env bash
# Backlot UX09-01: DISPOSABLE LOCAL Supabase only. NEVER hosted Backlot Lab.
# Requires two distinct disposable AUTHENTICATED user JWTs from local Supabase.
# This script creates synthetic Creator projects/ideas; never use private data.
# Usage: UX09_LOCAL_SUPABASE_URL=http://127.0.0.1:54321 \
#   UX09_LOCAL_PUBLISHABLE_KEY=... UX09_TEST_A_JWT=... UX09_TEST_B_JWT=... bash "$0"
set -euo pipefail
set +x
umask 077

BASE="${UX09_LOCAL_SUPABASE_URL:-}"
KEY="${UX09_LOCAL_PUBLISHABLE_KEY:-}"
A="${UX09_TEST_A_JWT:-}"
B="${UX09_TEST_B_JWT:-}"
case "$BASE" in
  http://127.0.0.1:54321|http://localhost:54321) ;;
  *) echo "REFUSED: only disposable local Supabase at localhost:54321 is allowed" >&2; exit 2 ;;
esac
for cmd in curl jq python3; do
  command -v "$cmd" >/dev/null || { echo "Missing $cmd" >&2; exit 2; }
done
if [[ -z "$KEY" || -z "$A" || -z "$B" || "$A" == "$B" ]]; then
  echo "Missing local publishable key or two distinct disposable JWTs" >&2
  exit 2
fi
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

new_uuid() {
  python3 -c 'import uuid; print(uuid.uuid4())'
}
# Never echo JWTs, request headers, PII, private payloads or curl's verbose output.
call() {
  local token="$1" endpoint="$2" body="$3" out="$4"
  local status
  status="$(curl --silent --show-error --connect-timeout 5 --max-time 20 \
    -o "$out" -w '%{http_code}' \
    -H "apikey: $KEY" -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -X POST "$BASE/rest/v1/rpc/$endpoint" --data-binary "$body")"
  if [[ "$status" -lt 200 || "$status" -gt 299 ]]; then
    echo "RPC $endpoint failed (HTTP $status); no response body logged" >&2
    return 1
  fi
}
expect_jq() {
  jq -e "$2" "$1" >/dev/null || { echo "FAILED: $3" >&2; return 1; }
}
account_id() {
  local token="$1" out="$2" status
  status="$(curl --silent --show-error --connect-timeout 5 --max-time 20 \
    -o "$out" -w '%{http_code}' \
    -H "apikey: $KEY" -H "Authorization: Bearer $token" "$BASE/auth/v1/user")"
  [[ "$status" == "200" ]] || { echo "Local identity read refused: $status" >&2; exit 1; }
  jq -er '.id' "$out"
}

id_a="$(account_id "$A" "$tmp/a_user")"
id_b="$(account_id "$B" "$tmp/b_user")"
[[ "$id_a" != "$id_b" ]] || { echo "Refused: A and B resolve to the same account" >&2; exit 2; }

ENTITY="$(new_uuid)"
CREATE_OP="$(new_uuid)"
WRONG_OP="$(new_uuid)"
B_CREATE="$(new_uuid)"
A_UPDATE="$(new_uuid)"
A_DELETE="$(new_uuid)"
make_change() {
  jq -cn \
    --arg op "$1" --arg domain "$2" --arg id "$3" \
    --argjson expected "$4" --argjson payload "$5" --argjson deleted "$6" \
    '{p_operation_id:$op,p_domain:$domain,p_entity_id:$id,
      p_expected_revision:$expected,p_payload:$payload,p_deleted:$deleted}'
}
make_pull() {
  jq -cn --argjson since "$1" --argjson lim "$2" \
    '{p_after_sequence:$since,p_limit:$lim}'
}
create="$(make_change "$CREATE_OP" "creator_project" "$ENTITY" 0 '{"title":"A synthetic project"}' false)"
call "$A" backlot_ux09_apply_v1 "$create" "$tmp/create"
expect_jq "$tmp/create" '.status=="applied" and .revision==1' "create A revision 1"
call "$A" backlot_ux09_apply_v1 "$create" "$tmp/retry"
cmp -s "$tmp/create" "$tmp/retry" || { echo "FAILED: idempotent retry differs" >&2; exit 1; }
echo "PASS: authenticated A create / exact idempotent retry"

# Reusing an operation UUID with a different payload must not silently succeed.
mutated="$(make_change "$CREATE_OP" "creator_project" "$ENTITY" 0 '{"title":"tampered retry"}' false)"
reused_status="$(curl --silent --show-error --connect-timeout 5 --max-time 20 \\
  -o "$tmp/reused" -w '%{http_code}' \\
  -H "apikey: $KEY" -H "Authorization: Bearer $A" \\
  -H "Content-Type: application/json" \\
  -X POST "$BASE/rest/v1/rpc/backlot_ux09_apply_v1" --data-binary "$mutated")"
[[ "$reused_status" -ge 400 && "$reused_status" -lt 500 ]] || {
  echo "FAILED: mutation with reused operation UUID was not rejected" >&2
  exit 1
}
echo "PASS: operation UUID reuse with different data is rejected"

stale="$(make_change "$WRONG_OP" "creator_project" "$ENTITY" 0 '{"title":"stale"}' false)"
call "$A" backlot_ux09_apply_v1 "$stale" "$tmp/stale"
expect_jq "$tmp/stale" '.status=="conflict" and .current_revision==1' "stale A must conflict"
call "$B" backlot_ux09_apply_v1 "$(make_change "$(new_uuid)" "creator_project" "$ENTITY" 1 '{"title":"trying A"}' false)" "$tmp/b_foreign"
expect_jq "$tmp/b_foreign" '.status=="conflict" and .current_revision==0' "B cannot update A"
echo "PASS: revision CAS / account separation"

bcreate="$(make_change "$B_CREATE" "creator_project" "$ENTITY" 0 '{"title":"B isolated record"}' false)"
call "$B" backlot_ux09_apply_v1 "$bcreate" "$tmp/b_create"
expect_jq "$tmp/b_create" '.status=="applied" and .revision==1' "B can have same entity id in own account"
call "$A" backlot_ux09_pull_v1 "$(make_pull 0 100)" "$tmp/pull_a"
call "$B" backlot_ux09_pull_v1 "$(make_pull 0 100)" "$tmp/pull_b"
# Compare only synthetic marker content, not account emails or tokens.
expect_jq "$tmp/pull_a" '.changes|map(select(.payload.title=="B isolated record"))|length==0' "A must not read B"
expect_jq "$tmp/pull_b" '.changes|map(select(.payload.title=="A synthetic project"))|length==0' "B must not read A"
echo "PASS: two accounts never exchange payloads"

upd="$(make_change "$A_UPDATE" "creator_project" "$ENTITY" 1 '{"title":"A updated"}' false)"
call "$A" backlot_ux09_apply_v1 "$upd" "$tmp/update_a"
expect_jq "$tmp/update_a" '.revision==2 and .status=="applied"' "A update"
del="$(make_change "$A_DELETE" "creator_project" "$ENTITY" 2 '{}' true)"
call "$A" backlot_ux09_apply_v1 "$del" "$tmp/delete_a"
expect_jq "$tmp/delete_a" '.revision==3 and .deleted==true' "A tombstone"
call "$A" backlot_ux09_apply_v1 "$(make_change "$(new_uuid)" "creator_project" "$ENTITY" 0 '{"title":"resurrect"}' false)" "$tmp/resurrection"
expect_jq "$tmp/resurrection" '.status=="conflict" and .deleted==true and .current_revision==3' "no resurrection"
call "$A" backlot_ux09_pull_v1 "$(make_pull 0 1)" "$tmp/page1"
expect_jq "$tmp/page1" '.changes|length==1' "bounded page 1"
expect_jq "$tmp/page1" '.has_more==true' "cursor has_more"
call "$A" backlot_ux09_pull_v1 "$(make_pull 0 100)" "$tmp/all"
expect_jq "$tmp/all" '.changes|any(.revision==3 and .deleted==true and .payload=={})' "tombstone delta"
call "$B" backlot_ux09_pull_v1 "$(make_pull 0 100)" "$tmp/b_last"
expect_jq "$tmp/b_last" '.changes|any(.payload.title=="B isolated record" and .deleted==false)' "B unaffected"
echo "PASS: updated records / tombstone / 1-row page / cursor / B unaffected"

# Validation guard: unsupported domains must fail before a mutation.
bad="$(make_change "$(new_uuid)" "personal_photo" "$(new_uuid)" 0 '{"title":"not supported"}' false)"
status="$(curl --silent --show-error --connect-timeout 5 --max-time 20 \
  -o "$tmp/rejected" -w '%{http_code}' \
  -H "apikey: $KEY" -H "Authorization: Bearer $A" \
  -H "Content-Type: application/json" \
  -X POST "$BASE/rest/v1/rpc/backlot_ux09_apply_v1" --data-binary "$bad")"
[[ "$status" -ge 400 && "$status" -lt 500 ]] || { echo "FAILED: unsupported domain not rejected" >&2; exit 1; }
echo "PASS: disallowed Personal/private-media domain rejected"

# The second whitelisted domain is real, not a documentation-only option.
idea_id="$(new_uuid)"
idea_body="$(make_change "$(new_uuid)" "creator_idea" "$idea_id" 0 '{"title":"Synthetic Idea"}' false)"
call "$A" backlot_ux09_apply_v1 "$idea_body" "$tmp/idea_a"
expect_jq "$tmp/idea_a" '.status=="applied" and .revision==1' "Creator Idea domain"
call "$A" backlot_ux09_pull_v1 "$(make_pull 0 100)" "$tmp/ideas"
expect_jq "$tmp/ideas" '.changes|any(.domain=="creator_idea" and .payload.title=="Synthetic Idea")' "Idea pull"
echo "PASS: Creator Idea domain applies and pulls"

echo "UX09-01 disposable A/B contract smoke PASS; NOT production/hosted verification"

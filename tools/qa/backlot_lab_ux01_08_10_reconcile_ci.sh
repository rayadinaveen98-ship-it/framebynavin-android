#!/usr/bin/env bash
# BL-UX-INTEGRATION-01: READ-ONLY provenance + protected P0 audit.
# The cumulative source entered the isolated branch via scoped PR #446.
# Never mutate stable/main, original backend, data or UX09.
set -euo pipefail

LAB_SHA="4ba1263124d118d3917f988228aec2248068466f"
UX_SHA="6654c730971309b5047473e2f69292fe799b3f46"
CANDIDATE="integration/backlot-lab-v337-ux01-08-10-20261118"
CURRENT_REF="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-$(git branch --show-current)}}"

test "$CURRENT_REF" = "$CANDIDATE" || { echo "REF GATE FAILED"; exit 1; }
git merge-base --is-ancestor "$LAB_SHA" HEAD || { echo "Owner-tested Lab V337 missing"; exit 1; }
git merge-base --is-ancestor "$UX_SHA" HEAD || { echo "UX10F cumulative source missing"; exit 1; }
echo "PASS: source includes exact owner Lab $LAB_SHA and cumulative UX $UX_SHA"

# For every Lab-only correction, require exact byte identity at the candidate.
# Only the three previously audited UX+P0 overlap paths are permitted to differ.
MERGE_BASE="$(git merge-base "$LAB_SHA" "$UX_SHA")"
while IFS= read -r path; do
  case "$path" in
    app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt|\
    app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt|\
    app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt) continue;;
  esac
  if ! git diff --quiet "$LAB_SHA" HEAD -- "$path"; then
    echo "FAIL: Lab P0 / backend file modified by UX integration: $path"
    exit 1
  fi
done < <(git diff --name-only "$MERGE_BASE" "$LAB_SHA")

# Real Android 26 devices cannot call API34 LocalDate.ofInstant; preserve our fix
# across ALL main/debug/release Android source, not just the conflicted files.
if git grep -n -F 'LocalDate.ofInstant(' -- app/src/main/java app/src/debug/java app/src/release/java app/src/personalStable/java; then
  echo "FAIL: unsupported Android API call reintroduced"; exit 1
fi
grep -Fq '?.toInstant()?.toEpochMilli() == scheduledAt' \
  app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt || {
  echo "FAIL: exact epoch-ms snooze P0 regression"; exit 1;
}
grep -Fq 'if (backlotLabMode) "com.framebynavin.app.lab"' app/build.gradle.kts || {
  echo "FAIL: Lab package switched"; exit 1;
}
grep -Fq 'vasleafrmsovdbdlrcsw.supabase.co' \
  app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt || {
  echo "FAIL: Lab backend switched"; exit 1;
}
test ! -d app/src/main/java/com/framebynavin/app/cloud/ux09rpc || {
  echo "FAIL: deferred UX09 code unexpectedly integrated"; exit 1;
}
test ! -d supabase/lab_migrations || {
  echo "FAIL: deferred UX09 migrations unexpectedly integrated"; exit 1;
}
echo "PASS: all Lab-exclusive files, P0 Snooze precision, Android26 API, Lab identity and UX09 exclusion"

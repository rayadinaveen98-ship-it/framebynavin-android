#!/usr/bin/env bash
# BL-UX-INTEGRATION-01: isolated, reviewable Lab V337 + UX01-08/UX10 merge.
# Never run on main, owner-tested stable, production, or UX09 branches.
set -euo pipefail

LAB_SHA="4ba1263124d118d3917f988228aec2248068466f"
UX_SHA="6654c730971309b5047473e2f69292fe799b3f46"
CANDIDATE="integration/backlot-lab-v337-ux01-08-10-20261118"
UX_BRANCH="feature/backlot-ux10f-global-consistency-integration-gates-20261117"

test "${GITHUB_REF_NAME:-}" = "$CANDIDATE" || { echo "REF GATE FAILED"; exit 1; }
git merge-base --is-ancestor "$LAB_SHA" HEAD || { echo "LAB ANCESTRY FAILED"; exit 1; }
git fetch --no-tags --depth=750 origin "refs/heads/$UX_BRANCH:refs/remotes/origin/$UX_BRANCH"
test "$(git rev-parse "refs/remotes/origin/$UX_BRANCH")" = "$UX_SHA" || {
  echo "UX SOURCE HEAD MOVED; REFUSE DRIFT"; exit 1;
}
if git merge-base --is-ancestor "$UX_SHA" HEAD; then
  echo "Already integrated; no second merge."
  exit 0
fi

git config user.name "backlot-integration-ci[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
set +e
git merge --no-ff --no-commit --no-gpg-sign "$UX_SHA"
MERGE_EXIT=$?
set -e
git diff --name-only --diff-filter=U > /tmp/backlot-unmerged.txt
echo "Git merge exit=$MERGE_EXIT; unresolved files:"
cat /tmp/backlot-unmerged.txt

# Only these three overlapping 21-day/Reminder files may be auto-reconciled;
# any unexpected conflict blocks the candidate without a push.
python3 - <<'PY'
from pathlib import Path
allowed = {
    'app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt',
    'app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt',
    'app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt',
}
got = set(Path('/tmp/backlot-unmerged.txt').read_text().splitlines())
assert got <= allowed, f'REVIEW REQUIRED: unexpected conflicts {sorted(got - allowed)}'
print('PASS: only three audited overlap paths (or fewer)')
PY

if [ -s /tmp/backlot-unmerged.txt ]; then
  while IFS= read -r path; do git checkout --theirs -- "$path"; done < /tmp/backlot-unmerged.txt
fi

# Reapply Lab P0 Android API-26-compatible LocalDate conversions while retaining
# the fuller UX06/UX07/UX10 behavior. This is an exact code rewrite limited to
# three previously reviewed overlapping files; never use global "ours"/"theirs".
python3 - <<'PY'
from pathlib import Path
paths = [
    Path('app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt'),
    Path('app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt'),
    Path('app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt'),
]
def android26_instant_conversion(code: str):
    needle = 'LocalDate.ofInstant('
    count = 0
    while needle in code:
        start = code.index(needle)
        args_start = start + len(needle)
        depth = 0
        comma = None
        end = None
        for k in range(args_start, len(code)):
            ch = code[k]
            if ch == '(':
                depth += 1
            elif ch == ')':
                if depth == 0:
                    end = k
                    break
                depth -= 1
            elif ch == ',' and depth == 0 and comma is None:
                comma = k
        assert comma is not None and end is not None, 'Unexpected LocalDate API syntax'
        instant = code[args_start:comma].strip()
        zone = code[comma + 1:end].strip()
        assert instant and zone, 'Empty Instant/ZoneId argument'
        code = code[:start] + f'{instant}.atZone({zone}).toLocalDate()' + code[end + 1:]
        count += 1
    return code, count

for path in paths:
    text = path.read_text()
    text, count = android26_instant_conversion(text)
    path.write_text(text)
    print(f'{path.name}: restored {count} API-26-safe conversions')
live = paths[0].read_text()
assert '?.toInstant()?.toEpochMilli() == scheduledAt' in live, (
    'P0 exact epoch-millisecond Snooze occurrence fix missing')
assert 'LocalDate.ofInstant(' not in ''.join(p.read_text() for p in paths)
print('PASS: Lab P0 Snooze precision and Android 26 chronology')
PY

git add --   app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt   app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt   app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt
if [ -n "$(git diff --name-only --diff-filter=U)" ]; then
  echo "UNRESOLVED CONFLICT BLOCK"; git diff --name-only --diff-filter=U; exit 1
fi

# Every Lab-exclusive modified file must remain BYTE-FOR-BYTE identical to
# owner-tested stable; reconcile only the three explicit overlapping files.
MERGE_BASE="$(git merge-base "$LAB_SHA" "$UX_SHA")"
git diff --name-only "$MERGE_BASE" "$LAB_SHA" | while IFS= read -r path; do
  case "$path" in
    app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt|\
    app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt|\
    app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt) continue;;
  esac
  if ! git diff --quiet "$LAB_SHA" -- "$path"; then
    echo "P0 / LAB FILE DRIFT: $path"; exit 1
  fi
done

if git grep -n -F 'LocalDate.ofInstant(' -- app/src/main/java app/src/debug/java app/src/release/java app/src/personalStable/java; then
  echo 'API26 UNSUPPORTED CALL BLOCK'; exit 1
fi
git diff --cached --check
git status --short
echo "PASS: ready for compilation + tests; NO source merged to stable/main."

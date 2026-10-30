# Backlot V203 — nudge fatigue & relevance

## Preview identity
- Code 223 / `2.7.3-v203-nudge-fatigue-preview`
- Parent: V202 contextual project notification preview
- Review branch: `feature/backlot-v203-nudge-fatigue-relevance`

## Problem corrected
V201 saved only the last nudge key globally. Sending A, then B, then A could bypass the promised 72-hour same-key cooldown. When the top-ranked task was still in cooldown, the worker would also skip every other otherwise eligible task. Both problems are fixed.

## Behavior
- Keep the **global 24-hour** nudge limit, mandatory local daytime window (09:00–19:59), optional more restrictive quiet hours, opt-in default off, Android permission and notification channel checks.
- Query existing active-project nudge list in priority order; skip READY/incomplete and suppressed entries; send at most the first eligible project, or none.
- Persist a SHA-256-derived preference key for each posted notification (not raw IDs), with a 72-hour same-key cap that survives app process death and A-B-A rotation.
- Migrate V201's saved last-key data into the per-key history before recording a different key.
- Prune per-key history older than seven days when a new nudge is posted. Preserve legacy last-key/last-at fields for compatibility.
- Keep the real-project tap destination from V202 and approved B status icon from V200.
- No changes to reminder/voice/alarm/smart delivery logic, user consent, app package name, signing identity or data schema.

## Release gates
1. Run `tools/check_v203_nudge_fatigue.py`, all inherited V202 source guards, unit tests, lint, personal APK signing verification, and full emulator regression on **exact commit**.
2. Pure JVM tests cover A→B→A rotation, next-eligible project, 24-hour global cap, READY exclusion, local quiet hours, clock rollback, blank candidates, hashed storage keys.
3. Android in-place update acceptance must verify no project loss; existing opt-out stays off; nudge tap routes to the correct project; notification channel choice is respected; no unintended audible alerts.
4. Confirm no history data leaks into logs or notification copy and no changes to actual scheduled creator reminders.
5. Keep PR **draft** until explicitly accepted on a real device; do not merge or mark stable without approval.

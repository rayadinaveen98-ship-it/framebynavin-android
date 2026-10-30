# Backlot V204 — engagement freshness acceptance

VersionCode **224**, versionName `2.7.4-v204-nudge-freshness-preview`.
Base: V203 nudge-fatigue and relevance. Draft preview only.

## Product rationale
An opt-in context notification should reference relevant current work. A project with a missed check-in many weeks ago should not receive an ongoing promotional reminder. These optional engagement rules do **not** modify alarm, voice, Smart escalation, explicit reminders, or project data.

## Rules
- Use real existing project state, never fabricated progress, statistics, user behavior, or generic marketing text.
- Reject missing, archived, completed or skipped project references.
- Overdue planned publish date: discretionary nudge only during the first **7 days** after that date. The actual overdue project remains in Backlot's existing UI/reminder systems.
- A passed user reminder time: engagement nudge only within the first **24 hours**, and only while that reminder remains enabled. Actual reminders and follow-ups remain governed by their own policies.
- Deadline-risk nudges must reference an actual future date that matches the live project.
- Other/unknown and READY-only nudges do not generate discretionary sends.
- Preserve previous V200–V203 protections: opt-in off by default, Android permission and channel checks, local 09:00–19:59 window, custom quiet hours, 24h global cap, 72h per-key cap, next eligible selection, hashed local history, appropriate project deep-link and Backlot brand.
- No app database, settings format, package, signing, alarm, voice, actual notification actions or reminder scheduling change.

## Verification
- JVM `V204NudgeFreshnessPolicyTest`: fresh/ancient overdue, missed reminders, future deadline matching, active and archived tasks, unknown keys.
- Source audit `tools/check_v204_nudge_freshness.py` layers on prior guarded changes.
- CI: source checks, clean JVM/unit tests, lint, debug/instrumentation/personal-stable APK build, signing identity and package/version checks, full emulator regressions.
- Real-phone test after CI: in-place update, no project loss, no false/obsolete prompts, deep-link to actual selected project, no duplicate alerts, preserved quiet hours/consent and actual alarms.
- Do not merge or promote as stable without successful exact-commit CI and creator's device acceptance.

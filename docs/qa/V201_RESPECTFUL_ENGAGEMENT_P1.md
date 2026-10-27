# Backlot V201 P1 — respectful creator engagement (preview)

Base: verified V200 `26a0d6da0d69598899d145601dd776c60ea32af5`.
Version: 221 / `2.7.1-v201-respectful-nudges-preview`.

## Scope and behavior
- Only the **existing, disabled-by-default** Settings toggle `contextNudgesEnabled` authorizes discretionary engagement notifications.
- Use the existing local `CreatorContextNudgeEngine.topNudge(tasks)` for authentic creator tasks (never fabricated deadlines, spammy clickbait, or platform-growth claims).
- Restrict non-critical nudges to local 09:00–19:59, even when quiet hours are off; if custom quiet hours are enabled, honor them too.
- Enforce at least **24 hours across all nudge keys** and **72 hours for repeating the same key**. Fail closed on backward clock jumps. Never mark a nudge sent unless Android receives the notification.
- Respect Android notification permission and the creator context nudge channel; do not bypass disabled channels.
- Retain Backlot approved monochrome status icon. Reuse notification channel `creator_context_nudges` and WorkManager IDs.
- No changes to actual reminders, alarms, voice, Smart escalation, user actions, project data schema, app ID, or installer signer. Existing nudge opt-in stays off by default. No remote tracking.

## Verification
- Pure-JVM `V201ContextNudgePolicyTest` covers daytime bounds, custom quiet hours, global cooldown, repeated-key cooldown, and backwards-clock behavior.
- Source audit `tools/check_v201_engagement.py` inherits V200 brand checks and asserts opt-in, permission, channel and anti-spam rules.
- Require CI build, lint, APK identity/signature, full inherited emulator regression, and physical install/in-place upgrade.
- Device QA: with nudges off, no discretionary notifications; with nudges on, test 9:00 and 20:00 boundary; custom quiet hours; disabled channel/permission; repeated keys; actual reminders unaffected.
- This **draft** does not authorize merge or promotion to stable until explicit creator acceptance of V200 and V201 builds.

## Next phases, not in scope
- P2: differentiated creator journeys/copy that reference verifiable in-app state.
- P3: contextual tap deep-links and optional per-category control, without tracking/remote services.
- P4: notification fatigue testing, accessibility, and final stable gates.

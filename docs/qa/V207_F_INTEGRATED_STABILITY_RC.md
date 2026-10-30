# Backlot V207-F — Integrated stabilization release candidate

**Identity**: versionCode 231, versionName 2.8.1-v207-stability-rc.
**Scope**: consolidate all V207 A–E work in one installable branch, without new features, rewriting creator data or merging into stable.

## Integrated acceptance areas
1. **Fullscreen Settings**: no Home leakage at top or bottom, safe areas correct across gesture/3-button navigation, font scale and IME, no Back regression.
2. **Project voices**: quick new, advanced new, existing project edit; compact selected voice, preview without saving; full eight-persona on-demand list; inherited/global defaults unchanged; saved/reminder TTS works.
3. **New user setup**: three pages, required creator mode/platform, visible default primary goal, valid profile, optional account, explicit cloud restore/keep-device choice for returning accounts, denied permissions do not block first run.
4. **Guide**: four lessons, real navigation target coordinates, no mandatory creation, Back does not silently complete, skip and replay respect current settings, large text and small phone.
5. **Revenue Insights**: owner-specific read-only monetary scope, verified channel summary, genuine zero vs unavailable, top-video format results disclosed as sampled, expired permission and cached/failed refresh states, no claims of finalized AdSense payouts.
6. **Legacy branding**: approved launcher, splash, status-bar monochrome icon, system aliases, widgets, 3-second welcome; retired drawables excluded from signed APK.
7. **Reminder safety**: projects and schedules unchanged; alarm, voice, snooze, unanswered follow-ups, optional seven-day nudge pause, quiet hours, notification actions, recovery after reboot.
8. **Durability and speed**: navigation from story/detail back to Home, content workspace, calendar, Insights lists and thumbnails, persistence after process death, local backup/restore and cloud conflict flow.

## GitHub branch / release gates
- V207 A/B PR #82, C PR #83, D PR #84, E PR #85 are **stacked** on one ancestry chain. V207-F is stacked on E: it therefore contains all changes, not an unreviewed merge of unrelated lines.
- CI must validate **exact V207-F commit**: all historic source scripts plus consolidated `tools/check_v207_integrated_stability.py`, Gradle JVM unit suites and lint, signed personalStable APK, app ID/version/retired resources check, all inherited Android emulator regressions, and A–E UI tests.
- Verify personalStable signing SHA identity equals previous creator preview; do not change applicationId. Reinstall **over** an existing Backlot preview, never uninstall first.
- For upgrade testing on actual Samsung, capture before/after count of projects, drafts, ideas, reminders, creator settings, backup status and connected YouTube channel. Check exact reminder times and voice labels.
- Fresh-install test on second/clean emulator: create profile with no Google account, skip permission requests, complete guide or exit and replay. Then create an actual project, grant only needed reminder permissions.
- Revenue test on live owner-authorized channel: verify date ranges 7D/28D/90D/Month, refresh error and offline cache; compare summary total to YouTube Analytics for the same currency/date window. Never infer missing daily rows are zero.
- Confirm Samsung status bar icon from **newly created notification**; an old pre-upgrade Android notification may retain the previous small icon until dismissed.
- New code stays draft until real-device approval and explicit stable-promotion decision. CI success is not proof of physical device UX, real permission dialogs or live YouTube API authorization.

## Not yet verified
- Physical Samsung in-place upgrade and device-specific status-bar/inset screenshots.
- End-to-end Google consent/live Revenue accuracy and real reminder delivery across Doze/reboot.
- Backup restore/upgrade continuity on user data without destructive wipe.

## Stop-ship conditions
Any compile/lint/emulator failure, app ID or signing drift, creator data loss, permission/consent regression, notification or TTS failure, invalid revenue reporting, Home bleed-through, broken guides, inaccessible text, or confirmed old-brand source rendering means **do not merge to stable**.
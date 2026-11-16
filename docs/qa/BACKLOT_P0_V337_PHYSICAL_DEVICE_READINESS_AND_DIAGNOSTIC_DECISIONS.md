# Backlot P0 V337 — physical acceptance and Alert Health diagnostic decision gate
Date: 2026-11-16

## Source of truth
- Owner-tested stable V337: `stable/backlot-v337-premiere-owner-testing-20261114`
- Stable SHA: `678502f4626150ea1945b81eed1593a362377454`
- PRs #379–#383 remain **draft, unmerged** and serve as independent recovery and Android 15 emulator evidence.
- The branch holding this document is **QA-only**; do not ship, merge, deploy, or install its test APK on a user's phone.

## Meaning of the new diagnostic test

The instrumentation explicitly characterizes two source-code concerns on a **disposable Android 15 emulator**:
1. Earlier `VOICE_SPEAK_STARTED` can outrank later `VOICE_SPEAK_FAILED` inside `Phase1HabitDeliveryEvidencePolicy.health`; this can display "speaking" even after a recorded failure.
2. `getNotificationChannel(kind)?.importance != IMPORTANCE_NONE` returns true when the Android notification channel is **missing**, potentially displaying "Allowed" for a missing channel.

A **green diagnostic characterization run means the discrepancy was reproduced**, not that functionality is correct. These tests intentionally assert current V337 misclassification. Before any approved fix is integrated, replace them with contract tests that demand `VOICE_UNAVAILABLE` for a later voice failure and `channelAllowed == false` when a channel is missing. Do not turn characterization tests into normal release gating.

## Real-device acceptance: not yet executed

**No automatic physical-device access exists from this GitHub/CI workflow.** Physical sign-off requires an actual tester with an Android phone. Use an approved V337 owner-test build already installed, or separately approve a safe test-build delivery. Never install an experimental CI debug APK over the founder's installation.

**Test setup / safety**
- Record Android version, make/model, OEM UI version, battery-optimization status, exact-alarm access, app notification permission, and notification-channel status.
- Avoid changing permissions on a primary device with important ongoing reminders. Use a secondary/test handset for destructive or permission-denied scenarios.
- **Never** run `adb shell pm clear`, uninstall, or wipe data on the owner's primary phone. Do not request backup/restore flows—these are excluded from approved V337 settings.
- Use disposable test challenges; do not export activity names, personal notifications, photos, tokens, account IDs or diagnostic ledger contents. Redact screenshots.
- For every case record: scheduled clock time, actual observed time, sound/voice result, screen locked/unlocked, banner/full-screen state, evidence screenshot/short clip (redacted), and whether the user explicitly pressed Done.

## Physical acceptance matrix (owner/tester to execute; all currently OPEN)
| ID | Scenario | Pass condition | Notes |
|---|---|---|---|
| PH-01 | First launch; notifications and exact-alarm access granted | Readiness reflects actual Android settings; no unexpected requests or crashes | Observe only |
| PH-02 | 21-day challenge ALARM, app foreground, unlocked | On-time alert, visible notification, no automatic Done | Allow small OS variance |
| PH-03 | Challenge ALARM with screen locked and app backgrounded | Actual notification/full-screen behavior documented; alarm audible if system permits | Device/OEM-specific |
| PH-04 | Challenge VOICE on locked screen | Real spoken words intelligible; three-repeat policy observed if configured; no false Done | Audible human verification required |
| PH-05 | Challenge VOICE with background music | Voice and music duck/mix and stop controls correct | Only test features present in V337 |
| PH-06 | Snooze challenge alert | One later alert, correct snooze duration, no duplicate/phantom Done | Document actual time |
| PH-07 | Device reboot with future challenge | Alarm re-arms automatically without manual Backlot launch | Only on spare/test handset |
| PH-08 | Natural battery optimization / longer idle | No unexpected silent failure; actual OS/OEM behavior recorded | Multi-hour gate |
| PH-09 | Exact permission denied/re-granted | Honest exact/inexact warning and eventual timing tested without claiming exactness | Spare phone only |
| PH-10 | Notifications denied/re-granted | Android blocked state clearly reported; no false `NOTIFICATION_POSTED` or Done | Spare phone only |
| PH-11 | Edit/pause/archive/delete challenge | No obsolete or duplicate OS alert after durable changes | Use test records |
| PH-12 | 21st/22nd day boundary, timezone change | No day-22 alarm, no wrong-day Done, date semantics preserved | Isolated test account/phone |

Any failure must be recorded with reproduction steps and categorized **confirmed device failure / settings restriction / unconfirmed delivery / tester did not see notice / harness limitation**. Do not silently 'fix' source or modify stable branch.

## Release decision and explicit authority
- **Verified on emulator**: awake actual timed alarm; real Alert Health panel; reboot recovery; short forced deep Doze; permission-denied source fallback and blocked notifications.
- **Not verified**: OEM physical delivery, long idle, audible VOICE, custom music, headless installed-owner-device behavior, and complete permission recovery.
- **Decision still needed**: founder authorizes or rejects narrow production fixes for the two reporting discrepancies; founder separately authorizes device-test APK/integration or retains current stable build.
- Keep PRs draft/unmerged and V337 frozen until evidence, owner approval and signed release criteria are complete.

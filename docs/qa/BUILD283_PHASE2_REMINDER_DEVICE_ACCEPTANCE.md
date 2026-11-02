# Build 283 Phase 2 — Real-device Reminder Reliability Acceptance

**Current gate:** OPEN. CI emulator checks cannot certify actual OEM background behavior.
**Test package:** non-debuggable preview, versionCode 283, package `com.framebynavin.app`. Keep the owner's current installation if signer does not match; **never uninstall as a test workaround**.
**Safety:** Export Creator and Personal backups before any testing. Never sign out, clear app storage, factory reset, or erase cloud data as part of Phase 2. Use a disposable device/test profile for destructive tests. Capture device model, Android version, app version, permission state and actual result for each case. No real account/phone data in GitHub logs.

## Device matrix

- Android 11 API30 emulator automated compatibility suite (not OEM acceptance)
- Android 15 API35 emulator automated compatibility suite (not OEM acceptance)
- Android 14–16 physical Android phone, ideally Samsung with One UI battery restrictions, plus a second device/brand if available
- Android 16/API36 physical or emulator compatibility check before Play submission

## Structured manual cases

| ID | Scenario | Steps | Must pass |
|---|---|---|---|
| R01 | Personal Voice foreground | Create a new Personal VOICE reminder 3 min ahead, set repeat=3 and default piano; keep app open | Audible speech repeats 3 times, music is clearly audible; Done stops audio; no later repeat |
| R02 | Creator Voice foreground | Set a disposable Creator project's reminder to VOICE, 3 repeats and default audio | Audio is intelligible; playback ends; tap Done and verify no re-alert |
| R03 | Personal Alarm background | Schedule Personal ALARM; go Home, turn screen off | Notification/alert arrives as OS allows; Done/Snooze actionable, audio stops; no duplication |
| R04 | Creator Alarm background | Schedule Creator ALARM, lock phone | Correct project title/action, respects permission state, no inadvertent Creator project completion |
| R05 | Custom music mixing | With granted media access pick custom audio; repeat R01 and R02 | Song audible before, during and after speech; adjustments are perceptible; missing file falls back cleanly |
| R06 | Bounded automatic retry opt-in | Enable Personal and Creator audible retry for 5 min, max 1 extra attempt. Let playback finish without Done | Exactly one eligible retry at permitted time; no uncontrolled looping |
| R07 | No retry after Done | Acknowledge during first playback before scheduled retry | Scheduled old retry is cancelled; never sounds later |
| R08 | Snooze and old token | Snooze once, then interact with previous notification if it remains | Only new snoozed occurrence is actionable; previous action does not complete/snooze newer occurrence |
| R09 | Automatic retry off | With OFF (default) let the original audio finish unanswered | No automatic audible replay |
| R10 | Reboot recovery | Schedule reminders at least 15 min into future; restart device, wait for OS startup and unlock | Eligible reminders recover without duplicate firings; log any OEM delay |
| R11 | Timezone and clock changes | On disposable device only, change timezone; verify eligible recurrence | Schedule follows intended wall clock; no surprise duplicate old reminder |
| R12 | Exact alarm denied | Deny Alarms & Reminders special access where system permits, schedule reminder | App explains limitations; fallback notification works where possible; no crash or false success |
| R13 | Full-screen denied | Disable full-screen intent access where available, lock screen | Usable high-priority notification fallback; no claim that full-screen always works |
| R14 | Notifications denied | Deny POST_NOTIFICATIONS, create reminder and reopen settings | Clear explanation of missing notifications, safe recovery after regrant, no crash |
| R15 | Battery limits / Doze | Run R03/R04 with normal and OEM restricted power settings separately | Capture what is actually delivered vs delayed; do not promise reliability under forced restrictions |
| R16 | App update | Install same-certificate Build 282 → 283 preview in-place on a test profile | Creator tasks, Personal records, completion history and settings preserved |
| R17 | Audio interruption | Incoming call, audio focus loss, silent/DND mode (where test permits) | No stuck foreground service, audio releases resources and obeys platform settings |
| R18 | Accessibility | Screen reader, 200% font scale, landscape/small display | Done/Snooze legible and actionable; full-screen controls reachable |
| R19 | Cloud restore after device change | Sign in on a second disposable profile, sync and restore both domains | All declared synced fields match; absent custom music and original media explicitly disclosed |
| R20 | Force-stop limitation | Force-stop app manually, verify OS behavior only, then reopen | Accurately state that Android/OEM restrictions may suppress alarms until re-open; no unjustified reliability guarantee |

**Do not certify R01–R20 without real observations.** The Android UI, settings routes and policies differ by device. This checklist is a test plan, not a passing test log.

## Evidence record (copy per executed test)

- Case ID:
- Device / Android API / OEM build:
- Installed APK SHA256 and signer fingerprint:
- Exact reminder type, scheduled date/time and permission state:
- App state (foreground, background, locked, rebooting, force-stopped):
- Expected result:
- Actual result and observed local timestamp:
- Outcome: PASS / FAIL / BLOCKED (never assume PASS)
- Sanitized screenshot / logcat pointer:
- Follow-up issue and severity:

## Phase 2 stop-ship criteria

Any unacknowledged duplicate audible retry, late stale-token response changing a newer event, irreversible data loss, critical crash/ANR, wrong-domain notification leak, or silent failure without a usable fallback is P0/P1. No Play production submission until resolved.

## Automated CI scope

Draft PR #247: `V283Phase2ReminderSafetyDeviceTest` tests with `HardeningTestEnvironment.requireCiEmulator()` on Android API 30 and API 35; no actual audio output or full-screen acceptance is inferred from these cases. In-place data preservation is tracked separately in PR #246.

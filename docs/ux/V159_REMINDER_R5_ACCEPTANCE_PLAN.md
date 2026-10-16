# Backlot V159 — Reminder acceptance R5

**Branch:** `feature/backlot-v159-reminder-acceptance-r5`  
**Base:** V158 R4, commit `108e048f876bc937f71ff2138604c3c0a2e4dcc0`  
**Release state:** review/preview only until emulator and real-device evidence is obtained.

## Scope
R5 does not redesign the reminder experience or change its persisted schema. It replaces unproven build-only assurances with measurable Android runtime, notification channel, recovery, action and accessibility tests across platform versions.

## Evidence categories

| Area | Automated checks | Physical-phone gate |
|---|---|---|
| R1 safe recovery | V155 occurrence-token tests + V159 schedule reconciliation, ledger integrity and duplicate claims | Reboot, change time, force-stop/reopen while alert shown, screen off |
| R2 reminder choices | V156 full/quick chooser with stable clickable tags and Compose synchronization | Create/edit all five plans and custom times on phone |
| R3 direct controls | V157 Reminder Center UI, V159 two-task cancellation/action isolation | Snooze a future vs overdue reminder; change time; pause; turn off; multiple reminders |
| R4 delivery | V158 check-in UI; V159 real NotificationManager quiet/loud channel checks | Voice/alarm audibility, lock screen, vibration, battery savers, TTS unsupported language |
| Accessibility | V159 Compose forced 200% font scaling and scroll-reachable essential actions | TalkBack, 100/130/200% fonts, landscape, screen readers |
| Upgrade and settings | V158 settings JSON import/export, legacy missing quiet-hours data | Install V159 over V158 with same signing identity and compare persisted projects/settings |
| Android coverage | Android API 30 (Android 11) and API 35 (Android 15) emulator suites | At least one actual Android device; newer/older OEM variants if available |

## Previous failed/cancelled verification
- V156 emulator job `38860197096`: crashed before executing any test; **not** a pass.
- V157 emulator job `38863518631`: 17 tests ran, with one V156 reminder-plan selector assertion failure (`URGENT` vs `CUSTOM`); **not** a pass.
- V158 emulator job `38866567346`: cancelled during device tests; **not** a pass.
- In R5, full/compact reminder plan cards expose stable Compose test tags. The V156 test clicks the actual card, scrolls it into view and waits for Compose idle before assertions.
- R5 CI uses independent Android 11 and Android 15 emulator jobs and retains device test reports on failures.

## Release gates
1. PASS: Source preservation check covering V154–V158.
2. PASS: Full JVM unit suite.
3. PASS: Android lint.
4. PASS: Instrumentation test compilation.
5. PASS: APK assembly and checksum/ZIP integrity.
6. PASS: Android 11 and Android 15 emulator suites — no ignored/failing tests.
7. PASS: Physical-device acceptance checklist signed off by creator, or remain PREVIEW / device-QA-pending.

## Physical-device acceptance checklist

**Standard reminder delivery**
- [ ] On the phone, schedule a Notification, Voice, Alarm and Smart reminder at known future local times.
- [ ] Repeat with app foreground, background, and screen locked. Record expected/actual arrival time (no invented SLA).
- [ ] Compare notification permission granted/denied and exact-alarm permission granted/denied.
- [ ] Test default battery policy, battery saver and OEM battery optimization. Document any system limitations.

**Actions and lifecycle**
- [ ] On active alarm/voice check-in: Step Done (when allowed), I'm Working, Snooze, More Options, Dismiss, Pause and Change Time.
- [ ] Resume app while alert visible: action buttons remain valid.
- [ ] Same project cannot accept duplicate taps; another project remains independently actionable.
- [ ] Smart final target never starts a new late ringing chain; a bounded catch-up notification is visible when relevant.
- [ ] New Project still defaults to seven-step wizard; Quick Create is separate and all five plans accessible.
- [ ] Reminder Center classifications and direct Snooze/Pause/Change Time/Turn Off work, including a reminder due tomorrow.

**Quiet hours and speech**
- [ ] Quiet Hours off by default; set 10 PM–8 AM. Normal/gentle notifications discreet but still accessible.
- [ ] Explicit Voice/Alarm and later Smart stages still interrupt when selected (with user awareness).
- [ ] Missing system speech language triggers actionable notification fallback; test with temporary device TTS settings.

**Data and accessibility**
- [ ] Install V159 over existing V158 (same signer) and confirm projects, reminders, settings, voice persona and backups still intact.
- [ ] Restart phone, adjust timezone/date, restore original settings; no duplicate or missing project.
- [ ] Run 100%, 130%, 200% font scale, TalkBack and landscape; primary and secondary controls reachable.

## QA evidence sheet

For each case record `device/OEM`, `Android version`, `battery optimization`, `notification/exact/fullscreen permissions`, `plan/mode`, `local scheduled time`, `actual time`, `outcome`, `screenshot or log` and `notes`.

**Do not claim production reliability from emulator tests alone.** Future release approval requires actual phone results, especially under Android background restrictions.

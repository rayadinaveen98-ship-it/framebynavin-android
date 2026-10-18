# Backlot V182 — Reminder Redesign Phase 6: End-to-End Reliability & Sign-Off

**Stack:** V177 acknowledgement → V178 speech/alarm lifecycle → V179 bounded quiet follow-ups → V180 controls → V181 inbox/history → V182 reliability.

**Candidate status:** Draft debug preview. **Never call this stable without passing emulator and physical-device acceptance.** The main/stable branch is not being merged by this phase.

## Reliability fixes in V182

1. **Archived project is non-actionable everywhere.** The existing authoritative ReminderActionSafety check is now reused in Regular/Smart broadcast delivery, scheduling, app-resume/boot recovery, and missed notification catch-up. Previously these pathways checked DONE/SKIPPED but sometimes omitted archived status. Archived task's one-use action token is invalid and background alarms are retired.
2. **Follow-up lifecycle cleanup.** An explicit project reminder cancellation/reschedule and recovery of inactive/archived projects cancels pending V179 quiet check-ins. Workers also revalidate authoritative task state, current occurrence token, response and global follow-up preference when executing.
3. **No unexpected notification ping after speech/ringing ends.** The V178 end-of-playback and screen-watchdog handoffs now explicitly use a discreet, low-importance channel, retaining the same token and actions. An original opted-in audible alarm/voice still works as configured.
4. **Independent Android notification channels.** The permission gate now checks the channel actually selected for posting. If the loud channel was blocked but the quiet channel is enabled, a quiet follow-up may post. If the quiet channel was blocked, a quiet follow-up is not silently sent through the loud channel.
5. **No false actionable tokens after a denied notification.** A notification's channel is checked *before* issuing a fresh occurrence; previously an undeliverable notification could still rotate a valid token.

## Automated acceptance matrix
- V161–V181 inherited source, project-data and reminder safety gates.
- V182 source-policy guards for archive, token, quiet channel and background worker cleanup.
- JVM unit suite including V182 regular and Smart archived-delivery/expiry regression tests.
- Android lint, androidTest compilation and assembled APK.
- Android 11 emulator: V182 actual low-importance channel notification with original token + response precedence; archived recovery/no catch-up; V181 unresolved inbox actions; V180 timing picker; V179 at-most-two claim; V178 voice/alarm handoff; V177 ack states; existing V159 action isolation and V155 recovery isolation.
- All steps must have an explicit successful result. Compiling instrumentation code is not an emulator pass.

## Manual physical-phone test plan — required for production sign-off
- **Signing/data:** Backup user data, confirm package ID and release signing certificate match before in-place upgrade. A debug APK signed differently may fail to install over existing data. **Never uninstall a data-bearing working installation.**
- **Notification:** Set a real short-delay notification, ignore it, verify pending state; accept Snooze/Working and verify record transitions. Test locked/unlocked phone and Android notification permissions.
- **Voice:** Test 1, 2 and 3 repetitions, speech gaps, missing/unavailable TTS, Bluetooth/headset, background restrictions, user action at final utterance and wake-lock release.
- **Alarm:** Test configured 30s, 45s, 120s and 300s, vibration/sound release, response just before/after timeout, missing sound URI, full-screen launch limitations, and no immediate extra loud ping on timeout.
- **Smart:** Confirm SOFT→VOICE→ALARM→CRITICAL progression only as configured; no quiet follow-ups before final stage and at most two afterward.
- **Quiet channel:** Disable regular channel only; verify quiet follow-up can post when quiet channel remains enabled. Disable quiet channel only; verify no sound-channel fallback for follow-ups, and correct unresolved state.
- **No response:** Let a reminder remain unanswered through its two deferrable WorkManager follow-ups; verify no infinite loop, duplicate notifications or new voice/alarm after missed follow-ups.
- **Cancel/edit/archive:** Archive or cancel a project with pending alarm/quiet check-in, reboot and open app; verify no stale reminders. Edit title/notes without disturbing unrelated progress. Change the global follow-up switch while a job is waiting.
- **Recovery:** Boot, timezone/clock change, Doze, battery saver, app force-stop, exact-alarm permission denied, notification permission denied, task termination, and multiple overlapping reminders. Android may delay or block deferrable work; do not claim guaranteed delivery.
- **Reminders Center:** Current unanswered occurrence is actionable, superseded tokens are historical/read-only, delivery failures are not falsely called ignored, buttons operate at most once, accessibility labels and small-screen layout pass.
- **Privacy:** No titles/notes/audio in acknowledgement/claim journals; local creator backup/restore with settings. No silent project completion or reward for dismiss-only actions.

## Release gates

| Gate | Approval needed |
|---|---|
| All inherited source and JVM tests | CI success |
| Android lint, APK assembly and checksum | CI success |
| Instrumentation on Android 11 | Emulator test success |
| Real-device Android 11/13+/14+ and OEM timing/audio | Manual sign-off |
| Compatible signing and in-place upgrade without data loss | Manual sign-off |
| Creator approval | Explicit approval |

**Phase 6 complete means implementation and automated verification only if CI actually passes.** Stable release is a separate decision; any incomplete gate remains pending.

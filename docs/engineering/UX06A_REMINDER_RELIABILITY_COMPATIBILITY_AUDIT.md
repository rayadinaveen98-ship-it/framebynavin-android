# Backlot UX06A — Reminder Reliability and Compatibility Audit

**Date:** 2026-11-17
**Classification:** SOURCE INSPECTION + PROPOSED IMPLEMENTATION CONTRACT. No production code changed.
**Reviewed cumulative source:** UX05.1E draft PR #402, head d9a4ef8271c34947ce238024dc31b9413bf47870.
**Protected owner-tested baseline:** stable/backlot-v337-premiere-owner-testing-20261114 (read-only).
**UX06A stage:** documentation gate only. These findings are not build/test/device results.
**Founder-approved sequencing:** Stage source improvements without intermediate comprehensive test runs; consolidated build, CI, QA and real-device certification after UX10. Do not merge or release early.

## 1. Scope and boundary

Reminder UI, policy, persistence, audio, Android manifest, notification and broadcast entry points were inspected directly from the cumulative source, with targeted V337 comparisons. Android exact alarm / full-screen policies checked against official documentation in November 2026.

**The four domain owners remain separate:**
- Creator projects: CreatorTask and TaskStore, ReminderScheduler, SmartEscalationScheduler, ReminderReceiver / EscalationReceiver, ReminderActionReceiver, VoiceReminderService and AlarmRingingService. Project Pulse stage and publication checks must remain authoritative.
- Personal: PersonalReminderStore, PersonalDeliveryCoordinator, PersonalReminderReceiver, PersonalReminderActionReceiver, PersonalPlaybackService. Only a user-confirmed Personal response constitutes Done.
- Single-activity 21-day Challenge: Phase1HabitStore, Phase1HabitAlarmScheduler, Phase1HabitAlarmReceiver / Phase1HabitVoiceService, challenge completion and action policies. No routine notification is a completion receipt.
- Ideas: Idea reminder metadata, IdeaReminderScheduler, IdeaReminderReceiver, IdeaReminderActionReceiver. Lightweight in-app and notification actions, not a CreatorTask.

Do not merge those stores, remove migration/backup adapters, change app ID or active alarm identifiers, or copy sensitive Creator/Personal data between them. UI and audio *capability contracts* may be shared; canonical write ownership may not.

## 2. Verified current inventory

| Component | Existing source behavior | Primary location |
|---|---|---|
| Creator reminder type | NONE, SIMPLE, VOICE, ALARM, SMART | data/CreatorTask.kt |
| Creator attention strategy | OFF, LIGHT, GUIDED, URGENT, CUSTOM; preferences AUTO/NOTIFICATION/VOICE/ALARM/SMART | data/CreatorTask.kt; ui/V156ReminderSetupUi.kt |
| Creator delivery | Valid current alarm ledger, action token, exact or inexact fallback, late audible suppression, notification fallback | reminders/ReminderScheduler.kt; ReminderReceiver.kt |
| Smart | SOFT, VOICE, ALARM, CRITICAL, stage eligibility by priority, bounded final target | reminders/SmartEscalationScheduler.kt; ReminderDeliveryPolicy.kt |
| Creator user responses | STAGE_DONE, STARTED, SNOOZE, PAUSE, RESCHEDULE, DISMISS; legacy DONE is dismiss alias | reminders/ReminderActionReceiver.kt |
| Personal cadence | ONCE, DAILY, WEEKDAYS, WEEKLY, MONTHLY, EVERY_N_DAYS, WEEKENDS, EVERY_N_HOURS | personal/PersonalReminder.kt |
| Personal response | DONE, DISMISSED, SNOOZED, RESCHEDULED; atomic stored occurrence and token checks | personal/PersonalDeliveryCoordinator.kt |
| Personal retry and follow-up | Explicit opt-in retry; default-off gentle follow-up with quiet hours | personal/PersonalDeliveryCoordinator.kt; ui/V2232PersonalReminderHub.kt |
| Single-activity challenge | One saved activity over 21 days; optional QUIET/VOICE/ALARM daily event; explicit progress; Strict/Flexible | personal/Phase1HabitStore.kt; Phase1HabitLiveDelivery.kt; ui/Phase1HabitSharedUi.kt |
| Older V297 21-day planning | 21 independently editable activities, with import/export; does not schedule notifications automatically | personal/V297Challenge.kt; ui/V297ChallengeScreen.kt |
| Idea reminder | Off / 3h / 6h / Tomorrow / Daily / Custom; notification Open, Make Project, Snooze 3h, Stop | ui/V144IdeaReminderUi.kt; reminders/IdeaReminderSystem.kt |
| Visual scenes | Seven saved Voice/Alarm scene choices, independent of Creator/Personal; opt-in experimental Premiere | reminders/V391ReminderSceneEngine.kt; V394PremiereExperience.kt |
| Recovery | Boot, package replacement, time/timezone changes, exact-alarm grant + low-frequency health reconciliation | reminders/ReminderRecoveryReceiver.kt; ReminderRecoveryEngine.kt |

## 3. Source-confirmed compatibility defects

### UX06A-F01 — Creator 4–5 voice repeats truncated in Intent-reconstituted task [P0]

**Evidence:** UX03 cumulative Voice controls offer counts 1–5 in ui/V180ReminderTimingControls.kt; VoiceReminderService now applies V403VoiceRepeatPolicy.count to 1–5; however, the shared Intent.toTask helper in reminders/AlarmRingingService.kt still executes:
`voiceRepeatCount = getIntExtra(EXTRA_VOICE_REPEAT_COUNT, 3).coerceIn(1, 3)` (line 440).

VoiceReminderActivity obtains its task with intent.toTask(); that object is used for full-screen action/replay paths. Regular service startup from the authoritative task can support 1–5, but not all reconstructed paths represent selected settings faithfully.

**V337 baseline:** Voice UI and service were 1–3; the three-repeat clamp was previously consistent. This is a **new cumulative UX03 compatibility gap** and must not be claimed as a V337 regression.

**Proposed minimal correction:** replace hard-coded 1..3 reconstruction with V403VoiceRepeatPolicy.count and import the shared policy. Keep persisted 1–3 records unchanged, default 3 unchanged, repeat counts never auto-increase. Audit all putTask/toTask, notification, service replay, timeout/watchdog, test preview and recovery serialization; don't change alarm scheduling or source of truth.

**Fail-closed tests after UX10:** counts 1, 2, 3, 4, 5; invalid -1, 0, 6, 999; missing extras, process/task rehydration; VoiceActivity re-entry and one-time Replay (one utterance); TTS callback duplicates, late completion, cancellation, fallback, language missing, five 45-second sentences, four 60-second gaps; acknowledge exactly one occurrence despite multiple spoken cycles.

### UX06A-F02 — Settings displays 20m Creator snooze, but persisted policy rejects it [P1]

**Evidence:** ui/FrameByNavinV101BApp.kt global Creator Settings presents chips 5/10/15/**20**/30 minutes (around line 1825). data/CreatorOsSettings.kt::setSnoozeMinutes calls V280ReminderSnoozePolicy.effectiveCreatorMinutes(0, value). reminders/V280ReminderSnoozePolicy.kt permits only 5/10/15/30; invalid 20 falls back to 10. Reopening Settings therefore shows 10m, not the user's selected 20m.

**Proposed minimal correction:** remove the invalid 20m option from Creator global UI and use the single canonical choices collection, unless owner separately approves adding 20m end-to-end (all persistent old/new Personal and Creator actions, policy, scheduled Intents, test fixtures, migration). Retain existing 5/10/15/30, default 10. Do not silently change previously valid values.

**Tests after UX10:** all displayed choices persist and survive process restart/backup; old raw 20 fails safely to 10 with disclosure if needed; invalid/missing preference never misrepresented as saved; Creator per-project 0=inherit maps to real global value; direct Snooze from full-screen, hub and notification uses shown minutes.

## 4. UX/UI behavior inconsistency requiring explicit semantics

### UX06A-F03 — Smart Snooze is not the same operation in the Hub and live alert [P1 design contract]

**Observed paths, not evidence of a runtime bug:**
- Creator Reminder Center's card calls CreatorViewModel.snoozeReminderFromCenter, which moves the reminder's scheduled target forward after validating the new Smart window and deadline. The current card text says snoozing moves the final Smart target.
- A live Smart stage Snooze goes through ReminderActionReceiver. For Stage check-ins it preserves the configured final target and uses SmartEscalationScheduler.snoozeStage to repeat the current stage within the bounded target.

**UX risk:** Both places display a generic Snooze even though they mean different operations. This is a semantic inconsistency, not proof either policy is wrong.

**Proposed decision:** preserve both existing semantics in UX06A, but distinguish them in copy. For a future Smart target, label 'Move target +Xm' with a visible resulting target time; for a live stage label 'Remind me again in Xm' and indicate that the final deadline is unchanged. Do not silently change either policy. Document any choice to unify as a separate behavior change requiring founder approval. Validate deadline and bounded Smart window before confirming.

## 5. Parity gaps and architecture constraints (not certified defects)

- **UX06A-G01 [P1]:** Single-activity challenge Voice uses a separate TTS foreground service, fixed repeat count / 5-second gap, and no per-challenge Piano/CUSTOM/OFF music choice in the current model. Propose parity in UX06D, but no implicit addition of a paid service or media upload.
- **UX06A-G02 [P1]:** Challenge ALARM is currently represented by a high-priority notification channel with default alarm sound; unlike Personal/Creator, it does not use an equivalent dedicated AlarmRingingService with configurable sound timeout. Label sound and full-screen capabilities accurately; design/approve desired parity rather than claiming it exists.
- **UX06A-G03 [P1]:** V394 Premiere for CHALLENGE is explicitly sample-only and automaticChallengeScreen() = false. Do not quietly make ordinary challenge reminders launch full screen or expose private details above keyguard.
- **UX06A-G04 [P1]:** Existing seven V391 scene values are stored by enum name and separately for VOICE and ALARM. A five-scene UX06C redesign must preserve old preference IDs, default Classic, reduced-motion choice and background-only input isolation.
- **UX06A-G05 [P1]:** Voice/Alarm and Personal use separate playback and foreground-service implementations. A shared display must not infer audible start success from ContextCompat.startForegroundService success; TTS initialization, language packages, media URI availability, audio focus, DND and FGS restrictions can fail later.
- **UX06A-G06 [P1]:** Creator alarm and voice scheduling catch/ledger failures, but UI should not display 'Guaranteed' or claim precise delivery from a mere successful save. Status taxonomy: configured, armed, fired, posted, speaking/ringing, stopped, responded, unanswered, degraded, blocked, unknown.
- **UX06A-G07 [P1]:** Personal lockscreen privacy should remain generic for locked sensitive content; notification previews and all new scenes must not disclose titles/photos/notes over keyguard without reviewed rules.
- **UX06A-G08 [P1]:** Local custom music uses content:// picker URIs; moving/deleting the underlying file or restoring to another device may break playback. Read permission/lifecycle and fallback behavior require verified physical test; don't promise copied media bytes.
- **UX06A-G09 [P1]:** Legacy V297 planning and new single-activity challenge are distinct. Don't migrate or destroy legacy completion receipts under a renamed navigation label.
- **UX06A-G10 [P2]:** Settings, composer, Reminder Center, full-screen alert and the Activity Center can all expose reminder actions; UX06B must unify *meaning*, not indiscriminately add identical buttons to every screen.

## 6. Android constraints verified against official platform guidance

- Android 14+ may deny SCHEDULE_EXACT_ALARM by default for new eligible installs. Always check canScheduleExactAlarms before setExactAndAllowWhileIdle or setAlarmClock, schedule explicitly labeled inexact fallback, and recover when permission is granted. Android docs: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- Full-screen intent access for non-calling/non-alarm apps is restricted by Android 14 and Play policies; user can deny or grant via settings. Read NotificationManager.canUseFullScreenIntent; do not promise auto-launch, especially for quiet Personal/Challenge. Android docs: https://developer.android.com/about/versions/14/behavior-changes-14
- Starting foreground services from background is restricted; alarm-driven and user-requested cases may qualify only under documented exceptions. Handle SecurityException and ForegroundServiceStartNotAllowedException and maintain actionable notification fallback. Android docs: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Manifest declares POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, USE_FULL_SCREEN_INTENT, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, FOREGROUND_SERVICE and MEDIA_PLAYBACK. Existing declarations do **not** prove permission or operational delivery.
- Future Play submission must separately review whether selected alarm uses meet permission and full-screen policy; do not assert approval simply because an APK is locally installable.

## 7. Design + technical invariants (must be preserved)

1. Only the originating domain store writes its record. No cross-domain Creator/Personal/Challenge conversion or deduced completion.
2. Explicit response is the only source of Done/Stage Done; delivery/timeout/absence of response never count as achievement.
3. Stage Done must pass Project Pulse stage and publication guards, existing durable reward recovery and current action token; legacy Creator DONE is dismiss-only.
4. Every OS alarm, notification, full-screen interaction and auto retry validates current ID, occurrence time, token, status, due/deadline, pause/restore state, current generation where applicable.
5. A rejected/stale action must not stop a newer alert or overwrite newer acknowledgment. Post-commit rearm failures must be reported as 'saved; delivery needs attention' rather than false unsaved UI.
6. A manually snoozed event re-arms a single valid occurrence according to its domain; auto-retry is off by default, bounded and cancels on explicit owner response.
7. Recovery after reboot, package update, time-zone/time change and permission changes may re-arm but never double complete or replay acknowledged audio as a fresh event.
8. Notification suppression, DND, denied full-screen or exact-alarm access must degrade honestly. No alert is ever 'guaranteed'.
9. Audio and visual layer cannot process Done, mutate schedules, or hide controls with animation; reduced-motion and low-power modes must remain supported.
10. Creator + Personal + Challenge + Ideas backup paths, archived plans, owner completion histories and current V337 stable backup bytes must not be altered by this stage.

## 8. Planned UX06A implementation boundaries (PROPOSED; not performed)

**UX06A-1: Minimal compatibility fixes** (1–3 production files; separate implementation authorization):
- AlarmRingingService.kt: shared repeat-count policy in Intent.toTask.
- FrameByNavinV101BApp.kt: global snooze chips from canonical V280 choices (preserve default and ordering).
- Any focused policy test fixture only if required by test-only source changes; no data migrations unless identified.

**UX06A-2: Behavioral labeling** (small UI scope):
- Distinguish moving future Smart target vs snoozing current live escalation; avoid misleading one-word Snooze.
- Keep current Smart scheduler, store, policies and Android behavior unchanged.

**UX06A-3: Capability/readiness contract** (no premature scheduler abstraction):
- Define common read-only ReminderSurfaceSnapshot and delivery health semantics for UX06B/UX06E; do not move domain writers.
- Identify stable Android-only audio capability checks. UI can show 'Precise timing unavailable' rather than optimistic success.

**Explicitly excluded from UX06A:** New full-screen art/theming, 21-day music persistence, creator Smart redesign, remote sync, backend, new permissions, migration, broad Kotlin refactors, background hosted service, payments, APK, merge/release.

## 9. Deferred evidence and acceptance

Full tests after UX10 per owner's request. **No such tests have been executed during UX06A**:
- Deterministic JVM tests: Creator 1..5 voice parcel/Intent/task reconstruction and notification/replay; 20m setting rejection/visible chips; Smart hub/live Snooze distinct semantics.
- Android instrumented/emulator: permission combinations, V337 upgrade, DND, exact-alarm restrictions, FSI allowed/denied, foreground-service failure, boot/timezone, receiver duplicate/stale token.
- Actual owner device: locked/unlocked, silent mode, app killed, missed alarm recovery, custom audio permissions, speaker/Bluetooth, low-power, small display/tablet.
- Complete cumulative compile/lint/CI, long-running repeated alerts, backup/restore, accessibility and physical certification after UX10.
- Evidence artifacts: exact SHA, passing test logs, before-after fixture checksums, screenshots/videos and owner acceptance notes.

**Decision request for implementation:** Approve UX06A-1/2 compatibility corrections separately or via explicit owner continuation. Do not silently expand to UX06B/06C or touch V337.

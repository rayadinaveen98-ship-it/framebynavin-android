# RX01-F — Backlot Premium Reminder Scenes: Final Source-Stage Freeze
Date: 2026-11-20
Status: SOURCE-STAGED ONLY. No compilation, unit/lint/instrumentation tests, physical QA, integration, merge, APK or release.
Owner contract: source-stage one change at a time; consolidate and test only after the founder says ALL DONE.

## Baseline and source lineage
Protected approved main and stable are both commit abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
Draft stacked PRs: RX01-A #448, RX01-B #449, RX01-C #450, RX01-D #451, RX01-E #452, then this RX01-F branch.
UX09 is excluded. Do not alter or merge any protected baseline before final integration approval.

## Verified source features — not runtime behavior
1. Three domain-specific entrance identities (Creator Pulse Studio, Personal Living Bloom, 21-Day Challenge Rise Core) backed by native Compose Canvas; three different subdued MAIN palettes (Studio Nocturne, Velvet Sanctuary, Ember Command).
2. Original optical mic/alarm and owner-confirmed combined icon artwork. No microphone recording, countdown, false streak or delivery receipt.
3. Double-tap gesture gate; accessible Open action; a 320ms transition; main response controls not composed before identity validation. The D refinement validates owner-controlled occurrence identity on IO twice, with VERIFYING before EXITING.
4. Creator Alarm and Voice wrap their original ReminderCheckInScreen and callbacks. Personal eligible, unlocked Alarm and Voice wrap the original privacy-protected action view. Voice notification does not gain unauthorized automatic full-screen delivery.
5. New non-exported 21-Day Challenge visual Activity opens from existing ALARM/VOICE notification content intent, validates real scheduledAt/ID/current challenge, and uses original MainActivity Challenge confirmation and existing Phase1HabitSnoozeReceiver for Snooze. QUIET remains original.
6. RX01-F adds an independent per-domain ON/OFF preference, default ON, and a scene Settings screen with static previews, accurate description of legacy precedence, and separate Challenge full-screen Alarm opt-in, OFF by default.
7. Shared RX01CurrentReminderMotion tracks Activity lifecycle/Android animator/battery saver and freezes motion when stopped or power restricted, without changing actual reminder audio or schedule.

## Exact settings and preference precedence
Settings -> Appearance & Sound -> Reminder Scenes now shows the new three illustrated switches before the preserved seven Classic/five Cinematic scene selectors and the original Premiere controls.
Creator and Personal: Fluid enabled means RX01 Entrance -> MAIN takes priority. If fluid is disabled, existing selected Premiere (when enabled) takes priority; otherwise original V391 scene/reveal.
Challenge: Fluid disabled returns the existing notification -> MainActivity deep link. Quiet never uses cinematic automatic full-screen. No existing scene ID or saved preference is deleted, migrated or replaced by turning fluid on/off.
Original companion preferences stay untouched. This phase adds no third-party animation library, paid service or account/cloud integration.
Failed preference commits must not be presented as success.

## Explicit Challenge Alarm FSI restrictions
Full-screen Alarm request requires original current ALARM occurrence, fluid enabled, separate owner opt-in, current HIGH Alarm channel and Android FSI eligibility (including API34+ canUseFullScreenIntent). The OS may still refuse the screen. VOICE and QUIET never request automatic FSI.
The setting is initially OFF and is not itself a platform permission grant; Android settings links and status indicators are provided. When unavailable, preserve an actionable notification rather than bypassing background-launch policy.
No new Android permission or alarm scheduler was added in RX01-F.

## Original source owners and data preservation
Creator: TaskStore, ReminderOccurrenceStore, original Activity watchdog, services and Stage Done/Working/Snooze/Pause/Reschedule/Dismiss/Replay callbacks remain authoritative.
Personal: PersonalReminderStore, PersonalDeliveryCoordinator, keyguard privacy and original Done/Snooze/Dismiss transactions remain authoritative.
21-Day: Phase1HabitStore, original 21-day v1/v2/v3 codec, daily confirmation receipts, 1–5 Voice repetitions, Piano/Custom/OFF music, Snooze receiver, notification, scheduler, recovery and completion modes remain authoritative.
Opening an entrance, double-tap, or challenge main action never itself marks a challenge day Done. The original Challenge screen handles explicit confirmation.

## Known risks / required tests AFTER explicit ALL DONE (none performed now)
- Kotlin/Compose compilation for the whole stack; source imports and UI signatures; all JVM tests, app lint and Android instrumented tests.
- Correct one-tap/no-op, double-tap/once, TalkBack/switch/keyboard equivalent and no ghost Done/Snooze controls beneath the entrance.
- Exact owner token checks and cancellation races during VERIFYING/EXITING; two simultaneous reminders; disabled/changed account; data recovery and process death.
- Three entrance and three genuinely different main palettes remain visible in real Creator/Personal/Challenge screens; old opaque layers don't hide them.
- All Creator/Personal response callbacks and original Voice/Alarm services still work correctly, including replay, smart Snooze, pause and reschedule.
- Challenge QUIET unchanged; VOICE opens on user action only and retains existing backing music; ALARM FSI only when eligible and opted in; denied FSI has correct notification fallback.
- Original Challenge Snooze 10m and unchanged daily Done receipts survive missed/repeated/edited/paused/archived/deleted/confirmed alarms; no false success on dispatch.
- Locked Personal and Challenge screens reveal no private activity/title/notes, both before and after device unlock.
- All three switches persist across restart and rollback. V391 scenes, Premiere toggles and Spark/Mira preferences remain saved.
- Test Android API26, Android14/15/16 notification/FSI entitlement/channel/foreground-background restrictions, Play Store FSI policy and production release variant.
- Measure performance, frame pacing, reduced motion, battery saver, lifecycle stop, 320dp/200% font, TalkBack, colour contrast and physical-device QA quality.

## Final integration plan, not yet authorized
When founder says ALL DONE: re-verify GitHub SHA and all draft PR boundaries; reconcile as one isolated candidate excluding UX09; fix source/compiler regressions and run full automation, Android/emulator/instrumentation tests; build precisely identified signed QA APK; deliver for the founder's multi-day physical-device testing; then decide on stable/release promotion separately. Any failed test or privacy/action/notification regression blocks promotion.
No final APK, release, merge or protected branch modification is claimed by this stage.

# UX06A — Implementation Decision Brief and Reminder Experience Handoff

**Status: PROPOSED / DOCUMENTED ONLY (no Kotlin changes, no test execution)**
**Audit source:** UX05.1E draft PR #402 head d9a4ef8271c34947ce238024dc31b9413bf47870.
**Android build target checked:** app/build.gradle.kts currently minSdk 26, compileSdk 36, targetSdk 36.
**Stable owner reference:** V337, unchanged.
**Owner instruction:** design/audit before implementation and one consolidated certification after UX10.

## Product goal

The user must be able to save a reminder, understand precisely how it will attempt to notify them, trust that no feature silently marks their work completed, and recover from blocked Android permissions or expired/custom audio without guessing. The Experience 2.0 design should be distinctive but action-first.

**Design principle:** preserve four canonical reminder domains. Share semantics, visual patterns, music policies and truthful health information via read-only adapters. Do not merge existing schedulers, force mandatory new permissions, or treat theme preferences as domain data.

## Executive decision table

| Decision | Recommendation | Needs further founder approval? |
|---|---|---|
| Fix Creator 4–5 repeat reconstruction | YES, narrowly use same V403 policy in Intent.toTask | Narrow implementation stage only |
| Global Creator 20m Snooze UI inconsistent with supported values | REMOVE unsupported 20m chip; leave valid 5/10/15/30 choices | Adding 20m end-to-end would need a separate approved scope |
| Live Smart stage vs Reminder Center future-target Snooze | PRESERVE both functions; change copy to indicate exact behavior | Any scheduling semantics change requires explicit approval |
| Five cinematic themes | Curated set: Aperture, Sunrise, Atmospheric, Projector, Rings | Visual-direction approval before UX06C implementation |
| Existing seven V391 themes | Preserve as Classic; never silently rewrite enum IDs or user preference | Removing Classic requires approval |
| Experimental V394 Premiere | Preserve optional setting and visible sample-vs-live distinction | Making default requires approval |
| Shared audio behavior | Prefer existing ducking implementation: ReminderBackingMusic already reduces gain around speech on supported services | Parity across domains after UX06D spec |
| Challenge music, custom URI, voice settings | Add after explicit UX06D data/migration and lossless restore contract | YES, user-sensitive data model expansion |
| Challenge automatic full screen | Do not enable as side effect of theme work | YES, OS policy/privacy and UX decision |
| Cloud sync / new paid TTS/music | NO; retain local and offline-safe behavior | YES, separate cost and security approval |
| Background permission bypasses | NO; use supported Android checks and graceful fallback | No forbidden OS workarounds |

## UX06A narrow source-fix work packages (for the next *implementation* turn)

### UX06A-1: Creator voice-repeat count consistency

**Target production code:** app/src/main/java/com/framebynavin/app/reminders/AlarmRingingService.kt, specifically Intent.toTask at present line 440.

**Change allowed:** normalize an already-present EXTRA_VOICE_REPEAT_COUNT through V403VoiceRepeatPolicy.count instead of the hard-coded 1..3 clamp. Preserve 3 as default, all existing Extras, task action identity, start/stop behavior, Service playback loop, alarm timeout and notification content. No new app permission.

**Required trace before edit:** VoiceReminderActivity.toTask; AlarmActivity.toTask; AlarmRingingService.putTask; ReminderScheduler.alarmPendingIntent; VoiceReminderService.start/stop/replay; V403VoiceRepeatPolicy. The actual authoritative task store is separate from this lightweight UI Intent snapshot.

**Negative scenarios:** missing/invalid extras, 1..5 counts, frozen speech text across repeat cycles, old Android instance created from 3-repeat V337, interruption by Done/Snooze, fallback upon TTS engine failure, watchdog not too short for a five-utterance cycle.

**Success definition:** no read path drops count 4 or 5; no path replays or completes more than one notification occurrence, without changing stored project state.

### UX06A-2: Creator global Snooze settings consistency

**Target production code:** app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt global Creator Snooze chips, present line ~1825.

**Change allowed:** render supported V280ReminderSnoozePolicy.CHOICES (visually ordered 5,10,15,30) rather than the hardcoded invalid 20. Existing persist/store logic and 10m fallback remain untouched. Existing valid per-project settings unchanged. Consider a tiny static source assertion for displayed options.

**Success definition:** every displayed option is accepted by the saved policy and recovers as selected. No silent 20 -> 10 switch.

### UX06A-3: Semantic Smart Snooze copy only

**Target surface:** future-target Snooze in V157ReminderCenterUi.kt / future stage read-only text; live stage actions in ReminderCheckInScreen.kt and V394PremiereExperience.kt.

**Allowed:** clarify whether a given action moves a scheduled final target or defers just the live stage. **Do not change** CreatorViewModel.snoozeReminderFromCenter, ReminderCenterPolicy, ReminderActionReceiver or SmartEscalationScheduler timing math. Scope copy separately if UI validation suggests ambiguous or cramped small-screen labels.

**Success definition:** same actual scheduling as before; user informed of resulting target or unchanged deadline; no accidental Stage Done.

**Implementation process:** one focused draft PR stacked on the exact reviewed docs-only branch; no direct stable changes and no merging until owner approval. Because owner requested consolidated testing after UX10, perform only safe static inspection during staging and record "not compiled/tested" until the final campaign.

## UX06B — Reminder Center & composer handoff (proposal, not implemented)

**Current entry surfaces:** V157ReminderCenterUi, V2232PersonalReminderHub, V219ReminderHubFilter, V181ReminderInboxUi, V2193ReminderHistoryUi, BacklotReminderHubChrome, V101BReminderUi and Phase1HabitSharedUi.

**Information architecture:**
1. Overview Today / Upcoming / Needs Response / History; filters Personal, Creator, Challenge, Ideas as *read-only projections* of owner stores.
2. Optional Quick Add: title + time + Save using relevant domain default, with visible scheduling fallback.
3. Existing advanced Composer: What / When / How / Review, but avoid forcing a four-step form for every user.
4. Active reminder status: tell user what the app knows (scheduled approximate vs exact, notification posted, response recorded) without claiming seen/heard.
5. Each domain opens its authoritative detail editor, not a shallow duplicate that writes new records.

**Preservation:** My Spaces links, companion icon, existing Creator stage context, Personal recurrence/day scopes, strict/flexible challenge progress and legacy V297 archive.

## UX06C — Cinematic Scene handoff (proposal, not implemented)

**Current primitives:** V391ReminderSceneEngine, V394PremiereExperience, V336DirectorReminderVisuals, V200ReminderLayoutPolicy, V201ReminderMotionPolicy, V195 particle scenes, V390CompanionNotificationArt.

**Art system:** Five curated scenes inspired by a cinematographer's toolkit: camera aperture, personal sunrise, soft particles, projector light, glowing rings. Maintain Classic seven-scene enum names/preferences separately and keep the experimental Premiere opt-in. No gesture control inside Canvas. Tone defaults: calm for Personal, deliberate for Creator, encouraging for Challenge, restrained for Ideas; do not force an alarm scene on a quiet notification.

**UX invariants:** readable high-contrast title, scheduling context and meaningful primary action; Snooze visible without opening Options; minimum 48dp targets; full-screen entry must not gate notification delivery or interaction; low-power/reduced-motion static backgrounds; small phone, TalkBack, landscape and tablet layouts.

**Companion:** optional celebratory or supportive presence with a <=3-second non-blocking animation, with reduced-motion fallback and no system response side effects.

## UX06D — Audio parity handoff (proposal, not implemented)

Current ReminderBackingMusic supports local Piano/CUSTOM/OFF and speaking-state gain reduction in supported Voice services; Phase1HabitVoiceService is independent and fixed-count. Parity should reuse tested low-cost primitives, but challenge model/codec, export/import, default sound behavior and user-selected local document URI permissions need deliberate schema review before code.

**Do not:** copy private media, assume a content URI survives cross-device, start voice without valid permissions, claim sound heard from a successful service-start request, or make opt-in automatic retry default ON.

## UX06E — Response, readiness and trace handoff

One action taxonomy, per-domain storage authority, truthful statuses and optional per-occurrence diagnostics. Readiness check may include notifications, exact permission, full-screen access, channel importance, TTS language availability and local sound path. Absence of evidence means Unknown, not a user ignoring a reminder.

## UX06F — Post-UX10 integration and certification handoff

Execute the R01–R35 matrix in docs/qa/UX06A_REMINDER_BEHAVIOR_AND_CERTIFICATION_MATRIX.md, plus UX01–UX05.1 and UX07–UX10 cumulative QA. Preserve and diff V337 backup/restore and historical completion. Any discovered failure must be fixed and the affected test set repeated. Provide SHA, CI, logs, screenshots and owner physical-device acceptance before calling stable.

## Founder review items

1. Accept the minimal 20m option removal (preferred) vs adding a fully supported 20m Snooze everywhere.
2. Confirm Smart Snooze should preserve its current two context-dependent meanings with clearer wording.
3. Choose whether to retain all seven Classic visuals while adding five curated cinematic families (recommended).
4. Explicitly approve any challenge audio-model persistence additions at UX06D, with no paid services.

**Current decision:** UX06A specs ready; actual source fixes NOT STARTED. Next is a small, isolated UX06A implementation PR only after owner authorization to advance implementation.

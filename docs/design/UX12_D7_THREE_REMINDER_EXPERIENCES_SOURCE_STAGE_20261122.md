# Backlot UX12 D7 — Three Reminder Experiences, practical visual continuity

Date: 2026-11-22
Status: **SOURCE STAGED ONLY** — NOT compiled, unit-tested, linted, instrumented, device-tested, merged, APK-built or released.

## Context and immutable lineage

- Approved UX12 Simple Cinematic Companion System: Film Editorial structure, Living Spatial scene movement and Personal Storybook warmth. Use original native Compose drawing and approved companion art, never generated wallpaper renders or unrelated assets.
- Baseline D6: `feature/backlot-ux12-d6-settings-appearance-20261122` at `a46367ad27e9cff9a11412d3d2d4063659adf6dc`.
- This D7 branch: `feature/backlot-ux12-d7-three-reminder-experiences-20261122`, stacked on D6, NOT main.
- Protected owner-accepted `main` and stable remain at `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` at stage entry, unchanged by D7.
- RX01 / UX11 already have an independent integration QA draft PR #460 and existing Android 15 limited instrumentation proof. UX12 D1–D7 source is not part of approved main and has NOT been certified by those older runs.
- No UX09 sync, cloud, auth, database, Android manifest, alarms, broadcasts, workers, notification actions, foreground services or policy changes in D7.

## Source audit: why this visual-only change is appropriate

The RX01 source already owns accurate domain palettes and double-tap guard: Creator Pulse Studio → Studio Nocturne, Personal Living Bloom → Velvet Sanctuary and Challenge Rise Core → Ember Command, with verified occurrence re-check before MAIN. No second entrance state machine is needed or permitted.

The RX01 Creator and Personal MAIN surfaces remained long after double-tap: large 170–220dp art and multiple 1dp bordered panels, with duplicated domain headings. Existing state/data/actions are correct owners but needs a simpler first-screen hierarchy. Challenge main had a functional Day 1–21/activity/explicit-open/snooze/close layout but no selected companion, despite the approved storybook companion continuity. We must never imply that viewing the challenge or tapping Snooze marks a day done.

## Exact implementation source paths

### 1. `app/src/main/java/com/framebynavin/app/reminders/UX12D7ReminderEditorialHeader.kt` (NEW)

- Read-only, domain-palette editorial line and short heading, optional factual clock label.
- Distinct existing RX01 MAIN tint per Creator, Personal and Challenge. Small vertical rule rather than extra framed card.
- Semantic Text and normal UI layout, stable `ux12-d7-header-*` UI tags. Does not identify, schedule, complete or announce a reminder, acquire audio, request permissions or write data.

### 2. `app/src/main/java/com/framebynavin/app/reminders/ReminderCheckInScreen.kt`

- When `rx01MainBackdrop=true`, uses the shared Creator editorial header, with original displayed time. Existing non-RX01 Classic/Premiere text remains unchanged.
- Reduces the extra decorative Creator MAIN hero height to 148dp under RX01; original Creator hero stays 170/220dp in its Classic path.
- Keeps both `v336-creator-reminder-context` and `v336-creator-reminder-next-step` information, styling them with a restrained border-free surface in RX01 MAIN only. Original context/action content, view tags, voice, callbacks, stage validation and primary/secondary response set remain intact.
- No changes to `onStageDone`, `onWorking`, `onSnooze`, `onPause`, `onReschedule`, `onDismiss`, `onReplay`, ledger/occurrence identity, alarm/Voice Activities or service state.

### 3. `app/src/main/java/com/framebynavin/app/personal/PersonalAlertActivity.kt`

- The already unlocked, ready, non-NOTIFICATION RX01 MAIN uses the shared Personal editorial header, smaller 148dp decorative hero and quieter two information panels.
- Removes the duplicate Personal domain label only on RX01 MAIN; the reminder title, schedule, alert method, important linked-plan details and all response disclosure remain exactly in place.
- All original Personal `Done/Snooze/Dismiss/Close` callbacks, `v2233` tags, canonical token verification, error and busy state remain. `rx01MainBackdrop=false` Classic/Premiere and locked/privacy/loading/invalid/notification-only routes retain their original presentation.
- Never shows a sensitive Personal title or companion while locked.

### 4. `app/src/main/java/com/framebynavin/app/personal/RX01ChallengeAlertActivity.kt`

- Reuses the shared editorial header and adds a restrained, compact approved Spark/Mira `PRESENT` pose on Challenge MAIN only AFTER the original unlocked + valid occurrence branch. Reads the saved companion; no new stored character selection or recording activity.
- Compact height policy uses only current device height; main content remains scrollable and 48dp+ controls. Close action is now full width with minimum 48dp height.
- Preserves truthful factual Day x of 21 and user's existing challenge activity. Only the original `MainActivity` Challenge page may commit explicit daily completion; opening the visual reminder is NOT Done. The original `Phase1HabitSnoozeReceiver` still verifies and performs Snooze with the same 10-minute request.
- Locked, checking, invalid, stale-occurrence, full-screen eligibility and notification fallback paths were not changed. No synthetic streak or invented status.

### 5. `app/src/main/java/com/framebynavin/app/reminders/RX01ReminderOpticHero.kt`

- Replaces the redundant bottom secondary line `Open your reminder` with the **actual verified mode label** (Voice reminder / Alarm reminder / explicitly verified combined).
- Double-tap instruction, hero optic, domain tint, TalkBack semantics, pointer gate and real current occurrence re-verification remain unchanged.
- Moving decorative optical bars still do NOT imply a live microphone, actual alarm ring, recording state, delay timer or day completion.

## Freeze and regression boundaries

- Do not add new permission, FSI policy, scheduled notification, Snooze, repeat, playback, retry, background-autostart or database behavior in this visual stage.
- Source paths explicitly outside this stage: `PersonalReminderReceiver`, `PersonalPlaybackService`, `PersonalDeliveryCoordinator`, `Phase1HabitSnoozeReceiver`, `Phase1HabitStore`, `TaskStore`, Creator Service/Receiver, Cloud/UX09, Supabase, AndroidManifest.
- User's Redmi Pad 2 HyperOS background-autostart smoke success is provisional, NOT independent P0 long-idle/retry/Snooze certification. Keep P0 gate OPEN.

## Deferred D8 tests (author them now; do not execute)

1. Caller matrix: Creator Voice/Alarm, Personal Voice/Alarm (user-opened only when allowed), Challenge Voice/Alarm. Ensure notification-only Personal and user-locked Challenge never accidentally show a private MAIN screen.
2. All three entrances: correct domain-palette, correct Voice/Alarm label, optical symbol, no false live microphone activity; one tap stays entrance, exactly two taps + valid occurrence leads to MAIN; accessibility activation equivalent; stale token closes.
3. Creator MAIN: primary Done/Working, secondary Working, Snooze, options, Pause, Reschedule, Dismiss, Replay still call exact original handlers. Keep existing V200/V336 UI test tags and context labels. Check small phone/tablet/landscape/200% fonts.
4. Personal MAIN: locked privacy, schedule info, linked diet details, Done vs Dismiss semantics, Snooze and disabled busy actions, expired occurrence, recovery after rotation; all original tags.
5. Challenge MAIN: chosen Spark/Mira pose only when unlocked; correct Day number and activity; Open Challenge navigates to original completion owner; Snooze dispatches real existing receiver and must not fabricate confirmed Snooze or Done; no face/title content on lock screen.
6. Legacy renderers: RX01 disabled, V391 classic and V394 Premiere variants, V407 optional companions, persisted scene toggles and full-screen opt-in unchanged; no doubled backgrounds or clipping.
7. Reduced motion, Battery Saver, TalkBack, low-memory/API26, Android13–16 FSI and notification grants/denials, silent notification, and real-world P0 long idle + 5/10m Snooze and unattended repeat on physical Redmi Pad 2. Record actual played sound, never infer from notification display.
8. Inspect every D1–D7 source delta together, resolve merge conflicts in isolated candidate, compile/JVM/instrumentation/lint/Android15 emulator/smoke, artifact SHA/certificate/package identity, then owner physical screenshots and multi-day acceptance before stable promotion.
9. Never count source-added test cases or prior RX01/UX11 CI as testing this unbuilt D7 source. APK generation requires the founder's exact final-stage command.

## Owner-approved staging and QA gate

- D0: approved design blueprint.
- D1–D7: source staged in isolated cumulative ancestry.
- **D8 NEXT**: integrated test + QA + traceable Lab APK **only AFTER explicit owner command** `Done bro, start next steps and give APK` or equally explicit final-stage approval.
- Do not invoke per-stage Android tests, lint, Gradle build, APK assemble, emulator, workflow dispatch, merge or deployment while staging D7.
- P0 Personal Reminder reliability and real-device UX acceptance remain separately mandatory before any release.

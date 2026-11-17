# Backlot UX06A — Focused Reminder Compatibility Corrections

**Implementation status:** SOURCE STAGED — NOT COMPILED, TESTED, CERTIFIED, MERGED OR RELEASED.
**Source parent:** Draft UX06A audit PR #403 at `42d1bcf77152abdd0fa5d6a73199ca8fe826a8fb`.
**Working branch:** `feature/backlot-ux06a-source-compatibility-fixes-20261117`.
**Protected baseline:** `stable/backlot-v337-premiere-owner-testing-20261114` remains unchanged.

## Why these changes were made

UX06A identified two source-confirmed compatibility mismatches in the cumulative UX03 update and one confusing Smart Snooze label contract. Fix the minimum affected source paths **without replacing the existing Creator, Personal, Challenge or Idea reminder engines**.

## UX06A-F01: Creator 4–5 utterance count preserved on rehydration

- Updated `reminders/AlarmRingingService.kt` shared `Intent.toTask()` to normalize incoming repeat count with `V403VoiceRepeatPolicy.count`, instead of the old hard-coded 1..3 clamp.
- `Intent.putTask()` already passes `CreatorTask.voiceRepeatCount`. UX03 Creator UI and VoiceReminderService use the same 1..5 policy. The read side now aligns with the source's saved and selected range.
- Missing extra defaults to three. Existing 1/2/3 historical settings retain their meaning.
- The Voice Activity's **Replay** remains intentionally a single TTS utterance of the current occurrence. It does not create an additional delivered reminder, receipt, reward, auto retry, or Stage Done.
- Do not infer that five speech cycles have been physically certified by this code adjustment.

## UX06A-F02: Remove an unsupported Creator global Snooze choice

- Updated `ui/FrameByNavinV101BApp.kt` Settings to render `V280ReminderSnoozePolicy.CHOICES.sorted()`, rather than hard-coding `5,10,15,20,30`.
- `CreatorOsSettingsStore.setSnoozeMinutes()` already accepted only 5/10/15/30 via the same policy; before this change selecting **20m** silently saved the fallback **10m**.
- All valid choices and saved values remain unchanged. Global preference default remains 10m. Project-specific `0` retains inherited global default behavior.
- Does not alter Personal/Challenge/Idea snooze values, notification action tokens, countdowns, alert scheduling, or storage migrations.
- Adding a real 20m option in every relevant domain requires separately approved semantics, persistence, serialization and QA.

## UX06A-F03: Show exactly what Smart Snooze does

**Reminder Center before the Smart alert fires:**
- In `ui/V157ReminderCenterUi.kt`, Smart cards use **MOVE +Xm**, not ordinary **SNOOZE Xm**, for the action calling the existing `CreatorViewModel.snoozeReminderFromCenter`.
- The helper copy displays the computed final Smart target and clarifies that the project deadline remains unchanged; invalid Smart windows explain why moving is unavailable.
- Uses the preexisting `ReminderCenterPolicy.nextSnoozeAt` calculation, bounds checks and callback. No scheduling behavior changes.

**Live Smart stage full-screen:**
- In `reminders/VoiceReminderActivity.kt` and `AlarmActivity.kt`, pass `smartStageSnooze` only when `ProjectPulseEngine.isStageCheckIn(task)` and task is Smart.
- `reminders/ReminderCheckInScreen.kt` shows **REMIND IN Xm** and a note that the *reached Smart stage repeats while the final target stays unchanged*.
- Both native classic screen and opt-in V394 Premiere receive the same label/hint. The latter uses its existing `supportingText` field; no modification to Premiere renderer.
- UI callbacks still send `ACTION_SNOOZE` with the same occurrence token and do not alter `ReminderActionReceiver` or `SmartEscalationScheduler`.
- Notifications retain their existing short action labels and token semantics in this narrow package; future UX06E may unify copy after testing Android notification action truncation and accessibility.

## Exact changed production files

1. `app/src/main/java/com/framebynavin/app/reminders/AlarmRingingService.kt`
2. `app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt`
3. `app/src/main/java/com/framebynavin/app/ui/V157ReminderCenterUi.kt`
4. `app/src/main/java/com/framebynavin/app/reminders/ReminderCheckInScreen.kt`
5. `app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt`
6. `app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt`

All changes are *source-only*, staged in a new draft branch stacked on PR #403. No source files in other domains, no manifest permissions, no data store, no scheduled alarm ID, no notification receiver action or visual theme preference were altered.

## Post-UX10 certification, NOT RUN

Required exact tests are specified in `docs/qa/UX06A_REMINDER_BEHAVIOR_AND_CERTIFICATION_MATRIX.md` in parent PR #403:
- R01–R03: 1..5 repeat/invalid Intent extras, persistence and replay; service watchdog under five long phrases.
- R04–R05: all global/per-project Snooze chips match the saved policy, including older/invalid inputs and backup/restore.
- R06–R08: Smart target movement vs reached-stage snooze; final deadline stays unchanged, valid sequence and one accepted token.
- R21–R28: Android permission, notification, FSI, foreground service, reboot/time change, stale and duplicate action pathways.
- R29: V337 upgrade, plus complete Creator/Personal/Challenge/Idea data preservation.
- R32: 320dp classic, Premiere, 200% font and TalkBack, including the revised Smart helper copy.

**Static review performed:** fetched and checked exact changed Kotlin snippets; checked Android actions and callbacks still reference original functions. This is NOT successful compilation, UI visual validation, device reliability or a passing test case.

## Next proposed product stage

**UX06B — Unified Reminder Center & progressively disclosed composer**: design navigation and adapter policy from the existing four source domains; do not migrate storage as part of surface layout work. The next UX package must remain an independent draft source stage and is not automatically approved by this PR.

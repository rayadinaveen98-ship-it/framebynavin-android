# Backlot UX06E — Response Clarity, Optional Companion & Truthful Android Readiness

**Date:** 2026-11-17
**State:** SOURCE STAGED / STATICALLY INSPECTED. No compile, tests, emulator, APK, CI, physical certification, merge or release.
**Parent:** UX06D cumulative draft PR #407, head `aa5a6933e53f3e7acc2b609976dbcc7865f8f69d`.
**Protected stable:** `stable/backlot-v337-premiere-owner-testing-20261114` — unchanged.

## Intended experience

1. Creator and Personal Voice/Alarm full-screen alerts remain **action-first**, with existing Done, Working, Snooze, Dismiss, Pause, Reschedule and Replay callbacks unchanged.
2. An owner may independently opt in to a brief **3-second appearance of their already selected Spark or Mira companion**, placed strictly within the artwork hero, not over actions, never triggered by a Stage Done/achievement receipt.
3. Every live Creator/Personal Voice/Alarm alert offers a user-expanded **read-only Android access** panel after core actions. It explains known permission/channel constraints without pretending an OS notification was seen or TTS sound heard.
4. The 21-day Challenge retains its independent Alert Health panel and notification action model. New UX06E copy projects the **actually saved** UX06D Voice count/gap/music, and emphasizes only explicit user Done counts.
5. Idea reminders stay short, actionable notifications with original owner action policies. No new full-screen requirement.

## Implemented UI files and behavior

### V407ReminderCompanionExperience.kt

- New separate preference `backlot_reminder_companion_v407/show_on_live_alerts` default **false**. No changes to V144's historical selected identity or to saved V391/V394 theme keys.
- `V407ReminderCompanionMoment(occurrenceKey)` uses existing `V144GuidePrefs.currentGuide` and `V144GuideCharacter` sprite/character assets, as stored by the owner; no new art/duplicate character packs.
- A visually restrained `CinePulseState.PRESENT` or reduced-motion `IDLE` enters inside the *decorative hero Box* and disappears after 3000ms using a Compose coroutine tied to the composition. It does not intercept pointer events or become a screen-reader node.
- Lifecycle: leaving the screen cancels the coroutine; changing the occurrenceKey displays only a fresh applicable greeting. Reduced motion/low power produces static art and a zero-duration transition.
- Companion ON/OFF is explicitly user selectable under **Settings → Reminder Scenes**. Off remains the upgrade default. No 21-day Challenge full-screen activation or forced notification companion art change.

### V407ReminderAlertReadiness.kt

**Read-only, current-device capability snapshot:**
- App notification access (AndroidX NotificationManagerCompat).
- Source-owned live notification channel: Creator `creator_voice_reminders_v1` or `creator_alarms_v1`; Personal `backlot_personal_reminders_v1`. A channel not yet created = **UNKNOWN**, not falsely Allowed.
- Exact alarm capability for API 31+, including best-effort timing possibility when denied.
- Full-screen intent access for Android 14+. **Personal Voice is notification-only** and therefore shows Full-screen: **Not used**. Creator Voice/Alarm and Personal Alarm may use the checked path.
- States ALLOWED/DEGRADED/BLOCKED/UNKNOWN/NOT_APPLICABLE are *not* delivery, read, played, acknowledged or completion events.

**Only after owner expansion:**
- The panel reports the current snapshot and explains Android heads-up vs full-screen, permission denial, and that audio requests do not prove actual sound.
- Buttons allow **Recheck**, **Notification Settings**, **Full-screen Access Settings** (only where applicable), and **Precise Alarm Settings** (only when access is limited). These are Android-owned settings intents opened exclusively by user taps, no bypass or permission auto-grant.
- Does not call AlarmManager scheduling, update a task, trigger retry, terminate a service, change a permission automatically, or collect telemetry.

### Creator `ReminderCheckInScreen.kt`

- Preserved existing opt-in Premiere route, particle intro/reveal behavior and all action callbacks/test IDs.
- The original `V336DirectorSignalArt` is wrapped in a hero-only Box; the opt-in selected companion appears there without replacing the signal artwork or moving response controls.
- The new access disclosure sits **below** full actionable content in the original scrollable Column, not above or covering STEP DONE/WORKING/SNOOZE.

### Personal `PersonalAlertActivity.kt`

- **Critical privacy guard:** the existing locked-screen branch returns before showing title, companion, access inspector or response controls; invalid/loading branches likewise return first.
- Only the unlocked live Personal artwork section can show the opt-in companion. Existing Done/Snooze/Dismiss controls, owner response callback, pending token and generic lockscreen content untouched.
- Small copy clarifies `Done` is explicit completion and `Dismiss` does not complete a Personal reminder.
- A Personal NOTIFICATION-only screen does not show misleading Voice/Alarm permission diagnostics; its native notification remains managed by existing Personal store and channel.

### Opt-in Premiere `V394PremiereExperience.kt`

- Creator/Personal **live** Premiere scenes receive the same brief companion and user-opened Android access disclosure.
- Preview is explicitly excluded when `onClosePreview != null`; Challenge preview remains sample only and receives no live access inspector or companion with implied completion.
- Supports normal busy/invalid/error and accessibility semantics of the existing UI.

### Challenge `Phase1HabitAlertHealthPanel.kt`

- Expanded existing readiness panel shows saved `voiceRepeatCount`, selected gap in seconds and the saved Music mode (Piano, local audio or Off). It does **not** assert audio was emitted or heard.
- Explains alert delivery never confirms a day Done. Existing actual Challenge ledger `Phase1HabitDeliveryLedger`, OS permissions, repair and owner confirmation remain as before.
- Does not enable full-screen for challenge or alter any scheduled occurrence.

## Interaction and data invariants

| Domain | Original primary action | Original other actions | UX06E effect |
|---|---|---|---|
| Creator ordinary | I'm Working | Snooze, Dismiss, Reschedule, replay | Decorations/copy only |
| Creator Pulse stage | STEP DONE only where stage eligible | I'm Working, live Smart Snooze, Pause, Dismiss, Reschedule | No stage validation or reward changes |
| Personal | DONE explicit | Snooze, Dismiss without completing | More explicit response meaning, same token/store |
| Current 21-day single activity | Confirm day Done explicitly in dashboard | Snooze, Undo, Edit, Pause, Archive, Restore, Delete | Read-only Voice plan/health, no new receipt |
| Legacy V297 21-day | Manual per-day Done | Import/export, undo | No code touched |
| Ideas | Open, Make Project | Snooze 3h, Stop | No code touched |

**No mutation or audio code in UX06E:** All helper components depend only on local SharedPreferences/Android diagnostics/owner-supplied UI parameters, never command handlers or cloud stores. If permissions appear 'Allowed', message still explicitly states that is not a guarantee of OS launch, sound or attention.

## Architecture, costs, privacy and a subtle product decision

- New preference defaults **OFF**: adding art does not unexpectedly interrupt, distract or inflate layout density on an existing user's live alarm.
- The companion is not a three-second *completion celebration*. That user-approved Creator/Personal achievements design is reserved for UX08 and must trigger only after an authoritative completed action, not on an alarm firing.
- No new paid service, background worker, analytics, microphone, AI voice, social/notification permissions or cloud connection.
- Existing V337 stable app and saved preference formats are unchanged. Old selected Spark/Mira assets are reused.
- Android may restrict full-screen intents, exact timing, notifications, MediaPlayback FGS and TTS. This screen offers access checks, not an OS workaround or Play-policy eligibility claim.
- No dynamic private titles are included in the readiness panel or system Settings intents. Personal locked state is generic and separately gated.

## Verified source checks and what is *not* verified

- Inspected existing V144 companion identity/persistence, V336 full-screen hero, V394 Premiere preview and live modes, Personal locked-screen branch, Creator/Personal action callback boundaries and Android permission methods.
- The newly added helper files are independent of owner delivery, scheduling and response stores.
- Review found Personal Voice doesn't use automatic full-screen; policy corrected to show Not used rather than falsely suggest granting access.
- Review found a conditional Compose `remember` in reduced-motion detection; normalized the call order before handoff.

**Not done:** Kotlin/Gradle compilation, JVM/unit execution, Compose UI instrumentation, locked-device/privacy verification, TalkBack, actual permission checks, 3-second timing measurement, battery/performance profiling, APK, merge or release. These remain deferred to consolidated post-UX10 certification.

## Product handoff

Next **UX06F** must reconcile UX06A–E source changes and upstream stacked PR ancestry, audit alert/voice/companion wiring against Android build and owner evidence, prepare the UX07 handoff, then reach UX10 for one comprehensive build, test and physical-device acceptance campaign.

Do not silently merge any UX06 draft PR, release a backup-incompatible APK, or declare runtime certification until actual tests are executed and reviewed.

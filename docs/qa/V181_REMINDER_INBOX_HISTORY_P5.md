# Backlot V181 — Reminder Redesign Phase 5: Reminders Center & Activity

**Stacked base:** V180 controls, V179 bounded follow-ups, V178 playback, V177 acknowledgement.
**Status:** Draft/debug preview. Stable/main unchanged.

## Creator experience
- Existing Reminders Center categories (Needs Attention, Today, Snoozed/Paused, Upcoming) and actions remain.
- New **Needs Your Response** section appears above the existing category lists. It displays current, authoritative, unresolved reminder occurrences only. It never uses a historical token for a current action.
- Each card shows the project title from the task store (not persisted in the acknowledgement journal), an honest delivery status, and a **quiet follow-up attempt count** (not a proof Android displayed the follow-up).
- **I'm Working** and **Snooze** dispatch only the original token to ReminderActionReceiver, which rechecks the current task fingerprint, creator-data generation, persistence and one-use claim before accepting. The UI says **Response requested** until confirmed; it never claims a response was saved immediately.
- **Open Project** preserves the existing editor/navigation route. The original reminder groups, selection and turn-off flows remain unchanged.
- **Delivery needs attention** shows a separate card when the currently authorized attempt actually failed, without inventing a user response. Creator can open the project to review permissions/scheduling.
- **Recent reminder activity** is collapsed by default and shows the latest six observations across current projects. Historic/superseded tokens are display-only. Privacy-minimal journal still stores only identifiers, channels, state and short reason/timestamps, maximum 12 occurrence records per project.
- No attempt to detect if a notification was actually **seen** or audio **heard**. Android does not reliably provide that evidence.
- No additional alarm/voice retry, notification scheduling, or automated project completion is introduced.

## Automated acceptance
- V161–V180 source contracts plus V181 history and action-token checks.
- JVM V181 policy tests for stale tokens, Done/archived projects, delivery failure and accurate labels.
- Android UI test of Working/Snooze/Open project, request-pending feedback, and past-attempt explanation.
- Android lint, androidTest compilation, debug APK build and checksum.
- Android 11 emulator runtime for V181 and existing V180/V179/V178/V177 action safety checks.

## Manual acceptance on real phone
- Back up the existing app before a signed in-place upgrade; do not uninstall a data-bearing installation.
- Send an original notification, then do not respond: inspect Needs Your Response; verify posted ≠ read. Do the same for voice/alarm timeout.
- Wait for one and two quiet follow-ups: verify attempts count is at most 2 and no extra ring/speech occurs.
- Tap **I'm Working**: verify authoritative task update and disappearance from pending response after refresh, no duplicate stage advance or accidental project completion.
- Tap **Snooze**: verify Settings preference is used and follow-ups cancel; verify action is rejected if token became stale.
- Open project: retains existing data and workflow stage; editing a title/notes must not invent a reminder response.
- Smart stages: superseded earlier stages may appear in recent history, not actionable. The final unresolved stage is actionable until creator responds or authority is replaced.
- Project marked Done/Skipped/Archived/disabled, or task restored from backup: old action buttons never operate.
- Notification permission disabled/quiet channel blocked: show delivery problem when recorded; no guessing about visibility.
- Rotate/reopen Reminders Center, small-screen clipping, screen reader controls, and back navigation.
- Physical-device emulator, OEM voice/vibration, boot/Doze and signed upgrade checks remain release gates.

## Current known limitations
- Journal history reflects the **last state per occurrence**, not every intermediate state transition. It intentionally cannot reconstruct exact audio audibility or notification reads.
- Worker claim count is an **attempt** count, not confirmed delivery.
- Async receiver action is acknowledged only after commit; open inbox may update with the existing 30-second UI refresh. Immediate UI success is not claimed.
- Recent history covers projects that still exist; no independent archive of deleted projects.
- Full production acceptance is Phase 6, not implied by a successful debug CI build.

# Backlot V296 — Guided Personal Reminder Composer (draft)

## Status and base
- Stacked on V295 foundation PR #293, commit `02a5a286be3bdc353b771b6b755a3845b59e84ab`.
- **Development only; no approved owner APK or production release.**
- Keeps package, versionCode 294, data model, Room schema, alarms, notification delivery, cloud, permission requests, and preview signer unchanged.
- Does not merge, uninstall, clear data, or access owner photos/account.

## Four progressive steps (new reminders only)
1. **Purpose** — neutral starter chips, purpose, editable title and optional notes.
   - Suggestions never override user-entered title/time/repeat/alert.
   - Continue requires nonblank title; no save or scheduling.
2. **Schedule** — date/time, repeat, weekly/day interval and hourly window options shown only if chosen.
3. **Alert** — notification, voice, alarm selection.
   - Custom sound, voice repeats, music, snooze and auto-retry are initially folded into a user-controlled disclosure on new reminders; existing settings/defaults remain in the draft state.
   - Existing full-screen alert permission guidance remains inside Custom options for alarm.
4. **Review** — actual draft title/notes, chosen recurrence, due time, alert, snooze and voice-repeat count. Save is present only here and calls the existing checked PersonalReminderDraft + existing caller's onSave.
   - Space linking is currently done by opening a Space and choosing Link existing reminder **after** saving. Review clearly states this; the wizard does not claim atomic linking.

- Back/Continue buttons and Android back follow steps; existing fields are maintained with `rememberSaveable`. The Close button cancels.
- Compose `AnimatedVisibility` fade + vertical expand/shrink transitions respect Android disabled-animation settings.
- Existing reminder editing intentionally retains full existing editor until a separate edit-flow migration and device QA are approved.

## Acceptance tests / quality gates
- `V288PersonalOsUiTest`: initial state hides future stages and Save, requires title, checks typed title survives Back, verifies alert custom disclosure, ensures no `onSave` until Review Save.
- Existing quick suggestion test now proceeds through all steps and checks submitted title/purpose/repeat.
- V294/V295 synthetic Android UI, Settings and Spaces suites remain in the workflow.
- CI script count updates from 20 to 21 Settings/Personal UI tests.
- Exact-source regression guard permits only the BOM-managed Compose animation dependency in V296. No change to production persistence, alarms or cloud.
- These code/test changes **are not proof** of a passing APK: confirm Android compile/instrumentation, check screenshot layout at small widths, review Source/Visual/Unit Actions results, then verify on owner's real phone without uninstalling.

## Deferred
- Rich user-created 21-day challenges and custom Reminder Groups need durable local models/migrations and full backup/recovery.
- Optional Space link directly on Review needs one consistent post-save transaction and recovery path.
- Rebuild existing-reminder Edit as guided workflow only after preservation tests and stakeholder review.
- Full private JPEG backup remains **not covered by cloud sync**.

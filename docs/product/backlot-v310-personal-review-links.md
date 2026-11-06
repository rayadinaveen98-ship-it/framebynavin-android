# Backlot V310 — Personal Reminder Review step: optional Space and Group links

**Development draft, stacked on V308.1. Not included in the signed V309 QA candidate, and not Play ready.**

## User experience
- Existing V296 four-step creation flow remains: **Purpose → Schedule → Alert → Review**.
- On **Review**, one compact **Organize this reminder** card is collapsed by default.
- Opening it progressively reveals saved, non-archived Spaces and existing Reminder Groups. Both are optional and independently selectable, one of each.
- Selection stays in Compose memory until Save. The preview says exactly what will be linked. The user can tap a selected chip to remove that selection.
- When a Space/Group catalog cannot be loaded, the UI displays a real error instead of claiming none exist. The reminder can still be saved without selecting unavailable links.
- The existing edit screen remains the original all-fields editor. Editing cannot remove existing Space/Group links; those continue to be managed through My Spaces and Reminder Groups.

## Persistence and safety
- V310 reuses **PersonalReminderCommands.create**, preserving one authoritative scheduling path and Personal DataStore.
- Selected Space/Group IDs must still exist before the reminder is created. An archived/deleted Space or deleted Group prevents the new reminder save, with a human-readable correction.
- After successful creation, one link to the chosen Space is added with the existing V289 Room repository, and one membership reference to the chosen Group is added through the existing V298 DataStore.
- This is **not** a cross-database transaction. If post-save linking fails, the reminder is retained, the editor closes and the UI explicitly reports a partial link failure. It never claims the reminder failed to save or silently rewinds Android alarms.
- No new Room schema/migration, reminder type, cloud backup, media files, or Creator reminders are created; Space links and Group links remain references to the original Personal reminder.
- Kotlin cancellation propagates instead of being represented as a successful link.

## Acceptance tests
- `V310LinkTargetsTest`: seven pure JVM cases covering optional, both selections, invalid deleted IDs and immutable selection.
- `V310ReviewLinksUiTest`: two Android 15 Compose tests for progressive reveal/selection, empty/error UI and a real screenshot.
- `V310CreateAndLinkDeviceTest`: one disposable Android 15 emulator test seeds a real Space and Group, creates one future Personal reminder through canonical scheduling, verifies Room + Group references, archives the Space, and proves a second invalid save was refused with no new reminder.
- Dedicated `backlot-v310-personal-links.yml` allowlists exact files, compiles debug app/instrumentation, runs full JVM regressions, and captures screenshot after interacting with the real Review component.

## No release promotion yet
- Wait for exact-head V310 CI (including actual Android UI and data store tests).
- Inspect screen with a populated list and a small viewport/large font.
- Upgrade from signed owner preview without losing Personal reminders, Spaces, photos, groups and link references.
- V309 frozen signed source remains unchanged. V310 requires a separately versioned QA candidate and the all-domain backup/restore release gate.

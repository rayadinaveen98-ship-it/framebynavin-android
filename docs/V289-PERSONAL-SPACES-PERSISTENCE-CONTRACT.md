# Backlot V289 — Personal Spaces persistence contract

**Status: design-only draft. No Room database, screen, media import, backup import or runtime migration is implemented by this file.**
Base: V288 Personal Today/Settings branch. Keep V288 sign-off, Personal reminders, alarm/voice/snooze leases, and existing restore paths unchanged.

## Product boundary

A **reminder** asks *when should I do this?* A **Space** answers *what am I working on, what have I actually done, and how has it changed?* These are related but distinct.
- Creating a reminder never implicitly creates or populates a Space.
- Creating a Space never schedules a notification without explicit owner confirmation.
- Existing reminder IDs remain stable, and a reminder may be linked to multiple Spaces without duplicating its delivery or completion.
- The Space journal persists independently of the existing capped PersonalCompletionJournal. Deleting a reminder does not erase previously and explicitly recorded Space entries.
- CreatorTask, YouTube and Firebase/cloud data are not modified by this feature.

## V289 phase boundary

Ship **local-only** Space CRUD, reminder linkage, manual entries and a read-only activity projection first.
Photo Picker, side-by-side before/after, media export/import and optional cloud movement are V290, not a V289 implicit dependency.

## Proposed Room database v1

Create a new Room database file (not a migration of `backlot_personal_reminders_v1` DataStore). UUID strings are stable IDs; `createdAtMillis`/`occurredAtMillis` are UTC epoch milliseconds and all user-facing dates are derived with the user's chosen ZoneId.

| Table | Primary key | Required columns / notes |
| --- | --- | --- |
| `spaces` | `id` | title (trimmed, bounded), type, iconKey, optional accentKey, goalText, createdAtMillis, archivedAtMillis nullable, displayOrder |
| `space_reminder_links` | composite `(spaceId, reminderId)` | optional label/role, createdAtMillis; FK to spaces only (Personal reminder lives in separate DataStore) |
| `space_entries` | `id` | spaceId FK, kind, occurredAtMillis, createdAtMillis, note optional, optional metricName/value/unit, source = MANUAL or CONFIRMED_REMINDER_DONE; immutable evidence fields when done |
| `space_media` (V290 reserved) | `id` | entryId FK, relativePrivateFileId, mime, width, height, sizeBytes, sha256, addedAtMillis, consent; no content URI persisted |

SQL requirements:
- Enable FK constraints, Room indices on `spaces(displayOrder)`, `space_entries(spaceId, occurredAtMillis)` and `space_reminder_links(reminderId)`.
- Deleting a Space: require confirmation; FK-cascade its own entries, links and media **only**. Never delete a linked PersonalReminder or CreatorTask.
- Archiving a Space changes `archivedAtMillis`, not its rows or reminders.
- Deleting a reminder externally makes the link **unavailable**, not a phantom scheduled reminder. The UI offers a non-destructive unlink; existing entries remain intact.
- Validate UUIDs, lengths, allowed enum strings, time bounds and numeric metrics at the repository boundary. Reject non-finite metric values, excessive notes and unknown serialized variants. No silent reset of corrupt database contents.

## Proof of a completion

A Space activity is never synthesized from the Android notification state.

**Manual check-in:** explicit user tap produces one journal row with `source=MANUAL` and `kind=CHECK_IN`.

**Confirmed reminder Done:** only an accepted `PersonalDeliveryCoordinator.apply(..., ACTION_DONE)` receipt may be associated with a Space. Store immutable evidence `reminderId` plus `originalOccurrenceMillis`; prevent duplicate imported entries with a unique index across non-null `(spaceId, reminderId, originalOccurrenceMillis, source)` or a separate immutable receipt-link table. If the completion event is no longer in the old capped history, the already-confirmed Space entry remains (with its original provenance). Snoozed, dismissed, unposted, missed, auto-retried and future scheduled occurrences produce **no** Done events.

Do not retrofit automatic completion mirroring into V288. For V289, begin with manual entries; add confirmed-Done import/linking only after dedicated receipt tests and a clear user-facing consent path.

## Space presets (content, not remote AI)

| Preset | Initial focus | Optional manual data |
| --- | --- | --- |
| Skin Care | AM/PM actions, SPF, 14-day journal checkpoint | Photo notes, routine reactions, date |
| Fitness | Workouts and walking | Minutes, sets, reps, user-entered weight |
| Reading | Book and reading sessions | Pages/minutes, notes, finished date |
| Editing & Filmmaking | Practice and project craft | Time, technique, feedback, milestone |
| Game Development | Coding and prototyping | Build milestone, time and notes |
| Gaming | Recreation | Session notes, time, optional limits |
| Custom | User-defined | Note or number with unit |

Presets are editable suggestions only. The owner's own terms, reminders and photos are not routed to a paid API or used to fabricate health conclusions.

## Private photo/media contract for V290

- Android system Photo Picker only; copy explicitly selected bytes to app-private storage with a generated opaque filename. Never depend on the user's source URI remaining accessible.
- Decode with pixel/byte bounds; create a safe thumbnail; apply orientation; strip EXIF GPS and other unnecessary metadata in the stored copy. Reject corrupt, unsupported and over-limit files with an error that preserves the entry.
- Stage media in a temporary private path, then commit metadata and atomically finalize the file with journaled cleanup so crashes cannot leave references to missing data.
- Photos are **not** uploaded or included in existing JSON/cloud backups. The UI must clearly say whether Space metadata and media are backed up.
- Future media export requires separate opt-in, documented size caps, checksums and encrypted/user-mediated transfer; restore media references only if their bytes are present and validated. Do not pretend that cloud sync is complete when private media is absent.
- Exclude personal Space photos from OS auto-backup/device transfer by default until an explicit verified private-media policy is implemented. Audit Android backup extraction rules and lock-screen/share preview leakage before shipping photos.

## UI boundaries and lifecycle

Home stays V288: My Day hero and agenda, with a future small Spaces strip only after backend stability. V289 adds a separate **Spaces** browser and a **Space detail** view: goal, linked reminders, manual check-in and date-ordered entries. Use existing dark cinematic tokens, compact icon-first rows and bottom action sheets. Empty states must be friendly; no dense setup explanations.

Safe defaults: local-only, no auto-created records, no background reminder writes, no photo permissions, no network requirement, no auto-generated streaks.

## Acceptance gates

1. V288 CI passes before V289 code is merged into the baseline.
2. New install and app upgrade both create the new Space DB without altering the old Personal DataStore byte-for-byte; no existing reminder IDs change.
3. Create, rename, reorder, archive, unarchive, delete and restore individual Spaces are tested; deletion never deletes linked reminders.
4. Linked reminder edited, paused, deleted or restored in DataStore: Space linkage behaves predictably; does not duplicate the reminder.
5. Manual check-in appears exactly once after process death and app restart; no clock/timezone adjustment fabricates extra Done.
6. Simulated malformed DB / disk full / interrupted transaction fail visibly; no silent empty-database replacement or loss of Personal reminders.
7. Progress shows *logged* vs *confirmed reminder Done* separately, and a future alert is never counted as complete.
8. Instrumented API 30, 35 and 36 tests; 360dp + large-font UI tests; backups/restores tested separately.
9. No Play/publishing or production signing claims until real-device owner QA, release signing and privacy/backup checks pass.

## Implementation ordering

A. Schema/entities/DAO/migration contract and isolated repository tests.
B. Space browser/detail CRUD without media.
C. Optional explicit reminder linking and manual check-in.
D. Read-only progress projection and import/export design.
E. V290 private Photo Picker integration, comparison controls and backed-up-media consent.

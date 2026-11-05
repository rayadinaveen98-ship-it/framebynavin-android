# Backlot V298 — Personal Reminder Groups

**Release status:** Draft implementation; CI, visual inspection, same-signed owner upgrade and physical-device acceptance must all pass before any APK is distributed. This branch is stacked on V297 (#295), V296 (#294), V295 (#293) and the frozen V294 signed owner preview.

## Why
The earlier Personal Settings was too specific to one user's schedules. Universal Reminder Groups let any owner organize *existing* saved Personal reminders by a custom name, color and icon, without creating fake/default schedules.

## Implementation
- New isolated `V298ReminderGroup` model. Owner chooses name (required), icon and color. No seeded groups, activities or fake completion.
- Membership is only a list of immutable Personal reminder IDs, **not a second copy of reminder data**. One reminder can belong to multiple groups; one group's unlink/delete never deletes or edits the original reminder, schedule, voice repeat, snooze, alarm or completion history.
- New `backlot_personal_reminder_groups_v1` private DataStore with mutex-serialized atomic mutation, checksum-verified versioned codec, bounded records/links, fail-closed corruption handling, previous revision retained.
- Personal Settings → My Reminder Groups, with compact cards, create/edit, link existing reminders, view/edit linked reminder, unlink, and explicit group-delete confirmation. Missing/deleted reminder references are marked unavailable and may be unlinked without touching reminder storage.
- Owner-invoked group-only JSON export and import with checksum validation, file-size limit, confirmation preview and additive-only merge. Existing groups are never overwritten by an imported ID collision. **A groups file contains only group names/appearance and reminder IDs, not the reminder records, completion history, photos or other Backlot data.**
- No changes to existing `PersonalReminderStore`/codec, alarm or notification delivery, Room schema, Android manifest, cloud syncing, Play signing or Creator task storage.

## QA
- `V298ReminderGroupTest`: seven tests cover no seeded groups, reference-only linking, multi-group ownership, unchanged membership on rename/decorate, checksum roundtrip, invalid data rejection, and no-overwrite import.
- `V288PersonalOsUiTest`: two additional Android UI tests cover create/delete while comparing complete Personal reminder store before/after, and no writes on cancel.
- `V294VisualAuditDeviceTest`: Android 15 synthetic empty Reminder Groups screenshot (12th capture). Existing Personal/Spaces UI test count becomes 24.
- CI boundary permits only the two new Personal `V298ReminderGroup*.kt` files and previously audited additions. Full unit suite enforces both V297 and V298 tests; source/copy/visual workflows run on branch.

## Known boundaries / remaining work
- Group records remain **device-local** and are not covered by the app's cloud sync; group-only JSON export/import is manual.
- Importing groups alone does not restore missing Personal reminders. Do not infer any reminder record was recovered merely because its link ID exists.
- Plan-to-Space/Reminder linkage and a complete all-domain, consent-based cloud recovery are separate future phases. Existing Spaces and private JPEG photos remain outside current cloud sync.
- V298 must not be merged/distributed without passing exact-source CI, 12 screenshots, 24 UI interactions, all unit tests, signed in-place upgrade testing and real-phone alert checks. Maintain V294 test APK unchanged until then.

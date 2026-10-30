# Backlot V186: Project status truth (Phase 1A)

Base: V185 commit d2072dab8fbe85a9b7e1ce51aa9baa7fc34cb92a. Version 186 / 2.3.6-v186-project-status-preview. Not a stable promotion.

- Shared, read-only facts are derived from the existing CreatorTask, workflow and workspace. No store replacement or migration.
- Workflow stages, DONE checklist items and creator-confirmed publication are separate. Skipped checklist items do not count as done. Output publishing never automatically publishes its parent project.
- Home, Today, Plan and Studio use named workflow counts rather than ambiguous percent complete. Home removes the artificial 3% minimum bar fill.
- A legacy planning Insights average stage percentage is replaced by the count of confirmed published projects. Undefined finish rate is shown as unavailable, not zero.
- Home image slideshow, Quick Create, reminders and existing project data are untouched.

Acceptance: pure JVM tests cover planned, finished-unpublished, published-active, output-only publishing, skip, archive and separation. Run existing 23-case V185 Android 11 emulator regression plus full unit, lint and personalStable APK. Physical-device acceptance must check progress on multiple project states, existing photo slideshow, TalkBack/large text, reminders, upgrade and export/restore. Insights Phase 1B remains separate.

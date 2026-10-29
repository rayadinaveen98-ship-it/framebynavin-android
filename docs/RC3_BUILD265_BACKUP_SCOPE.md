# Backlot RC3 Build265 — Creator vs Personal backup clarity

## Scope verified against source
CreatorBackupManager and PortableCreatorBackupManager export creator projects, ideas, project reminders, creator workflow and settings, Voice Idea recordings and original Best Frames where present; project attachment files are still external references. The separate PersonalReminderCodec intentionally excludes personal reminders from creator and cloud backups and offers a separately approved private checksum-verified export and restore.

## Changes
- Export card now explicitly says **PROJECT reminders**, and states Personal Reminders require a separate backup.
- Export completion banner repeats that Personal Reminders are not included; notes project attachment files are references only.
- Restore card explains that only creator data will be replaced and private Personal Reminders will not be replaced.
- Restore confirmation distinguishes **Project reminders** in the counts and prominently repeats the separate private backup limitation before approval.
- The existing Personal Backup & Restore screen remains the approved path for private reminders.
- Existing backup archive, checksum, media, encryption disclosures, restore journal and persistence are unchanged. No automatic merging of sensitive personal records.

## QA
- Android11 and Android15 tests for source-safe backup landing, clear Personal separation, previous Creator and Personal reminder, Spark, Workspace, and cold-start behaviors.
- Full Build265 JVM, lint, signed preview APK and upgrade signing compatibility from QA-verified RC2 Build264.
- Physical-device acceptance remains necessary for restoring a real ZIP, a separate private personal backup and preservation of existing local data.

## Production status
Stacked draft on RC2 PR #176. Not a Play signed release, not tested on a physical phone. Track release blockers at issue #174.

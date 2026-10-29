# Backlot RC2 — Build 264 legacy starter-data safety

## Evidence-backed RCA
The previous CreatorViewModel startup collector removed any persisted project whose ID was `starter-frame-breakdown` in both the read cleanup and transactional `store.mutate` path. This could silently delete an actual project if the creator customized that old starter record.

Original v0 source commit `15aa6faf64525ff043a1b184598580cdde5c360a` seeded exactly:

```kotlin
CreatorTask(
  id = "starter-frame-breakdown",
  title = "Frame Breakdown",
  platform = "Instagram",
  contentType = "Reel",
  dueLabel = "Today · 7:00 PM",
  status = TaskStatus.WORKING,
  progress = 72,
)
```

## Conservative implementation
- A pure `V230LegacyStarterProjectPolicy.isUntouchedDemo` reconstructs this historical starter with today's canonical `CreatorTask` defaults and uses FULL data-class equality, never only an ID.
- Both initial flow cleanup and authoritative `store.mutate` now use that exact same predicate.
- Customized title, progress, status, due label, platform, format, notes, reminders, stage, workspace, archive, publication or other persisted state preserves the original-ID creator project.
- Keep existing separate post-publish checkpoint migration unchanged; user work is never assumed to be a template based on starter ID.
- Exact original demo is still eligible for removal to satisfy the request to eliminate preloaded FrameByNaveen sample data.
- Limitation: old records have no separate user-edited audit receipt. An object indistinguishable from the complete original seed cannot be distinguished; a migrated record that differs is intentionally preserved rather than deleted.
- No new wipe, reset, backup rewrite or forced migration.

## QA and release gates
- [ ] Five JVM tests: exact demo, customized title/notes/format, saved reminders/workflows/assets/archive, stable order and idempotence.
- [ ] Android 11 and Android 15 full regression suites including RC1/ V222 cold-start real UI, Personal, Spark and Workspace.
- [ ] Full Build264 Gradle JVM/lint/owner APK and signing continuity vs Build263.
- [ ] Physical device: upgrade over real installation without uninstall, verify saved projects, customized seed, reminders, backup recovery, cold-start and archived work.
- [ ] Preserve stable main; release only after issue #174 checklist accepts QA, verified production signing and Play readiness.

Draft PR stacked on RC1 #175. Prototype sideload signature is not a Play release key.

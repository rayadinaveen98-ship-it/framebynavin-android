# Backlot V162 — Idea Vault Phase 3

Source: V161 `f581e03401a88a1f68cd6e406f5b390dd7e29cb9`. Preview feature branch; not a stable release.

## Goals
- Keep all existing ideas, original voice recordings and linked projects.
- Replace the destructive single-tap delete with a confirmation and **Recently Deleted** area. Items stay recoverable without a timed expiry. No automatic permanent deletion.
- Show a **Restore Idea** action that retains status, notes, original audio path, project link and other fields. While deleted, reminders stop; restoration does **not** reactivate canceled reminders without an explicit new choice.
- Active Home, Insights, Quick Actions, Idea Vault and idea counts do not treat deleted ideas as work; backup/cloud retains the tombstone record.
- Keep the existing filters and editor organization fields behind optional controls; lightweight guidance for similar saved titles is advisory only.
- Avoid duplicate project creation when a formerly converted idea has a missing linked task; leave the dialog open with a helpful failure message.

## Verification gates
- Static smoke confirms all previous V160/V161 protections and the new view/restore wiring.
- JVM tests for Unicode similarity, archived/trash semantics, voice/project preservation and reminder inactivity.
- Android lint, instrumented compilation, and preview APK artifact.
- Device QA: old backup import, real audio playback before/after delete + restore, reminder cancellation, search/filtering and conversion (including missing project link), TalkBack and large font, project ownership after cross-device restore.
- Do not merge to stable without device acceptance and creator approval.

## Limitations
- The Preview retains deleted content indefinitely in backups until we explicitly design and approve a safe permanent removal workflow.
- Deleted ideas retain voice media; removing an idea is **not** a privacy erasure operation.
- Title similarity does not upload anything and cannot guarantee discovery of duplicates.
- Project-plus-idea writes are not yet a single atomic multi-store transaction; do not claim full transactional conversion until recovery coverage is proven.
- Reminders and Settings designs remain unchanged. Phase 4 is Daily Brief + Content Calendar.

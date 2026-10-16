# Backlot Quick Actions — LOCKED roadmap and Phase 1 safety contract

**Locked 2026-10-16 (creator-approved).** Source base: V159 R5 branch `d4e4c2e8c43f3d5b36ee0846695ee3d7752b327e`.

## Roadmap (no scope drift)
1. **Phase 1: Data Safety & Preservation** — protect weekly-created projects; preserve user-empty weekly plans; ensure schedule templates do not overwrite project work; verify portable Voice Idea audio integrity and old backups; retain automation routine preferences on portable restore.
2. **Phase 2: Quick Actions hub and Quick Capture** — simpler navigation; capture flow and confirmation; no data loss.
3. **Phase 3: Idea Vault** — progressive disclosure, clear statuses, safer delete/restore, reliable conversion.
4. **Phase 4: Daily Brief and Content Calendar** — actionable project links, clearer event semantics and calendar navigation.
5. **Phase 5: Weekly Plan and Automation UX** — understandable creation previews, profile-aware templates, honest background job status.
6. **Phase 6: Creator Progress and full end-to-end QA** — truthful progress descriptions, recommendations, regression sweep.

**Out of scope:** Rebuild/redesign Reminders (R1–R5 unchanged); redesign Settings (separate later release); migrate stable branch or merge a preview without creator signoff.

## Phase 1 implementation contract

### Never delete existing weekly projects implicitly
- Changing Weekly Auto Plan from on to off **stops future generation only**.
- App startup/reload with Auto Plan off must not delete pending recurring projects.
- Disabling/deleting/resetting a schedule slot never deletes any project from that slot.
- Every generated project is a snapshot owned by the creator: editing the weekly template must not silently modify that project's title, due date, format, priority, custom notes, state, or reminder.
- Next generations use the latest slot template. Manual project edits remain authoritative.
- The original intentional deletion command for an individual project remains available in Projects; it is **not** triggered by a weekly template toggle.

### Preserve a deliberately empty plan
- Only on first-ever install, with no stored weekly schedule, may Backlot seed the legacy starter slots.
- Stored `[]` survives reload, export and restore unchanged.
- Explicit Restore Default Week is the only intentional reset to the default template.

### Media portability and backup safety
- Existing `PortableCreatorBackupManager` / `PortableBackupArchive` already exports the original app-owned Voice Idea audio in a ZIP with SHA-256 checks. **No second archive protocol is added**.
- Export must fail clearly if a referenced original file is missing, empty, outside Backlot's owned final-audio directory, or an active unfinished take; no silent "backup complete" on incomplete voice data.
- Detect audio file truncation during ZIP creation; verify old v8 archive media can be restored even when new backups use v9.
- Existing plain-JSON legacy restore remains supported, but plain JSON never falsely claims to contain voice file bytes.
- New backups are unencrypted local/exported files; users must keep them private.

### Automation routine preference portability
- Backup schema 9 includes the creator's three independent routine toggles (Daily Brief, Weekly Review and Idea Review), under `automationPreferences`.
- Strict Boolean validation; checksum and manifest cover new section.
- Schema 1–8 backups remain readable. If they lack the section, they preserve existing local routine settings rather than wiping them.
- Voice-media restore re-signs only the original version (v8 or v9).
- No change to recurring worker cadence, reminder scheduling, or current Settings UI.

## QA release gates
1. Android compile, preservation smoke, all unit tests, lint, instrumentation compilation and APK build.
2. Android 15 emulator tests: empty schedule, schema 9 routine preferences, schema 8 compatibility, actual portable original audio round trip, missing audio fail-closed behavior; existing backup hardening checks.
3. Physical-phone checks: install V160 over V159; preserve real projects and original audio. With Auto Plan on, create a future recurring project, edit its title/notes/due time, edit template and switch Auto Plan off; original project must remain unchanged even after restart. Delete every weekly slot and restart; empty state must remain. Export a voice recording and import on another device to confirm playback. Restore routine toggles, compare before/after.
4. Approval requires Android emulator completion **and** physical-phone acceptance. Do not claim a stable release from APK build alone.

## Known limits / deliberate deferrals
- No automatic restoration of weekly projects that were removed by old versions before V160.
- Existing preset contains FrameByNavin film-related defaults; profile-aware presets/preview are Phase 5.
- Switching off generation does not cancel reminders for already-created projects; those projects and their creator-selected reminders remain intact. The creator can manage them individually from Projects/Reminders.
- Portable backups only include Backlot-owned finalized local recordings; recording pointers to missing/external media cause export failure rather than data omission.
- Voice file portability is already functional; the earlier audit's suggestion that backup held only paths was incomplete. This phase hardens, tests and preserves its existing implementation.
- No Settings redesign, notification behavior modifications, or new Quick Actions layout in Phase 1.

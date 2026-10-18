# Backlot V171 — New Project Phase 1: Editing and data safety

Base: `feature/backlot-v170-settings-s5-backup-sync-qa` (Settings S1–S5 preview).
Branch: `feature/backlot-v171-new-project-p1-safety`.
Status: **DRAFT PREVIEW / NOT STABLE.** Do not merge to stable/main without testing and creator sign-off.

## Scope / implemented

1. **No hidden deadline changes.** Opening an overdue project in Edit keeps its saved past deadline. A creator may change title or notes without first postponing it. A changed deadline must be in the future.
2. **No hidden reminder resets.** Unrelated edits to title, notes or other non-scheduling details retain existing snoozes, pauses, checkpoint acknowledgements, reminder sequence, runtime mode and next scheduled occurrence. Explicit deadline, delivery, reminder plan, alarm/voice setting, priority or project format changes still recompute reminder schedules.
3. **Preserve existing progress and historical due label** when editing unrelated fields. Changing project format may intentionally recalculate stage/progress.
4. **Do not change legacy empty alarm sounds** just by opening and saving an old project.
5. **Reject unsafe saves honestly.** If the project write queue is in a known failed state, the save returns no ID. The composer stays visible and shows an error; it does not claim a save succeeded. If an edited project ID no longer exists, do not turn Edit into Create. Accepted saves are queued for disk persistence, and the UI says **Saving project** rather than **Saved**; the global write recovery dialog remains the failure path.
6. **Keep product contracts unchanged.** Full seven-step project creation is still default; Quick Create is separate. No schema migration, reminder engine rewrite, data reset, automatic release or stable branch modification.

## Important known limits

- An accepted queued save is **not** proof the disk write completed. Runtime write failure is handled by the existing global recovery dialog. Durable saved-state confirmation is a later design task.
- Composer discard-draft confirmation, closing Settings without losing a draft, and process-death recovery are **not** included in P1; they remain part of the next UX/draft-preservation phase.
- The scope of an intentional format or scheduling change can include reminder reinitialization. These actions must be user tested.
- Runtime Android platform delivery, background constraints and OEM alarms remain covered by the earlier reminder R1–R5 acceptance gate, not assumed by unit tests.

## Automated acceptance

1. V161–V170 inherited source-preservation guards, then V171 guard.
2. V171 JVM tests: overdue due, unchanged old custom reminder, unchanged GUIDED voice runtime mode with snooze, explicit time/plan/delivery/format changes and custom time edits.
3. All project/backup/reminder JVM regression tests.
4. Android lint, instrumentation test compilation and debug APK assembly. Build artifacts are preview only.

## Real-device sign-off (mandatory)

- Back up the current app *before* using V171; upgrade in place with same signing identity, never uninstall an app with valuable data.
- Create a project using the seven-step wizard; create another using separate Quick Create.
- Edit an overdue project (change only notes/title). Verify exact deadline and past-due status are preserved after app restart.
- Snooze a reminder, mark working and save only notes. Verify next reminder time, working pause, stage acknowledgment, priority, voice and alarm mode are preserved.
- Edit a historical CUSTOM reminder that is now elapsed; save only its title and verify its original reminder record is not silently replaced.
- Change deadline, explicit plan, delivery preference and format in independent test projects; verify intentional updates and no unrelated project impact.
- Try to edit a deleted project; ensure no new project appears.
- Revoke notification permissions; check reminder fallback paths and prevent duplicate ringing.
- Exercise failed persistence in a test environment: the app must not report a committed save or discard all recoverable input.
- Recheck idea conversion, project creation, Quick Actions, Weekly Plan, archived/skipped projects, backup integrity and Android text scaling.
- Require creator approval before stable promotion.

## Next phases

P2: Seven-step usability and save/discard/draft continuity. P3: typography and layout. P4: simplified Quick Create. P5: clearer reminder/project details. P6: final end-to-end device acceptance.

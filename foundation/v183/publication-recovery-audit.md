# v1.8.3 Publication and Reward Recovery Audit

Source reviewed: RC5 application commit `f1a986c54e2a26a67dc3cf3231923e58dc82626c`. This is a source audit, not a completed implementation or physical-device acceptance result.

## Verified existing behavior

- `CreatorPublicationEngine.finish` completes a project without asserting publication. `record` and `correct` maintain explicit publication metadata independently of workflow completion.
- Reward event keys are stable and `CreatorRewardStore.record` rejects duplicate keys. `reconcilePublication` replaces/removes the publication reward according to the current publication state.
- Post-publish checkpoints have stable project-owned IDs and their due dates derive from `publishedAtMillis`, not project completion time. `reconcilePublication` rebases pending dates and preserves completed reviews.
- Alpha22 backfill is conservative and intentionally does not invent historical stage-completion timestamps.

## Confirmed recovery gap

`CreatorViewModel.advanceWorkflow` and `correctPublication` persist the project first and then reconcile reward/checkpoint stores in an `after` callback. `completePostPublishCheckpoint` likewise updates the checkpoint before writing its reward. These writes use separate durable preference stores. `CreatorDataGate` serializes in-process transactions but explicitly does not provide a database transaction across unrelated preference files. A process death or failed subsequent write can therefore leave authoritative project/checkpoint state and derived reward/checkpoint state inconsistent. Existing one-time reward backfill is not a complete recovery mechanism for later edits or publication corrections.

The audit does not establish that this has happened to any real user. Do not infer missing or duplicate real rewards from synthetic evidence alone.

## Narrow implementation target

1. Reuse the existing data gate and durable task/checkpoint stores. Do not create a parallel publication model, rewrite the project UI, or introduce a backend migration.
2. Make publication-related reconciliation idempotent and recoverable from the latest authoritative project/checkpoint state. A startup/recovery pass must repair interrupted publication and checkpoint writes without replaying stale state after restore.
3. Preserve explicit correction semantics, including removal of an incorrect publication, re-recording, completion/reopening, and completed review history. Do not infer publication from `DONE` or from a reminder action.
4. Preserve stable reward keys and avoid rewarding repeated status taps, corrections, or an already-credited checkpoint. Reconcile only the keys whose authoritative evidence is known; do not delete unrelated historical rewards or invent missing stage timestamps.
5. Ensure queued work carries the creator generation, re-reads current authority under the gate, and cannot replay across restore/account transitions. A failed write must remain retryable and visible rather than silently disappearing.
6. Add synthetic regression coverage for process interruption after project save and after checkpoint save, repeated recovery, publication removal/re-recording, corrected review dates, completed-review preservation, and stale-generation rejection.
7. Run the full Android unit/lint/instrumentation/APK/signing/emulator gates. Commit the complete source and deliver a verified APK only after all gates pass. Preserve RC5 and frozen v1.8.2 as rollback points.

## Separate remaining foundation gates

Cloud reconciliation/full account deletion, the broader authoritative-writer and backup inventory, physical-device reminder acceptance, accessibility/navigation, and production signing/OAuth/privacy remain separate. Do not declare v1.8.3 final on the strength of this audit.

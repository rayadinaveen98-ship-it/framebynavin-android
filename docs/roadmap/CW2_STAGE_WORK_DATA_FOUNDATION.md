# Backlot Content Workspace 2.0 — CW2 stage-owned data foundation

Date: 2026-11-12  
Status: **source implemented on a draft branch; CI and Android/device acceptance not yet passed**  
Base: CW1, which is based on V336 stability candidate (not stable).

## What CW2 delivers
- One existing `CreatorContentWorkspace` per project. A new optional `stageWork` list
  stores stage-specific text and metadata, with records keyed by the workflow
  template's stable ID and stage's stable ID. The parent `CreatorTask.id` is
  the project identity; project title and stage index are never keys.
- Each stage may own notes, checklist items (separate from the global checklist),
  evidence claims with timecodes, explicit decision history, and IDs linking
  back to existing project sources, assets, and deliverables.
- `reviewedAtMillis` records an explicit stage review, not workflow advancement,
  deliverable publication, app notification or reminder acknowledgement.
- `CreatorStageWorkPolicy.put` rejects invalid template/stage IDs, ambiguous
  duplicate identities, and edits of archived/completed/skipped projects.
- `TaskStore.saveStageWork` uses the existing creator mutation transaction,
  restore-generation fence, revision comparison and stable project ID. A successful
  write increments only the shared workspace revision; there is no stage
  advancement, notification, audio or publication side effect.
- `TaskStore` now serializes stage-owned records alongside the shared project
  workspace inside normal on-device JSON and portable Creator backups.
- Old project JSON without `stageWork` defaults to empty without creating
  data or changing its stage. Malformed stage arrays fail closed rather than
  silently deleting saved stage notes or evidence.
- Legacy Plan and Publish saves explicitly preserve canonical stage records;
  Alpha3 Plan draft snapshots retain current stage records. Script saves copy
  the containing workspace and already preserve the optional field.

## Example
Project `Why Silence Works` — `v150_film_review`:
- `watch_notes`: viewing notes and source links.
- `evidence`: scene claim, timecode, referenced source/asset IDs.
- `script`: stage checklist/decisions while canonical narration remains in Script Studio.
- `edit`: optional edit notes and task receipts, without claiming a video editor
  exists in Backlot.
- `package`: optional production notes linked to its deliverable IDs.

The shared project library continues to be the source of truth for assets,
references, the canonical Script Studio and publish records; stage links do
not clone the files, scripts or publication history.

## Preservation contract
1. A stage record is absent until a creator explicitly saves stage work.
2. Multiple stage records in one project never collide with another project,
   even if titles and workflow stage names are identical.
3. New or malformed stage data cannot be silently truncated on decode.
4. Shared Plan and Publish editors cannot delete stage work.
5. Stale revisions and post-restore writes fail before replacing newer data.
6. Review/checklist updates do not change project stage, status, rewards, alarms,
   reminders, publication status or published timestamp.
7. External media is link-only. No media bytes are added to stage JSON or backups.
8. When a workflow template is changed, old stage data is preserved under its
   original template ID and is not auto-reassigned to a possibly unrelated stage.

## Tests
- JVM `CreatorStageWorkPolicyTest`: identity, isolation, preservation, stage
  review behavior, archived/completed refusal, Plan save retention.
- Existing Creator workspace and workflow unit tests.
- Real Android JSON and epoch-aware storage test:
  `CreatorStageWorkPersistenceAndroidTest`. It verifies old JSON compatibility,
  full-stage round trip, fail-closed malformed stage data, guarded project save,
  and portable export. Its transaction test runs only in disposable CI emulators
  and restores the original Creator backup in `finally`.
- Scoped unit workflow: `backlot-cw2-stage-data-unit.yml`.

**Important:** The instrumentation acceptance suite is authored but deferred to
final integrated Android validation in accordance with owner direction. A CI
unit pass would not make CW2 a stable APK.

## CW3 handoff
Implement a *Current Stage* contextual view using the new store rather than
building nine disconnected editors. Keep Plan, Script, Publish and Guide
available as shared tools, and add explicit stage notes/evidence/decision
editing through the revision-guarded `saveStageWork` method. Any new
editable surface must have project ID, template ID, stage ID, current generation
and workspace revision checks; also test restoring during edit, keyboard
scrolling, screen readers and small phones. Avoid automatic stage completion.

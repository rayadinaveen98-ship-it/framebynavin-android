# Backlot UX05.1C — Connected Creative Board and Contextual Stage Modules

**State: SOURCE IMPLEMENTATION STAGED, NOT COMPILED, TESTED, CERTIFIED, MERGED OR RELEASED.**
**Parent:** UX05.1B draft PR #399, commit `3140d5b3bd8769d2c19b8fb1f3442e438e0244bd`.
**Branch:** `feature/backlot-ux051c-connected-creative-board-20261117`.
**Protected:** V337 owner-approved baseline, main, hosted services, reminder system and QA data remain untouched.

## Product decision

Introduce a connected creator board **without** creating another source-of-truth for scripts, original image files, assets, project publications or stage history. The board is a browse-and-open card collection, not an unbounded infinite canvas, inline video editor, or separately editable document. This is deliberate: UX09 cloud sync is not designed yet, and a duplicate media store would invite loss and operating cost.

Creators can curate underlying stage evidence order, edit/relink it, connect it to a Script Studio beat and add context-specific tasks using the existing revision/restore-gated Stage Desk. Source and asset editing remains in the shared Plan; script beats remain in Script Studio; deliverable metadata in Publish Studio; cut versions in Output Craft.

## 05.1C source changes

1. **CW51CreativeBoardPolicy + CW51CreativeBoard:** Project-owned, on-demand projection of actual references, assets, stage evidence (for the currently active workflow template), Script Studio beats, deliverables and edit versions. Filter by all, sources, scenes/proof, story, outputs. Cards have stable type-prefixed keys, truthful status labels, missing-history warnings and destination actions. LazyColumn renders large collections without building every visual row eagerly. There is no data write on opening or filtering.
2. **Activity navigation:** new BOARD mode in ContentWorkspaceActivity. Hub card opens Board, and each item routes directly to the existing owned editor (Plan Sources/Assets, Stage Desk, Script Studio, Publish Studio or Output Craft). Android system Back from read-only Board returns to Hub without pretending there is an unsaved edit. BOARD mode is handled on activity state restoration and exhaustive Activity mode branches. No project stage advances or sends alerts.
3. **Creator-specific stage modules:** deterministic, optional stage-focus prompts based on the existing workflow template and Content DNA: film scenes/production, podcasts, tech testing, food recipes/shot plans, travel routes/locations, education, verified news, and generic capture/edit. A creator can opt into suggested checklist tasks; duplicate titles are skipped; only explicit Stage Save persists them. Nothing becomes automatically verified, completed or published.
4. **Stage evidence curation:** move up/down within the stage, edit existing evidence without changing its stable ID, reassign/remove source/asset/script beat links, and retain unavailable historical IDs when editing unrelated fields. All changes remain in the existing Stage Draft and explicit Save. Editing or reordering invalidates stale review state from UX05.1A.
5. **Evidence-to-script connection:** additive `CreatorStageEvidence.scriptBeatId`, default `""`, references an existing canonical `CreatorScriptStudio.beats.id` and never copies narration text or stores media bytes. Current beat selection uses a dropdown in the Stage Desk; cards show linked narration/number of evidence links. Domain validator rejects newly fabricated/cross-project beat IDs while preserving previously recorded historical unavailable references. Existing TaskStore Stage Work JSON and Stage Draft journal share the codec and round-trip the new optional key.
6. **No new paid features or host services.** The board is rendered locally using existing project data, without backend or media processing.

## Files

- `ui/CW51CreativeBoard.kt` — new board and read-only policy
- `ui/CW51StageModules.kt` — new creator/stage contextual guidance
- `ui/CW3StageDesk.kt` — contextual card, optional tasks, reorder/edit evidence, script-beat selector
- `ui/V19ContentWorkspaceAlpha6Ui.kt` — Cockpit board entry
- `ui/ContentWorkspaceActivity.kt` — BOARD navigation, restoration and canonical destinations
- `data/CreatorStageWork.kt` — appended evidence scriptBeatId and integrity checks
- `data/TaskStore.kt` — existing stage JSON codec supports optional scriptBeatId
- `docs/product/UX051_ADVANCED_CREATOR_WORKSPACE_ROADMAP.md` — roadmap update
- This handoff

## Invariants

- No project/new stage/title/identity created on board open; no automatic workflow advances, publication records or reminder changes.
- Evidence belongs to the exact project, stage and workflow. Links to sources/assets/beats must be from that project or carried forward from a previously saved record.
- Historical unavailable links are disclosed and retained; no automatic cleanup, bytes copies or fabricated validation.
- New stage data uses the existing source-of-truth `TaskStore.saveStageWork` and Stage Draft journal. No new database table, migration script, backup store, server roles, payment or cloud write.
- Historical absent `scriptBeatId` values decode to blank; existing `CreatorStageEvidence` constructor order is preserved by appending the optional property.
- Plan/Publish editors retain stage-owned records from the authoritative workspace; Board never writes them.
- Read-only archived/finished projects can be browsed; editing must follow existing canonical restrictions.

## Known limits, not falsely claimed solved

- No thumbnails, frame annotations, asset rendering, freeform drag/drop, user-created custom board-card persistence or embedded production playback. Those require a separate data/UX/performance design approval.
- The Board is a connected visual card collection, not a replacement for Milanote, Frame.io or video editors.
- Optional stage-task priority/due date/dependency structures remain unimplemented pending a migration/sync policy review.
- Post-publish learning and output-specific publish validation are UX05.1D.
- Data sync conflict strategy is UX05.1E / UX09.
- No compilation or runtime QA has been run in this staged package.

## Deferred after-UX10 certification matrix

- Compile exact cumulative UX01–UX05.1C branch: Kotlin symbol/type checking, Material icons, Compose dialogs and route callbacks, missing enum branches, minSdk compatibility.
- In-place upgrade from actual V337 fixtures; legacy projects with no `scriptBeatId` remain readable and usable.
- Normal and portable creator backup/restore, stage draft replay and recovery: new source/asset/script beat relationships survive round-trip without duplicating media bytes.
- Evidence add, edit, remove, reorder, link/unlink/relink; reviewer invalidation; both valid and previously unavailable links; rejecting newly fake/cross-project IDs.
- Source and asset links without actual local media after restore: show truthful messages, never remove records.
- Board deep links to Plan tabs/Stage Desk/Script Studio/Publish Studio/Output Craft; Back and process recreation; archived, completed, skipped and active variants.
- Workflow fixtures across film analysis, podcasts, news, technical comparison, education, food, travel, music and unknown/legacy archetypes.
- 0, 1, 500 and 5000 linked cards; source unavailability, long titles, unicode and empty placeholders; no lag or excessive recomposition.
- 320dp phones, tablets, large fonts, landscape, keyboard overlay, TalkBack labels, touch targets, motion settings.
- Preserve all earlier PR tests, reminders, publish/approval integrity and no user-data loss. Fix discovered defects, then rerun affected checks and get explicit merge/release approval.

**Handoff:** Source staged for owner review; UX05.1D is next. No test, certification or merge claims are authorized.

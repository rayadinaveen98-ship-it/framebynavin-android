# Backlot UX05.1 — Advanced Creator Workspace

**UX05.1A–E source advancement staged in draft branches; never compiled or certified. UX06 is next.**
**Parent:** UX05 draft PR #397 / `feature/backlot-ux05-project-workspace-20261117` at `ff6bc49f441f9c1564604d66f0ccc0e570c497c1`.
**Protected:** `stable/backlot-v337-premiere-owner-testing-20261114`. No merges, releases or baseline edits.
**Process:** Stage source changes sequentially; comprehensive build/unit/UI/integration/emulator/physical certification occurs after UX10 as previously approved. Source inspection is not certification.

## Product outcome
A creator can move Idea Vault → creative brief → evidence → script → production → deliverable → real publication → learning without duplicating records, losing unsaved work or mistaking stage progress for published work.

Beginners see the next useful action. Advanced users opt into deeper contextual tools. Keep one canonical project/task/workspace and stable template/stage/deliverable IDs.

## 05.1A — Stage integrity and source-grounded evidence (source-staged in this branch)

1. **Save & Open stage switching:** passing the exact Stage Desk snapshot and expected revision into the Activity, persisting with existing TaskStore, and navigating to Plan/Script/Publish/Output only on successful save. Keep Editing never saves; Discard & Open preserves explicit discard and fail-closed draft-clear handling. No workflow advancement or publishing on navigation.
2. **Review freshness:** in Stage Desk, explicitly checked reviews bind to the exact stage-content snapshot; editing notes, checklist, evidence, decisions or links invalidates the visible review until rechecked. `CreatorStageWorkPolicy.put` also invalidates a carried-forward unchanged review timestamp when stage content changes.
3. **Evidence ownership:** evidence can optionally choose an existing project source and asset. Existing evidence shows the referenced labels or a truthful unavailable marker. The domain policy verifies the evidence IDs belong to this project, preserving previously saved unavailable IDs and never embedding media.
4. No schema migration: `CreatorStageEvidence.referenceId` and `assetId` already existed. No new permissions, paid service, cloud call, reminder changes or destructive migration.

**Files touched:** `CreatorStageWork.kt`, `CW3StageDesk.kt`, `ContentWorkspaceActivity.kt`, this handoff.

**Not yet certified:** Kotlin compilation, visual/navigation QA, concurrency and process-death flows, accessibility, data restore and large project performance.

## 05.1B — Context-aware Cockpit and Brief 2.0 (source-staged in UX05.1B draft)
- Pure read-only, stage-aware recommendations derived from saved project facts: evidence/source gaps, draft script beats, missing or unapproved cut records, missing deliverables, publication readiness and outstanding work. No paid AI calls, fabricated source verification or auto-advancement. Maximum two focus prompts.
- Suggestions route directly to the actual Stage Desk, Script Studio, Output Craft, Publish Studio, Plan → Sources, Tasks or Outputs, or Plan → Brief. Stage primary Continue remains unchanged.
- Existing audience/viewerProblem/promise/angle are canonical. Added **optional** `objective`, `successCriteria` and `constraints` as additive string fields, with defaults for old projects and positional legacy constructors preserved. Existing users are never required to fill them.
- Both authoritative TaskStore JSON and local CreatorEditorDraftStore JSON serialize/deserialize the new fields. Held-draft previews and guarded historical text recovery include them. Non-Plan Publish saves reattach the latest authoritative optional brief fields.
- Brief is collapsible in the hub, and the Plan Brief tab has an optional expand-on-demand editor; no duplicate source of truth and no new platform permissions.
- **Not certified:** Source changes are staged only. Migration, restore, offline, small-screen, compiler and CI compatibility remain to be checked in the after-UX10 certification campaign. No external platform metrics are inferred.

## 05.1C — Connected Creative Board and Stage Modules (source-staged in UX05.1C draft)
- New **Connected Creative Board** is a filtered, lazy-rendered card collection of real project Sources/Assets, stage Evidence, Script Studio beats, planned/ready/creator-recorded published outputs, and cut version records. Cards route to existing canonical editors; no shadow media, independent text editor, drag-and-drop promise or infinite-canvas implementation.
- Uses actual identifiers, with item keys scoped to reference/asset/workflow+stage/evidence/beat/deliverable/cut. Broken old links are shown as unavailable, never deleted. Board is read-only; creators edit and order records inside their existing owners.
- **Contextual Stage Modules** now provide focused questions and optional task suggestions for film analysis, podcasts, tech tests, food production, travel, education, news, capture and editing. Suggested checklist items are explicit opt-in; they save through canonical Stage Desk, not on opening.
- Stage evidence can be edited/relinked in place without replacing its ID, and reordered using Up/Down controls; all changes remain local drafts until an explicit Stage Save.
- Evidence optionally links directly to a **stable canonical Script Studio beat ID**, reusing `CreatorStageEvidence` with an appended `scriptBeatId` defaulting to empty. `TaskStore.encodeStageWork/decodeStageWork` and the existing Stage Draft Store journal retain it, including legacy absent-key fallback. The Stage Work ownership policy rejects new fake/cross-project beat IDs but preserves historical unavailable beat links.
- Preserves Board state through Activity recreation; System Back returns to project hub without a destructive discard dialog, because Board itself is read-only.
- **Not part of this stage:** inline media preview/editor, user-drawn freeform canvas, independent custom board cards, background processing, structured due-date/dependency task schema and expensive video/audio preview hosting. Task metadata extension remains pending architecture review before UX09 sync.
- **Not certified:** compile, UI tests, Kotlin exhaustive paths, persistence round trips, V337 in-place upgrade, TalkBack/320dp/tablet and stress testing remain deferred to the post-UX10 campaign.

## 05.1D — Output-specific review and learning (source-staged in UX05.1D draft)
- **Target-specific stage checks:** explicit Stage Desk linked output IDs determine the intended editing/release output(s). Without a link, one unambiguous root output is a safe implicit primary. If several root outputs exist, or a required historical link is missing, publication approval blocks until the user selects valid stage targets; one unrelated published derivative cannot approve the main output. Editing warnings are scoped to cut versions on selected outputs, never project-wide unrelated approvals.
- The advisory Cockpit next-action and publish readiness hints now apply to the same target selection as the authoritative domain gate. Existing confirmed publication and cut approval histories remain the sources of truth; no automatic publishing, platform queries or stage progress mutations were added.
- **Manual Output Learning Desk:** an append-only, deliverable-owned history for creator-entered expected outcome, observed outcome, what worked, what to improve, next action, and optional metric label/value/source. Learning is available for creator-confirmed live deliverables, including completed but not archived projects; archived/skipped are read-only. It is not a substitute for live analytics or a claimed platform integration.
- New `CreatorOutputLearning` entries are saved using TaskStore's restore-generation and workspace-revision fences, without changing output publication state. Entries are appended, not edited in place. Canonical TaskStore JSON/portable export/restore paths round-trip `outputLearnings` with an absent-key legacy fallback and fail-closed malformed/duplicate record detection.
- Plan/Publish editors explicitly reattach current `outputLearnings` during save, and Plan cannot remove an output with recorded learning. Unsaved reflection and output-switch/navigation require explicit discard.
- Reusable template enhancements and export features are intentionally deferred, pending UX05.1E sync, privacy and migration contract approval.
- **Not yet certified:** compile, lint, JVM and instrumentation tests, V337 upgrade, legacy publication compatibility, multi-root and derivative selection, report/reopen history, process death, accessibility and physical-device review remain to be performed after UX10.

## 05.1E — Integration and handoff before UX06/09/10 (source-staged in UX05.1E draft)
- **Creator workspace integration contract** documents all canonical writers, cross-reference identities, revision/restore-epoch checks, JSON/backup expectations, deletion safety, and a proposed **not approved or implemented** offline-first cloud conflict design for future UX09.
- **Output Learning draft recovery gap closed in source:** a separate `CreatorOutputLearningDraftStore` now journals unfinished reflections locally with project/output/revision/restore-generation fences, matching existing Stage Desk principles. Only TaskStore writes final append-only learning. The exact matching journal is cleared on confirmed save or explicit discard; missing/older-generation entries never silently rebase.
- Learning forms disable inputs and navigation during in-flight writes. System Back, explicit Back and output-switch discard paths now use one Activity-owned generation-checked clear route; failed clears do not claim a successful discard. Source review covered nullable output route, selected output identity, and old revision quarantine.
- **Post-UX10 certification matrix** now defines G01–G25, including cumulative compile, V337 upgrade, restore and draft integrity, stage/cut/publication gates, 320dp/tablet/TalkBack, CI and real-device evidence.
- All UX01–UX05.1D PRs have been confirmed stacked, open, draft and unmerged as of this source review. UX05.1E also remains an unmerged draft. No owner baseline mutation or unrelated Android reminder work.
- Visual tokens, typography, density and motion must be unified during UX10 and then checked on physical devices. Current source inspection is **not** visual certification.
- UX09 identity, hosting costs, database/remote schema, conflict resolution and account deletion require separate founder approval; the architecture draft is **not approval** to deploy paid/hosted services.
- With this source-stage handoff, proceed to **UX06 — Reminder Experience**. Do not merge, release or label an APK stable until the complete after-UX10 gate passes.

**Deliverables:** `docs/engineering/UX051E_CREATOR_WORKSPACE_INTEGRATION_CONTRACT.md`, `docs/qa/UX051E_WORKSPACE_CERTIFICATION_GATE_AFTER_UX10.md`, and UX05.1E handoff.

## Certification gates after UX10
- In-place V337 upgrade with legacy and new projects; preserve entire histories.
- Save & Open save success/failure, stale revision, restore epoch, rapid double-taps, process death and pending draft journal; no navigation on failed save.
- Keep Editing never saves. Discard & Open never silently advances or deletes canonical data; unavailable draft clear fails closed.
- Edit any stage field after reviewed; review becomes invalid until deliberately checked again. Domain-level stale receipt invalidation also works for non-UI writers.
- Reference and asset ownership, unavailable historic links, duplicate evidence references, deletion/reopen/restore, very large link sets.
- Script, Plan, Publish, Output Craft routing and Home Continue consistency, old stage IDs and archived read-only behavior.
- Multi-output publication and target association; no creator publication inferred from stage completion.
- Screens at 320dp, tablets, landscape, keyboard, large fonts, TalkBack, reduced motion, empty/loading/error states, and project-stress performance.
- Kotlin compile/lint and full JVM, instrumented, CI and physical-device suites; fix found defects and rerun affected checks before promotion.

## Release control
This branch is a draft, **not tested/certified/merged/released**. Each subsequent package must record changed files, preservation rules and source-review findings. V337 remains owner-testing baseline until later explicit approval.

# Backlot UX05.1 — Advanced Creator Workspace

**Owner-approved advancement plan; source implementation underway.**
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

## 05.1B — Context-aware Cockpit and Brief 2.0 (pending)
- Current-stage next action based on missing stage-owned materials, script readiness, source verification and per-output publish readiness; no invented intelligence or fake completion.
- Optional creative brief fields for objective, audience, success metric and constraints; preserve existing `audience`, `viewerProblem`, `promise`, `angle` as canonical.
- Stage status labels reflect actual saved facts, never count time worked or infer platform publication.
- Avoid large permanent summary cards; responsive compact and expanded modes.

## 05.1C — Connected creation modules (pending)
- Contextual, optional Stage Desk modules: film scene evidence/shot plan; podcast segment notes; tutorial lesson plan; tech test matrix; travel location sequence; food method/shot plan.
- A lightweight Creative Board built from references to project sources, existing assets and linked outputs. No infinite-canvas dependency, shadow editor or copied media bytes.
- Add structured stage-task priority, due date and dependency only after migration/restore/sync contract is documented and reviewed.
- Link evidence to appropriate Script Studio beat IDs via stable relationships, keeping script text in Script Studio.

## 05.1D — Output-specific review and learning (pending)
- Resolve explicit target deliverable(s) in stage approval checks and editing reviews. Project-wide `any published` must not stand in for the intended output; retain legacy compatibility in migration rules.
- Preserve actual publication and independent per-deliverable correction history, derivative script history and cut approvals.
- Post-publication reflections and manually recorded outcome metrics (no claimed platform integration).
- Reusable templates and exports only if they avoid new privacy/sync/dependency hazards.

## 05.1E — Integration and handoff before UX06/09/10 (pending)
- Design explicit cross-reference IDs, serialization compatibility, restore round trips, offline-first conflict policy and deletion semantics before UX09 cloud sync.
- Global visual tokens, type scale, density and reduced-motion behavior coordinated with UX10.
- Keep current reminder system untouched until UX06.

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

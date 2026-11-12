# Backlot Content Workspace 2.0 — CW5 Workflow and Publication Gate

Date: 2026-11-12  
Status: Source committed on isolated draft branch; CI and physical Android acceptance required.  
Base: CW4 Output Craft at `22e0e0e39c6e6321ccd911edb6a63649dcc0b520`.

## Goal

A creator's five independent facts must remain independent:
1. **Workflow position:** which stage is current, and whether the project is complete.
2. **Stage desk work:** saved notes, evidence, checklist, optional review receipt.
3. **Script Studio readiness:** narration beats that are draft, ready, or locked.
4. **Edit approval:** a specific output cut/version approved for internal review.
5. **Actual publication:** a confirmed live output in Publish Studio, or a separate
   legacy Publication Details record, with its own observed time and link.

None of the last four may silently cause any other event.

## Actual integration

- Centralized pure `CreatorWorkflowTransitionPolicy.assess` with blockers and
  advisory warnings. Both UI and CreatorViewModel use it.
- All Home and Projects **Advance / Complete Step / Finish** entry points now
  open a deliberate creator-approval dialog. Dialog binds to **project ID,
  current stage ID and workspace revision**, shows possible warning overrides,
  and does not auto-advance on screen entry.
- Backend `CreatorViewModel.advanceWorkflow` refuses missing approval identity,
  stale stage, stale workspace revision, archived/skipped/finished or otherwise
  unapprovable projects.
- Every confirmed transition appends a `CreatorStageTransitionReceipt` with
  stable template/stage ID, index, workspace revision before save, timestamp
  and the warning codes explicitly acknowledged. Receipts are stored with the
  original Creator project JSON and backup.
- Existing Plan/Publish editors cannot overwrite approval history.
- A stage approval does **not** change Script Studio beat statuses, stage notes,
  stage checklist statuses, cut approvals, publish history, or output metadata.
  Only actual workflow position changes (and existing reminder re-scheduling).
- CW5's first goal is release truth: **no implicit publication while advancing a
  workflow step**. The legacy `CreatorPublicationEngine.advance` now refuses
  a publication-stage transition without separately recorded release evidence.
  Content Project 2.0 requires at least one real `PUBLISHED` deliverable;
  legacy projects require an actual non-migrated publication date in
  Publication Details.
- `V071WorkflowInlineContent` shows a distinct **Record Publication** action
  when a published-stage workflow has no actual release, and only offers
  stage approval after confirmation.
- Project Pulse's explicit **Stage Done** reminder action uses the same
  transition policy and records a stage approval; it cannot pass a missing
  publication gate. Existing guided/custom reminder timing is retained.

## Blockers versus warnings

| Context | Gate behavior |
|---|---|
| Planned, archived, skipped, completed project | Block advancement until valid active state |
| No independently confirmed live publication at Publish/Send | **Block**; offer real record action |
| Saved stage notes have unfinished tasks | **Warning** with explicit acknowledgment |
| Saved stage notes have not been reviewed | **Warning**; do not fake review status |
| Script Studio has no saved script / draft beats | **Warning**; do not mutate beat states |
| Editing versions exist but none approved | **Warning**; do not auto-approve a cut |
| Final nonpublication stage | Explicit confirmation finishes the project, not the release |

Warnings remain advisory to avoid stranding existing creator projects.
Warnings acknowledged in a successful approval are auditable. Publication
without an independently confirmed record is never an overridable warning.

## Example — Why Silence Works

At Evidence, the creator may retain an unfinished checklist and still
explicitly approve moving to Spoiler Plan after reading a warning. That
approval is logged; the checklist remains unfinished.

At Edit, a saved rough cut may not yet be approved. The creator can review
that warning, approve the stage for workflow purposes, and later approve
the cut independently. Neither action makes the YouTube output live.

At Publish, the stage **cannot** be completed until Publish Studio actually
records a published output. A completed Cut and READY deliverable are not
publication evidence.

At Promote, explicit final-stage approval can complete the project without
inventing a publication timestamp or post-publish rewards.

## Safety/compatibility acceptance

- Old task JSON without `stageTransitions` decodes to an empty list.
- Corrupted or repeated receipt IDs fail closed; original Creator data remains.
- Restore, schema version and existing backup envelope are unchanged.
- Existing backup includes receipts in embedded project JSON.
- New route/app modal has no automatic navigation on opening.
- Real publication continues to be recorded exclusively through existing
  creator-confirmed Studio/legacy publication actions.
- No changes to personal reminders, voice/alarm UI, external media access,
  app signing, version code or release artifacts.
- Focused unit tests verify old and new transitions, stale approvals,
  non-published projects, advisory warnings, duplicate approvals, and Project
  Pulse behavior. Android real backup/restore tests are authored but remain
  for integrated CW6 acceptance.

## CW6 release handoff

CW6 must finish test integration and create a safe, owner-testable APK only
after passing:
- Main Android compile + focused CW1–CW5 and broad regression suite
- Compose UI flow on 320dp phones, tablet and landscape
- Actual stage confirmation screenshot and back/rotation/stale-dialog tests
- Backup/restore with multiple projects and derivative scripts
- Real published output checks including Publish → Reopen → Correct
- Device reminder Stage Done confirmation and exact-step handling
- Signed upgrade key checks with the user's existing install
- Explicit owner validation and no automatic stable promotion

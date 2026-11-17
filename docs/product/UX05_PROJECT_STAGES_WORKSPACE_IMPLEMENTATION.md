# Backlot UX modernization — 05 Project Stages & Content Workspace

**Status: SOURCE IMPLEMENTATION STAGED; NOT COMPILED, TESTED, CERTIFIED, MERGED OR RELEASED.**

Parent: `feature/backlot-ux04-settings-overhaul-20261117` / draft PR #396 at `6c0507230d41127c48d6bb6449062e54396aae28`.  
Feature branch: `feature/backlot-ux05-project-workspace-20261117`.  
Protected baseline: `stable/backlot-v337-premiere-owner-testing-20261114`.

## Business and UX goal

Make Backlot's central workspace an actionable creative cockpit with one next action and honest project context. Do not replace proven CW1–CW5 architecture, invent video/audio editing engines, auto-advance stages, or confuse recorded publication with a completed project.

## Eight packages implemented in source

### 05A — Architecture and preservation
- Audited `CreatorContentWorkspace`, `CreatorWorkflowEngine` and V2 templates, `CreatorStageWorkPolicy`, `CreatorWorkflowTransitionPolicy`, `StageWorkspaceRoutingPolicy`, `HomeContinueRoutingPolicy`, `CreatorStageDraftStore`, Output Craft, Idea Vault conversion and workspace Activity.
- **No persistence schema, workflow template IDs, stage IDs, revisions, canonical TaskStore transactions, restore generations or archived-project rules are modified.**
- Added pure `V405WorkspaceOverviewPolicy`. It derives presentation-only project facts from the canonical in-memory `CreatorTask`; opening a workspace never writes data.

### 05B — Project Cockpit
- Clarified the active workflow stage and percent as **WORKFLOW POSITION**, not universal creative completion.
- Shows the current stage's saved tasks, evidence and link counts, reviewed status, and a single stage-appropriate Continue action.
- Distinguishes read-only completed/skipped/archived projects from active work. Preserves the existing Plan, Script, Publish, Guide and Output Craft tools.
- Project Steps show whether a stage is earlier, current or upcoming, alongside stage-specific saved facts. Opening a different step opens its notes; it **does not** move the workflow.
- Existing staged approval count/testTag is preserved for integration.

### 05C — Contextual Stage Desk
- Extended the single `CW3StageExperiencePolicy` to cover more actual workflow stage types: research/sourcing/verifications, central idea and brief, tests, production planning, travel/food, capture/record, editing, packaging, publishing, distribution and narrative.
- Reuses stage-owned text, checklist, evidence/timecode, decision and shared-material linking fields. **No new per-stage shadow editors or duplicate data stores.**
- Keeps archived, done and skipped stages read-only; existing explicit Save remains canonical and stage work never advances stages.

### 05D — Connections to canonical editors
- Stage Desk now has an actual contextual link to existing shared Plan, Script Studio, Publish Studio or Output Craft.
- Video and audio production contexts may open Output Craft if the project already has outputs. Does not claim built-in rendering/recording/editing.
- Existing shared source, asset and deliverable IDs are referenced without copying bytes, scripts or publication events.
- Missing selected links remain preserved and visible as an unavailable-item count, even if there are no currently available materials. No automatic deletion.

### 05E — Home and Workspace navigation
- Verified that both `HomeContinueRoutingPolicy` and Hub primary action route SCRIPT → Script Studio, PUBLISH → Publish Studio, and PLAN/HUB → actual Stage Desk for working projects. No change needed to the existing source-of-truth stage router.
- A Stage Desk → canonical tool switch requires deliberate **Discard & Open** confirmation; **Keep Editing** returns to the actual draft. The existing Activity handles navigation.
- External Home stage hints are still checked against the loaded project's actual current stage; stale/archived and nonworking projects use existing safe fallbacks.
- Reopening earlier/future stage notes never changes `workflowStageIndex`.

### 05F — Approval clarity and safety
- Added visible stage number and current stage identity in the existing approval dialog.
- Stage warnings must be **explicitly acknowledged** with a checkbox before Approve can be used.
- Blockers remain non-overridable. Publication evidence is still a hard blocker.
- Unchanged `CreatorWorkflowTransitionPolicy.approveAndAdvance` creates only revision/stage-matched recorded transitions. Script readiness, cut approval, stage notes review and external publication remain independent.

### 05G — Idea and output journey
- A converted Idea Vault item now exposes **Open Project**, which opens its exact existing linked `ContentWorkspaceActivity` by the original project ID. Missing project displays a truthful notification, and conversion is never rerun.
- Workspace shows whether it originated from an Idea Vault conversion, counts planned/ready/recorded-published outputs separately, and exposes independent script-beat and edit-version counts.
- A read-only "Stage History" section displays the latest three **recorded approval receipts**, not time worked or publication proof. Older projects without receipts are described accurately.

### 05H — Safety UX and handoff
- Existing Activity-owned draft/discard, stage save single-flight gate, project revision fencing, restore generation and read-only statuses remain in use.
- A tool switch fails closed if a restore generation changed or a stage draft cannot be cleared. It does not silently navigate away from the draft on clear failure.
- Confirmation copy differentiates closing an editor from switching tools.
- Existing empty-stage/linked-material and unsaved-warning states are clarified, without issuing a new migration or backend request.

## Scope and changed files

- `ui/V405WorkspaceOverviewPolicy.kt` — new read-only projection
- `ui/V405WorkspaceSections.kt` — new stage/output/approval summary components
- `ui/V19ContentWorkspaceAlpha6Ui.kt` — cockpit and project steps
- `ui/CW3StageDesk.kt` — contextual stages and canonical tool links
- `ui/ContentWorkspaceActivity.kt` — confirm-before-switch and fail-closed draft navigation
- `ui/FrameByNavinV101BApp.kt` — warning acknowledgement and Idea Vault project navigation
- `ui/V09IdeaVaultUi.kt` — Open Project action on already converted ideas
- This document.

**Excluded:** Reminder engine overhaul (UX06), Personal Spaces (UX07), achievement engine (UX08), cloud sync/backup/account deletion (UX09), global visual tokens (UX10), video/audio encoding, payments, paid AI, permission grants, real user-data or production changes.

## Integrity invariants

1. Task ID is the owner identity; stage data is keyed by stable workflow template ID and stage ID, never title or index.
2. Opening any stage or helper never mutates the project, reminder, completed state, script or output.
3. Stage Desk Save modifies only its allowed stage record at the expected revision and restore epoch; do not accept stale writes.
4. Stage approval requires a fresh project/stage/revision check and separate explicit user confirmation; warnings are acknowledged, blockers cannot be bypassed.
5. A stage approval is not a script readiness event, cut approval, stage notes review, reminder acknowledgement or publication event.
6. Publishing requires its independently creator-recorded actual output via existing Publish Studio/legacy Publication Details. Never infer external live status.
7. Linked media are references, not embedded copies; unavailable historical references remain visible and are not silently deleted.
8. Converted Idea Vault items never create a duplicate project on Open.
9. Old projects without approval receipts are shown as such; no invented time-spent metrics.
10. All existing project drafts, editors, backups and prior project identities remain intact through this source implementation.

## Deferred full certification — execute after UX10

- Kotlin/Android build and lint for exact cumulative head, including Compose types and all new UI routes.
- JVM project/Stage Desk/publish/Idea Vault/restore regression, first-launch and old project fixtures.
- Device upgrade from V337 with existing account, local-only and converted Idea Vault project history.
- Long film-analysis and non-film workflows (podcast, tech, food, travel, business): appropriate stage hints, direct routes, no fake media editing.
- Home Continue vs Workspace action for PLAN, SCRIPT, PUBLISH and unknown stage IDs; stale hint and archived fallback.
- Old/new stage notes, checklists, evidence, decisions and links survive reopen, switching, save, export and restore.
- Stage draft save/discard, rapid navigation and save taps, process death, configuration change and restore-epoch race.
- Approval warnings unchecked/checked, current-stage identity changes, publication hard gate, repeated approval and receipt uniqueness.
- Idea Vault source → linked workspace correctness, missing/deleted project safety, duplicate prevention and no accidental creation.
- Published, ready, planned, derivative and cut-review counts retain exact canonical meaning.
- Layout/accessibility at small phones (320dp), tablets, large text, landscape, TalkBack and input keyboard with visible buttons.
- Resource/performance stress with many stages, references, script beats, outputs and archived projects.
- Physical APK in-place signature acceptance, original reminder reliability, Data & Privacy, account sync and global regression.
- Repair all discovered issues, repeat affected checks, then secure explicit owner approval before merging or releasing.

## Current execution evidence

Feature branch contains code commits and this handoff, but **no compile, unit, UI, integration, emulator, physical-device, CI, release or APK evidence was produced in UX05**. Do not claim completion/certification from source commits alone. PR remains DRAFT and all protected references stay untouched.

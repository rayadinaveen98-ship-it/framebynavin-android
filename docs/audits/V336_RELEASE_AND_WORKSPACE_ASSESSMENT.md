# Backlot V336 — Stability Gate & Content Workspace End-to-End Assessment
Date: 2026-11-12  
Scope: **source-code assessment**, using V336 release-candidate branch; no user's project records were created or modified.

## 1. Release truth
- V336 personalStable owner APK compilation, signing, and its original unit verification passed in the earlier feature workflow (run 42995858381).
- That workflow **failed overall** at compiling the Android instrumentation test APK due to Java/Kotlin compiler heap exhaustion. Android screenshot and interaction acceptance never ran.
- Do **not** call V336 release-stable or merge it to main on APK compilation alone. The separate gate on `release/backlot-v336-stability-candidate-20261112` includes source freeze, signed owner build, workspace/reminder unit suite, isolated Android test APK compilation with increased heap, actual Android UI actions and four screenshot captures.
- Stable criteria: signed upgrade, all four screen captures inspected, unchanged schedules/alarms/audio/callbacks/privacy, no test failures, no install/migration regressions. A cosmetic screen design is not evidence of release reliability.
- No code change to workspace data model is authorized by this audit.

## 2. Actual project architecture
**Current invariant:** one `CreatorTask` is one project. It stores:
- `contentDna` = mode, archetype, platform, delivery format and production styles;
- `workflowStageIndex`, `status`, `progress`, task reminders and attention plan;
- one **project-level** `CreatorContentWorkspace` object.

`CreatorContentWorkspace` has one revision, common audience/problem/promise/angle/hook, script, references, checklist, assets, deliverables, learnings, and one nested `scriptStudio` per project. None has a `stageId` key or a per-stage partition. A project can contain multiple publishable outputs but only one canonical full script studio.

The stage sequence is resolved by `CreatorWorkflowEngine.templateFor(task)`: when Content DNA specifies a supported mode/archetype, `CreatorWorkflowV2` provides a specialized template; otherwise a platform/content-type template handles it. Progress uses stageIndex/number-of-stages, not checklist completion.

## 3. Five actual user journeys
### A. Create a project
Create project via wizard/quick-create, choose content/platform/DNA and due date/attention. It starts with project-level workspace and a stage sequence. The Create flow does not create separate workspace objects for each stage.

### B. Open Content Workspace
`ContentWorkspaceActivity` loads by `EXTRA_PROJECT_ID` from `TaskStore` after `CreatorDataGate` epoch/generation check. Optional HOME_CONTINUE_STAGE_ID is revalidated to avoid opening a stale stage. Default is HUB with four destinations:
- **Plan**: overview, brief, sources, assets, production tasks, outputs, notes.
- **Script**: hooks, titles, beats, creator notes, compiled narration.
- **Publish**: deliverable-by-deliverable title/description/tags/cover, publish gates, live URL, variants, history, derivatives.
- **Guide**: read-only guidance from Content DNA, not a separate editable stage record.
Every stage can access all four; current-stage selection influences which opens first.

### C. Work on a stage
`V19ContentWorkspaceAlpha6Hub.workspaceActionForStage` maps script/narration/outline/draft/commentary/guide_script to Script, packaging/thumbnail/metadata/upload/cover/etc. to Publish, everything else to Plan. Hub shows current stage, N/M stage position, a Continue action and at most two project-content focus hints. `V2285WorkspaceFocusPolicy` prioritizes hints for the current destination, but the underlying checklist, assets and script remain shared.

**Important route mismatch:** `HomeContinueRoutingPolicy.destination` recognizes only idea/research/outline/angle/select as Plan, only `script` as Script and only `published` with deliverable publication as Publish; all other stages route to the Hub. Thus tapping **Work On** at Packaging opens Hub, while tapping **Continue Packaging** in that Hub opens Publish. "Work On" does not reliably open the same editor as the Hub's Continue.

### D. Save and resume
Plan autosaves a local draft after ~350 ms; Script and Publish do likewise. Canonical project saves are explicit via Save/Save & Next. `ContentWorkspaceActivity` uses one save-in-flight gate, workspace revision comparison, data generation/restore guard, and project-scoped update transaction. Script Studio saves both its own revision and the project workspace revision. Historical drafts are held separately if the source revision changed, not silently merged into newer data. Save & Next routes to Script/Publish only **after** a successful durable save. Closing unsaved changes can require confirmation.
`TaskStore` serializes project workspace and script into JSON for export/restore. Linked source/media entries are references (HTTP(S) or Android SAF content URIs), **not media bytes**; their availability on a different device after restore is not guaranteed.

### E. Finish a stage or publish
Workflow progression is separate from content editing. `CreatorWorkflowEngine.stageIndex` tracks the current stage; `CreatorViewModel.advanceWorkflow` updates project stage/progress and reminders. `Plan.productionProgressPercent` measures optional checklist status, not project progress. `ScriptStudio.progressPercent` measures script beat readiness, not project progress.
When deliverables exist, `CreatorPublishWorkflow.publish` confirms specific outputs after required gates, title and explicit publication action; it appends publication history. Parent project's published data is synchronized separately; publication does not automatically finish or advance the entire project. The Workflow UI blocks the ordinary Publish-stage completion action without a live output, but the lower-level `CreatorPublicationEngine.advance` path itself does not enforce that condition for a deliverable-backed project. Audit all stage advancement entry points before hardening this as a universal invariant.

## 4. Synthetic nine-stage user example (NOT written to user data)
Project: `Why Silence Works — Filmmaker POV Review`. DNA: `film_entertainment/review`, platform YouTube, long-form. Specialized `CreatorWorkflowV2.filmReview` stages:

| # | Current stage | Hub Continue destination | Home 'Work On' destination when WORKING | Expected ideal stage surface |
|---|---|---|---|---|
| 1 | Watch Notes (`watch_notes`) | Plan | Hub | Viewing log, timestamps, quotes and notes |
| 2 | Verdict + Angle (`verdict`) | Plan | Hub | Thesis, verdict, audience fit |
| 3 | Evidence (`evidence`) | Plan | Hub | Scene evidence table, timecode/source/claim |
| 4 | Spoiler Plan (`spoiler_plan`) | Plan | Hub | Spoiler-safe section boundaries |
| 5 | Script (`script`) | Script | Script | Beats, narration, selected hook, status |
| 6 | Edit (`edit`) | Plan | Hub | Cut versions, edit checklist, exports, notes |
| 7 | Packaging (`package`) | Publish | Hub | Title/thumbnail variants, copy, QC |
| 8 | Publish (`published`) | Publish | Publish when deliverables exist | Required checks, live URL, publish event |
| 9 | Promote (`promote`) | Plan | Hub | Platform-specific derivative outputs, promotion links, learnings |

A single Plan checklist is visible in all stages. A saved source in Evidence is still visible during Edit; a Script beat survives stage advancement. However, there is no stage-specific asset board, evidence table, edit tracker or promotion tracker built into Workspace today.

The stage number increases **only when the user advances workflow**, not merely when editing Script or ticking a checklist box. Going Back moves stageIndex without replacing the workspace. Stage-specific reminders can be re-evaluated by Project Pulse, but cannot substitute for saved creative work.

## 5. Benefits worth preserving
- One project ID and shared materials; no duplicate plans on each stage.
- Differentiated workflow templates from creator mode/archetype; fallback for legacy projects.
- Purpose-specific Script and per-output Publish editors.
- Parent project completion is separate from per-output publication, with history and protected rights checks.
- Revision-based conflict/restore safeguards and controlled navigation after save.
- Non-prescriptive focus hints; users can switch between tools without losing work.
- Links to external files instead of forcing users to upload/copy large media into Backlot.

## 6. Confirmed friction, severity and proposed behavior
**P0 / release reliability:**
1. V336 native visual/emulator gate unresolved (compiler OOM). Do not label stable until evidence and device install verified.
2. Media links/SAF content URIs are metadata, not portable media backup. Surface clear offline/restored-link availability and repair affordance; never imply all media is synchronized.

**P1 / creator UX correctness:**
3. Home Work On vs Hub Continue routing differs. One shared stage→destination resolver should serve all entry points.
4. Stage work currently lacks stage-specific records. Add optional stage-owned cards with stable stage IDs: evidence, edit versions, exports, review notes, promotion checklist; keep project-wide materials shared.
5. Different stages show almost the same Plan interface. Make stage goals, next relevant materials, and completion evidence prominent without hiding other tools.
6. Project progress by stage count can mislead users about actual effort. Show 'Stage 6 of 9' separate from readiness (checklist and script) and publishing status.
7. Publication completion gate should be enforced at data/domain level for deliverable-backed projects across every advancement path, not just one UI.
8. Multiple deliverables share one canonical Script Studio; a derivative Reel may need its own script variant without overwriting the main film review script.
9. A checklist item has no `stageId`; tasks cannot be automatically filtered/reviewed by stage, and completed checklist work does not prove stage completion. Introduce optional stage-tag and explicit stage-complete confirmation (no automatic rewriting of existing data).

**P2 / usability:**
10. 'Guide' largely depends on content DNA/archetype, not current-stage proof of work; add genuinely stage-contextual guidance and contextual empty states.
11. The seven Plan tabs can feel like one overwhelming generic form. Default to Current Stage with relevant sections, while retaining 'All materials'.
12. Show saved/unsaved status prominently, especially when switching Plan→Script→Publish or returning after backup restore.
13. Add reusable per-stage evidence/decision snapshot and stage timeline; moving Back should not erase what was previously completed.
14. Narrow screens and high font scales need section-jump affordances and keyboard-aware scrolling.

## 7. Recommended architecture (not implemented)
**One project → one shared library → stage workspaces as filtered, linked views:**
- `ProjectWorkspace` (existing) continues to own common brief, script, sources, media links, output publication records and revisions.
- `StageWork` added additively keyed by `projectId + stable stageId` (not just stage index): optional task checklist, notes, linked assets, evidence/output pointers, stage-specific review state and dated versions.
- A single `StageDestinationResolver` maps stage types to primary modes: IDEA/RESEARCH → Plan; SCRIPT → Script; CAPTURE/EDIT → Production; PACKAGE/PUBLISH → Publish; PROMOTE/LEARN → Distribution/Insights. Unsupported custom stages fall back to Hub with honest copy, not a fake editor.
- `Current Stage` surface changes its *content*, not Backlot's global visual style; retain the four core areas and add a contextual production workspace as needed.
- `Asset`, `Source`, `Output` references can be linked to multiple stages without copying media or detaching them from the project.
- Per-stage reminders continue to use existing Project Pulse policy. Stage completion is **explicit**, with guarded criteria and optional evidence; no automatic completion on draft save, file attachment, publish form editing or timer usage.
- Backups/migrations must preserve existing `CreatorContentWorkspace` and read old projects unchanged; stageData starts empty until owner creates it.

## 8. Evidence from external workflow research
- Atlassian Jira distinguishes the project/space container from the board/workflow view and permits reusable workflows across task types: https://www.atlassian.com/software/jira/guides/boards/overview
- Atlassian recommends keeping editorial workflows simple rather than making every minor variation a new workflow: https://support.atlassian.com/jira-core-cloud/docs/how-to-create-workflows/
- Frame.io's editorial guide distinguishes asset organization, editing versions, review/feedback, delivery, and preservation of files: https://workflow.frame.io/guide
- YouTube's official Upload guide separates copyright checks, metadata and visibility, and warns copyright checks are not final: https://support.google.com/youtube/answer/57407

## 9. Product acceptance matrix before any Workspace refactor
1. New project with Content DNA + older legacy project with platform/template fallback.
2. Both Home Work On and Workspace Continue route to the exact same correct editor for *every* stage.
3. Opening a stage and moving backward/forward keeps Plan sources, Script beats, output history and saved files.
4. Two projects with the same title never share drafts or notes; all indexing uses project UUID.
5. Two outputs for one project can have different metadata and derivative scripts, without cloning the main narrative or overwriting publish history.
6. Checklist Done, Script Ready, Deliverable Published, and Project Done remain separate confirmed states.
7. Publish stage cannot advance without explicit published evidence for deliverable-backed projects, through any callback.
8. Draft recovery, conflicting workspace revision, cloud restore during editing and archived project are tested.
9. Small phones, tablets, 200% font, keyboard, TalkBack and offline media link failures remain usable.
10. No edits to alarm, notification, reminder scheduling, audio playback, save/restore schema or stored stage index without a dedicated compatibility review.

This is **an audit and proposed plan only**; no new workspace screens or project records have been created.

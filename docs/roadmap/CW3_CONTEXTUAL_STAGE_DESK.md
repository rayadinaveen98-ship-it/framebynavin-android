# Backlot Content Workspace 2.0 — CW3 contextual stage desk

Date: 2026-11-12  
Status: Implemented on draft branch; Android acceptance and release review still required.

## User-facing difference

CW1 maps project stage types to existing Plan, Script, Publish and Hub tools.
CW2 adds project-scoped, stage-owned records without changing prior project data.
CW3 makes those stage records accessible in a real native Compose Stage Desk.

### User journey: new film review

Create **Why Silence Works**, Content DNA film_entertainment / review:
- Watch Notes: stage desk with viewing journal, scene/timecode entries, stage checklist.
- Verdict: stage desk prompts for central angle and supporting observations.
- Evidence: stage desk supports evidence/claim cards, timecodes, linked project
  references/assets and dedicated stage notes.
- Spoiler Plan: guided spoiler map, own checklist and decisions.
- Script: existing structured Script Studio is still the primary action; the
  stage desk is available from the Hub for script-specific notes.
- Edit: Stage Desk becomes the primary action; use it for edit version notes,
  unresolved tasks and review evidence. It is **not** a built-in video editor.
- Packaging: existing Publish Studio remains the primary tool, with Stage Desk
  accessible separately from the Hub.
- Publish: Publish Studio remains publication authority, never Stage Desk.
- Promote: distribution/follow-up desk with its own checklist and links.

For every stage, the Hub's expanded project-stage list now has a read/write
Stage Desk entry; switching between stages does NOT advance the workflow.

### Visual and interaction contract
- Native Android Compose using the existing Backlot Cinema surfaces and crimson
  highlights, existing adaptive workspace frame and 48dp+ navigation controls.
- Project stage name, specific objective, conditional instructions.
- Separate notes, interactive checklist with user-selected suggestions,
  evidence/timecode cards, linked shared source/asset/output IDs, decisions.
- Read-only mode for completed/skipped/archived projects; planned/working
  projects may edit.
- Explicit **Save Stage Work** (no premature canonical mutations); draft-only
  auto-journal after 400 ms, scoped to project ID/template ID/stage ID/workspace
  revision and restore generation. A successful save uses TaskStore's CW2
  transaction and clears the local draft; Back offers explicit discard.
- Same shared project data and Script/Publish authority. No media bytes copied,
  no reminder or alarm changes, no stage auto-completion.

### Safety and navigation
- Home Continue now opens Stage Desk for planning, evidence, editing and other
  stages where no dedicated editor exists; Script/Publish continue to use their
  existing tools. Both Home and Hub derive the class of stage from the shared
  `StageWorkspaceRoutingPolicy`.
- A stale Home stage hint still falls back to Hub.
- Stage saves fail closed if workspace revision/restore epoch changed.
- A stage cannot link to a newly unknown/foreign source, asset or output ID.
  Historical links may be preserved if the referenced shared item was deleted
  after it was originally linked.
- Notes, task completion and stage review receipts are NEVER treated as
  creator workflow advancement or proof of publication.
- Existing Plan/Publish saves preserve CW2 stage records.

### Regression checks
1. Existing CW1/CW2 Home and workspace unit tests.
2. CW3 stage presentation policy for film, tech, edit, script and publish.
3. CW2 stage persistence and foreign-asset link tests.
4. Native Compose CW3 device test for editing the Evidence desk, saving only a
   stage record, Edit desk guidance, screenshot.
5. Android end-to-end should inspect real 320dp phone, tablet/landscape,
   accessibility, rotation/draft restore and upload/restore validation.
6. Signed owner APK must use correct upgrade certificate and preserve user data.

**Acceptance gate:** Tests and signed Android device evidence are pending; do
not merge/promote based only on this source implementation.

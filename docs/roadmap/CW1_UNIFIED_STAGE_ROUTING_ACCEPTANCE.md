# Backlot Content Workspace 2.0 — CW1 Unified Stage Routing

Date: 2026-11-12  
Status: **implementation branch, pending CI and owner acceptance**  
Source base: V336 stability candidate (not promoted to stable)

## Owner objective
One project and one shared workspace. Stages must lead to the actual relevant existing
tool, never a fake stage editor. Keep all saved work intact and make Home **Work On**
and the Content Workspace's primary stage action agree on supported destinations.

## What CW1 actually changes
- Adds `StageWorkspaceRoutingPolicy` as the sole mapping from a workflow stage ID to
  the existing Plan, Script, Publish, or Workspace Hub destination.
- Home's working-project deep link uses this same mapping, preserving stale-stage,
  archived/non-working and data-generation safeguards already enforced by the Activity.
- Workspace Hub's primary action uses this mapping instead of its former private
  script/publish ID tables.
- Unimplemented production-specific tools (Capture, Edit, Record, Promote) and unknown
  custom stages intentionally enter the existing Hub from Home. The Hub explicitly
  says no dedicated editor exists and provides honest access to shared materials.
- Changes are navigation/copy only; no new fields, stage records, workflows, alerts,
  audio behavior, publication mutations, backups, or restored data changes.

## Sample: film-review project
Content DNA: film_entertainment / review; YouTube long-form.

| Stage | CW1 Home destination | Hub primary tool | Stage-specific editor built? |
| --- | --- | --- | --- |
| Watch Notes | Plan | Plan | No, shared Plan |
| Verdict + Angle | Plan | Plan | No, shared Plan |
| Evidence | Plan | Plan | No, shared Plan |
| Spoiler Plan | Plan | Plan | No, shared Plan |
| Script | Script Studio | Script Studio | Existing structured Script Studio |
| Edit | Workspace Hub | Open shared project materials | No |
| Packaging | Publish Studio | Publish Studio | Existing metadata editor |
| Publish | Publish Studio | Publish Studio | Existing output publication editor |
| Promote | Workspace Hub | Open shared project materials | No |

**Important nuance:** For stages without a dedicated editor, Home deliberately enters
the Hub, while the Hub offers the *shared Plan* as a truthful fallback. We do not
pretend a custom editing/promoting experience is already implemented.

## Scenarios that must pass
1. Working film review opens the same existing stage tool in Home and Hub for all
   supported Plan, Script and Publish stages.
2. Packaging never routes Home to Hub while the Hub opens Publish.
3. Legacy YouTube long-form Research → Plan, Script → Script, Thumbnail → Publish;
   Record/Edit/Promote → Hub (no fictitious editor).
4. Working tech review Test Plan/Evidence → Plan, B-roll Capture → Hub.
5. Working gaming guide Narration → Script, Edit → Hub, Publish → Publish.
6. Changing stage while Home is loading does not silently open an old editor.
7. Planned/done/skipped/archived projects do not deep-link into a working editor from Home.
8. Routes are read-only and never advance workflowStageIndex or modify Content Workspace.

Focused tests:
- `HomeContinueRoutingPolicyTest` (existing preservation scenarios)
- `StageWorkspaceRoutingPolicyTest` (new nine-stage/legacy/tech/gaming matrix)
- GitHub workflow `backlot-cw1-stage-routing-unit.yml`

## Explicitly NOT delivered by CW1
- Stage-owned notes/evidence/edit versions (CW2/CW3)
- Dedicated capture/edit/record/promotion screens
- Per-deliverable narration and derivative script variants
- Automatic stage completion or publication
- A new or stable user-facing APK
- Device/UI regression acceptance (save for the final integration gate)

## Next stage
CW2 should introduce optional stage-owned data keyed by stable project ID, stable
stage ID and workflow-template identity. Link existing shared assets by reference
without copying media; old projects must load with empty stage data. Before
migration, verify portable backup round trips, revision checks, archived projects,
restore-with-editor-open, cross-project isolation and media availability disclosure.

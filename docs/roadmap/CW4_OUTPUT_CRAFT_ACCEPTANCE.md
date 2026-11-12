# Backlot Content Workspace 2.0 — CW4 Output Craft

Date: 2026-11-12  
Status: Source implemented on an isolated draft branch; CI and emulator acceptance pending.  
Base: fixed CW3 source commit `95520468371378aec4783d89da612dc8302b7b0b`.

## Goal

One creator project can produce the main YouTube video plus multiple independent
Shorts/Reels/other outputs. The original Script Studio continues to own the
main narration. Each derivative can have its own independent narration,
version history, output metadata and cut review trail without duplicating
project-wide assets or changing publication records.

## Example: Why Silence Works

- Project: `Why Silence Works`, film review.
- Main output (`main`): 10-minute YouTube review, canonical Script Studio.
- Derivative (`reel`): Instagram Reel with a separate 35-second narration,
  optional explicit one-time copy from parent script as a starting point.
- Derivative (`short`): YouTube Short with a *different*, separate script.
- `reel` v1 narration remains in history when the creator saves v2.
  Changing `reel` does not change `short`, `main` or their publication state.
- Main cut and vertical cut have independent records and labels:
  `Rough Cut v1` -> `REVIEW` -> `APPROVED`, with review timestamps and
  notes. An approved record is immutable; a revision must be a new cut.
- Output Craft is a fifth, clearly labeled tool in the project Hub. The existing
  Plan, Script, Publish and Guide remain accessible and unchanged.

## Real Android UI delivered
- Output selection, summary, main vs derivative distinction and live-output notice.
- Independent hook, narration, visual notes and creator notes for derivatives.
- Explicit one-time parent-script copy when beginning a derivative.
- Saved script revision history, showing recent snapshots.
- Per-output cut list, explicit review/approval, cut labels/notes/HTTPS references.
- Android SAF document selection with persisted read permission; only a URI is
  stored in project data. A cross-device restore may lose the actual source media.
- A local output draft journal bound to project ID, output ID, workspace revision
  and restore generation. It cannot auto-replay a stale draft after a restore.
- Explicit Save actions for scripts and cuts; navigating away from unsaved work
  requires confirmation. Independent sections prevent saving a script and
  unintentionally invalidating an unsaved cut draft.
- Read-only Output Craft in completed or archived projects.

## Data safety
- `CreatorContentWorkspace.outputScripts` and `editVersions` default to empty.
  Older backups without the new fields load without migration writes.
- `TaskStore` serializes/deserializes both fields with the existing project JSON
  used by Creator backups. Script history is append-only and verified during
  decode; corrupt or duplicate records fail closed.
- `TaskStore.saveDerivativeScript`, `addOutputEditVersion` and
  `reviewOutputEditVersion` use the existing Creator write lock, restore epoch
  and workspace revision checks, incrementing the workspace revision once.
- `CreatorContentStabilization` and the live Plan/Publish editors preserve
  existing Output Craft histories. They block deletion of outputs that still
  own scripts or cut-version history.
- Approval of a cut does **not** mark it published or advance the project.
  Only Publish Studio can record a live release. No output bytes are stored,
  no embedded media editor or automatic production is claimed.
- No reminder, alarm, voice, challenge, project schedule or signing changes.
- Work is in a draft PR, not merged into the stable Android release.

## Test acceptance
- JVM `CreatorOutputCraftPolicyTest`: main script isolation, independent
  derivative scripts, append-only revisions, cross-output safeguards, cut
  review progression and immutable approved cuts, deletion restrictions.
- Android `CreatorOutputCraftPersistenceAndroidTest`: full serializer
  roundtrip, old-backup defaults, corruption fail-closed, guarded TaskStore
  transactions and actual Creator backup restore.
- Android Compose `CW4OutputCraftDeskUiTest`: synthetic Reel script and main
  cut entry, stage/outcome isolation and native screenshot.
- CI workflow `backlot-cw4-output-craft-unit.yml`: scoped compilation and
  CW1–CW4 unit suites.
- Emulator and real-owner upgrade/restore validation remain at the final
  integration gate, after all approved workspace features are implemented.

## Next scoped steps
CW5: strengthen completed/published transitions and explicit creator stage
approval. Preserve separation of project workflow stage, stage checklist,
Script Studio readiness, output cut approval and publication state.
CW6: integrated QA on real Android devices, upgrade signing, backup/restore,
bilingual text, scrolling, TalkBack, multiple projects and release acceptance.

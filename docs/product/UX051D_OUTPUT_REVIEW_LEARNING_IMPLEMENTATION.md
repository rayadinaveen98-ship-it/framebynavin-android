# Backlot UX05.1D — Target-Specific Output Review and Manual Learning

**Status:** SOURCE-STAGED / NOT COMPILED / NOT TESTED / NOT CERTIFIED / NOT MERGED / NOT RELEASED

**Parent:** UX05.1C draft PR #400 at `551335fbd113db02cd6164f78c2557c3efd34e26`.
**Branch:** `feature/backlot-ux051d-output-review-learning-20261117`.
**Owner baseline:** `stable/backlot-v337-premiere-owner-testing-20261114` preserved; no hosted, Android testing data, reminders or release changes.

## Purpose

Resolve a high-risk inconsistency: publication gates and cut-review warnings previously looked project-wide, so a different published derivative or approved cut could satisfy the current stage accidentally. Attach each stage to the intended output(s), then give the creator an append-only, manual learning log per actually published deliverable.

## Implemented in source

### 1. Target-specific stage policy

- `CreatorOutputTargetPolicy.select` uses the current stage's saved `linkedDeliverableIds` when present. Those references must belong to the same project; missing/stale IDs block publishing and warn for editing until reviewed.
- When no explicit stage target exists, a **single root** deliverable is the implicit main target, even with derivative outputs present. This preserves the straightforward case. Multiple independent roots are ambiguous; the stage must link its intended outputs in the existing Stage Desk → Linked Shared Materials → Outputs UI.
- The Publish transition is blocked unless **all selected** outputs have both `PUBLISHED` status and a positive creator-recorded publication timestamp. Publishing a derivative cannot authorize an unpublished primary. Legacy tasks still use their pre-existing parent publication compatibility path if `usesDeliverablePublication(task)` is false.
- Editing warnings evaluate approved cut versions only for the intended output IDs, rather than looking for any approved version anywhere in the project. They remain **warnings**, not newly invented hard approval requirements.
- `V2285WorkspaceFocusPolicy` shares the same output resolution, including stage-specific cut hints and filtered publish readiness. It never changes stage state.

### 2. Append-only manual learning

- New `CreatorOutputLearning` belongs to one `CreatorDeliverable.id` and records expected outcome, observed outcome, what worked, improvement, next creative action, and optional manual metric label/value/source.
- Recording is allowed only on a currently confirmed published deliverable, in a non-archived/non-skipped project. A completed project can still collect retrospective learning. No online analytics, social credentials, auto-metrics or paid service is used.
- `CreatorOutputLearningPolicy.append` validates output identity and publication state, unique learning ID, positive timestamp, a meaningful reflection, metric consistency, and bounded text lengths. Existing snapshots are immutable; any later insight is a new record.
- `TaskStore.appendOutputLearning` uses the pre-existing restore-generation fence and optimistic workspace revision check, increments the canonical revision, then saves atomically with the rest of CreatorTask. Failed writes must leave the unsaved editor text available, not claim success.
- Canonical project JSON now includes `outputLearnings` as an array. Older absent keys decode to empty; malformed arrays, missing IDs, nonpositive dates and duplicate learning IDs fail closed rather than silently losing historical notes. Existing normal/portable creator export/import routes carry the same TaskStore payload.
- Plan/Publish editor saves reattach authoritative learning history. Plan removal of a deliverable with output learning is blocked; existing derivative scripts, cut versions and publication histories remain protected.

### 3. Output Learning Desk

- New `CW52OutputLearningDesk` opened from the existing Creator Cockpit. It selects a saved deliverable, exposes a manual reflection form only when actual publication is recorded, and displays append-only historical reflections newest first.
- A user can record a manual metric with its source/observation timing. The screen expressly distinguishes this from live connected platform data.
- Switching outputs with an unfinished reflection requires explicit discard; the Publish Studio route also confirms discard when necessary. Empty project state routes to canonical Plan → Outputs.
- `rememberSaveable` retains form fields through supported Android saved-state recreation, but is **not** a durable cross-device draft journal; full process-death/restore/retry acceptance remains pending.
- Completion of a project is not undone by learning. No stage transition, publish event, reminder or reward is created.

## Files changed

1. `data/CreatorOutputTargetPolicy.kt` — output selection and current live requirements
2. `data/CreatorWorkflowTransitionPolicy.kt` — output-scoped review and Publish gate
3. `data/CreatorOutputLearning.kt` — append-only observations and strict validation
4. `data/CreatorContentWorkspace.kt` — append-only optional learning array field
5. `data/TaskStore.kt` — canonical JSON serialization and optimistic/epoch safe append
6. `data/CreatorContentStabilization.kt` — Plan data-protection fence
7. `ui/V2285WorkspaceFocusPolicy.kt` — consistent contextual hints
8. `ui/CW52OutputLearningDesk.kt` — new manual reflection UI
9. `ui/ContentWorkspaceActivity.kt` — LEARN route and guarded save, Plan/Publish preservation
10. `ui/V19ContentWorkspaceAlpha6Ui.kt` — Cockpit entry
11. `docs/product/UX051_ADVANCED_CREATOR_WORKSPACE_ROADMAP.md` — roadmap state
12. This handoff

## Explicit scope exclusions

No live analytics API, YouTube analytics overwrite, video upload, publication automation, new paid integrations, edit-review hard gate, destructive migration, new media storage, changes to stage IDs, existing Original/Portable backup UI, or account/cloud sync behavior. The root-output fallback and multi-root ambiguity rule are a **proposed behavior change pending complete legacy/multi-output certification**, not a certified current-app policy.

## After UX10 certification gates (NOT run)

- Kotlin/Compose compile, lint, static/API compatibility; complete cumulative UX01–UX05.1D regression.
- Legacy V337 in-place upgrade, project JSON round trips, older missing `outputLearnings`, malformed/duplicated records, unicode/multiline/long text, normal/portable/unified creator restore.
- Primary output unpublished, derivative published: cannot approve intended primary. Single root plus derivatives: fallback chooses root. Multiple roots with no explicit links: cannot silently approve. Explicit multiple targets: each must be live. Missing historical stage links: fail closed. Legacy parent-only published state: no unintended regression.
- Edit warning only for targeted cuts, not unrelated approved versions. Review version transitions stay immutable. Stage receipts and reward/reminder behavior unaffected.
- Recording learning on planned/ready/published/reopened output; archived/skipped/finished projects; save conflicts, restore epoch changes, double taps and retry.
- Source-draft Plan/Publish save cannot erase output learning or delete output with linked learning; unrelated project edits never alter the entries.
- Process death/rotation, Back/Discard, output switching, empty states, scroll/keyboard at 320dp, tablet, landscape, TalkBack and large font.
- Large history/card counts; user-visible error messages; no fake success; manual metric source labelled as user-entered.
- Full CI/emulator/physical APK acceptance remains owner-gated after UX10; no merges/releases without explicit approval.

**Next:** UX05.1E — integration review, ID/serialization/backup contracts, offline/multi-device conflict design and global UX handoff before continuing the original ten-point roadmap.

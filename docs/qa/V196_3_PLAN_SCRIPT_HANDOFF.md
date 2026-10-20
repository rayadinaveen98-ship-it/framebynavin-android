# Backlot V196.3 — Phase 3A Plan → Script Handoff

Branch: `feature/backlot-v196-3-plan-script-handoff`. Inherits V196.2 UI, V196 P1 safety, frozen V195.5 unchanged. Preview versionCode `206` (`2.5.6-v196.3-plan-script-preview`).

## Functional behavior

- Plan footer has **SAVE** (original save + Hub) and **SAVE & SCRIPT** (same durable Plan save, then Script navigation).
- The handoff passes the same complete project draft through the existing canonical TaskStore transaction, normalization, revision/generation checks and safety protection. It **does not navigate on write failure**; error state remains visible and unsaved editor drafts are not cleared by a failed save.
- Script Studio displays a collapsed **PLAN BRIEF** read-only disclosure when the saved project has audience, viewer need, promise, angle or hook. No duplicate persisted brief, no sidecar notes, no secret write to script beats or title alternatives.
- Hook reflects selected structured Script Studio hook where available; falls back to legacy canonical workspace hook, without changing it.
- No workflow stages, task status, script status or publication status advance automatically.

## Automated acceptance
- `CreatorPlanScriptBridgeTest`: stable field order, trimmed non-empty values, selected structured hook precedence, no mutation of scripts, absence of empty context, legacy hook fallback.
- `V196PlanScriptHandoffUiTest`: SAVE & SCRIPT supplies the current draft and expected revision through the callback, and Script shows saved Plan fields when expanded.
- Phase 3 workflow enforces the persisted-save-before-navigation source contract and executes inherited V161–V195.5, V196 P1 and V196.2 guards, all JVM unit tests, lint, APK assembly, instrumentation compile and Android emulator UI checks.
- Emulator runs Workspace UI tests earlier than graphics-heavy reminder tests, to isolate user-facing assertions if the Android emulator crashes in unrelated particle tests.

## Manual acceptance before stable
1. Enter Plan from an existing project, edit brief, tap **SAVE & SCRIPT**. Script displays **PLAN BRIEF** with the saved values, retains prior hooks, titles, beats and notes.
2. Tap **SAVE** instead. Returns to Hub; no Script screen launched.
3. Force a revision/generation save conflict or data failure: do not open Script; previous saved data remains; user can recover unsaved draft.
4. Open Script directly without plan brief: no empty panel, no invented content.
5. Confirm selected Script hook takes precedence over obsolete legacy hook; publishing and planned outputs unchanged.
6. Stress 20+ beats, rotate device and test back navigation/draft recovery; verify Backlot reminder schedules and existing projects unchanged.
7. Install in place from V195.5/V196/V196.2 using same signing identity. Never delete live app data to bypass signature problems.

## Out of scope
Script → Publish metadata transfer, direct source/asset opening, and next-step suggestions are follow-on Phase 3B/3C tasks. This preview must not be marked stable without successful CI and physical device testing.

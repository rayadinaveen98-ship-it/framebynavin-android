# Backlot V196.2 — Content Workspace Phase 2: Minimal UI

Based on V196 Phase 1 candidate `087a197bd8fdfe0386f29ebf1f05139078aa30d0`; V195.5 stable remains frozen.
Candidate branch: `feature/backlot-v196-2-content-workspace-minimal-ui`.
Version: `2.5.5-v196.2-workspace-ui-preview`, versionCode `205` (same package and preview signing identity).

## Design goals
- One current workflow step, one progress value that reflects workflow authority, and one prominent action.
- A lean project header, four equally prominent tools (Plan, Script, Publish, Guide).
- Keep the detailed stage timeline opt-in, not repeated above every tool.
- Remove technical revision numbers and explanatory filler from everyday screens.
- Maintain readable typography, consistent section labels, and useful touch targets.
- Do not alter persisted model, reminders, projects, publication history, or backup.

## Implemented surfaces
1. **Workspace Hub:** project name and platform; truthful workflow progress; current step; one context-sensitive button; 2×2 tool layout; expand/collapse full stages. Existing callbacks, workflow resolver and project ownership unchanged.
2. **Plan:** remove redundant read-only Script sub-tab (the full Script editor remains independently accessible); rename tabs to plain user language (Sources, Tasks, Outputs, Notes); remove redundant jump-to row; compact project overview; increase heading, chip, metric and field label sizes. All editing and save paths remain.
3. **Script:** concise section names; larger heading/metric/beat text and status chips; keep hooks, titles, beats, narration, notes, preview and save intact.
4. **Publish:** concise sections and output tabs, larger summary and field labels; all protected gate, derivative and publication history interactions remain.
5. **Guide:** concise heading structure, readable prompts; turn inert recommendation/output chips into non-interactive labels. No claim that suggested actions perform work.

## Regression / acceptance
- Source guard enforces new Hub layout, removal of the duplicated Plan script tab, no inert Guide click targets, and continued V196 P1 safety protection.
- New `V196WorkspaceMinimalUiTest` verifies a completed optional checklist cannot turn 0% workflow progress into 100% and that Plan can be opened, and verifies expandable workflow steps.
- Inherited V161–V195.5 guards, V196 P1 checks, unit tests, lint, APK assembly, instrumentation compilation and emulator suites run in `.github/workflows/backlot-v196-2-content-workspace-p2.yml`.
- CI execution on the final commit and physical-device checks are release gates, not assumed passed because the workflow file exists.

## Manual device sign-off
- Confirm Project opens from Studio and Today, shows correct workflow progress for planned/working/finished projects.
- Open each Hub tool and return; reopen the same project with no lost edits.
- On small screens and 100%, 130%, 200% fonts verify no inaccessible buttons, clipped titles, overlapping navigation or keyboard obstruction.
- Populate 10+ references, 20+ script beats, a long title, many deliverables and publication events. Verify scroll, saving and Back/discard behavior.
- Confirm earlier projects, reminders, publication history and local files survive in-place upgrade from V195.5/V196, with unchanged signing certificate.
- Exercise protected publication gate, derivative deletion confirmation and recovery of unsaved edits.
- Validate Guide readability and that its suggestion labels cannot be clicked. Check both dark and light modes.

Do not mark stable until all automated and physical-device checks are completed, visual results reviewed and user acceptance recorded. Phase 3 cross-editor handoffs remain separate.

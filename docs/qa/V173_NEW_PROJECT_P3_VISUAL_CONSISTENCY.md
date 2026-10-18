# Backlot V173 — New Project Phase 3: Visual consistency and accessibility

Base: V172 draft/wizard continuity preview. Branch: `feature/backlot-v173-new-project-p3-visual-consistency`.

**State:** Draft preview. Do not merge to stable/main without real-device and creator acceptance. No database schema changes, reminder rescheduling changes, or other data migrations.

## Implemented visual direction
- Retain the original cinematic dark theme, red action buttons, gold highlights, progressive full seven-step structure, and independent Quick Create.
- Define shared project-specific text/touch metrics in `V173ProjectUiMetrics.kt`: no under-12sp secondary copy in the New Project wizard, 13sp for body and review values, 18sp section headings, minimum 48dp tappable choices, and flexible 52dp-height action controls.
- Increase type-selection, mode-selection, style-selection, platform/format and reminder chips from tiny labels to readable text with flexible minimum touch heights.
- Convert adjacent wizard actions to wrapping layouts so controls do not crowd each other on smaller displays.
- Give project type/style summaries more space; preserve multi-line review values rather than forcing all values into a single line.
- Use flexible `heightIn(min = ...)` controls instead of fixed heights that clip large-font content.
- Align existing-project editing and full/compact reminder-plan card visuals with the wizard while leaving all 5 reminder plans intact.
- Keep V171 overdue editing/reminder protection and V172 draft-discard/Settings continuation unchanged.

## Automated CI gate
1. Inherited V161–V172 source guards and V173 readable-layout guard.
2. JVM unit regression suite including V173 metrics contract and V171/172 protections.
3. Android lint, Android instrumentation-test **compilation**, debug APK assembly and artifact upload.
4. A compiled Compose visual UI test checks editable review controls. Compilation is **not** proof of execution on device.

## Physical phone acceptance checklist
- Upgrade only on a backed-up test device with compatible signing. Do **not** uninstall a data-bearing stable installation.
- On a compact phone, create a full seven-step project; verify no horizontal clipping or tiny unreadable chips.
- Repeat using Android system font sizes around 100%, 130% and 200% and with Display Size enlarged. Confirm long content type names, summaries and primary actions remain accessible (wrap/scroll rather than overflow).
- Browse all content types, switch creator modes, select production styles and revisit an earlier step.
- Review a long title and notes, edit a review item, confirm selected step scrolls into view.
- Repeat Quick Create, the five reminder plan choices and advanced delivery choices.
- Edit an old project: check equally readable controls and no hidden changes to its stored deadline, snooze, progress, voice or alarm.
- Confirm V172 Keep Editing / Discard, Settings detour, activity recreation and draft restoration still function.
- Confirm successful local data backup/restore and no cross-feature navigation breakage.
- Require creator approval before marking V173 stable or merging.

## Out of scope
Quick Create simplification (P4), changing reminder semantics/project details (P5), full release acceptance (P6), and restoring unsaved drafts after force-stop/reboot.

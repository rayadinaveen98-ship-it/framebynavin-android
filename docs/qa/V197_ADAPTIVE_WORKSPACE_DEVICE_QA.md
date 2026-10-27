# Backlot V197 — Adaptive Workspace Foundation (device compatibility)

Branch: `feature/backlot-v197-adaptive-workspace-foundation` based on V196.6 Phase 4; **V195.5 stable untouched**.
Preview: versionCode `210`, versionName `2.6.0-v197-adaptive-workspace-preview`.

## Why this change
A creator supplied a narrow-viewport screenshot with clipped bottom Save actions and hard-to-discover horizontally clipped Plan tabs. V196.6 used separate full-screen `Dialog` windows for Hub, Plan, Script, Publish and Guide, each applying status/navigation padding independently while the host Activity used edge-to-edge. Insets from a separate window combined with fixed footer rows can obscure controls. The screenshot supports clipping, but the exact OEM/window mode causing overlap still requires on-device measurements.

## Implementation
- New `WorkspaceAdaptiveFrame` creates the **same Activity-owned viewport** for the five Workspace destinations; full-screen dialog wrappers are removed, but confirmation `AlertDialog` remains modal.
- The frame consumes `WindowInsets.safeDrawing` and `imePadding` at its outer Activity content surface, then uses `Scaffold` with zero additional inset calculations to avoid double padding.
- Original Plan / Script / Publish `Column` and weighted scroll body remain unchanged; the footer is now inside a safe visible region rather than its own dialog window.
- The OS Back gesture still triggers unsaved-editor confirmation after switching to Activity-owned screens. Guide Back returns to Hub.
- Plan and Script use the shared `WorkspaceAdaptiveSaveActions`: standard screens retain parallel SAVE and SAVE & NEXT; widths below 380dp or font scale 1.3+ stack two full-width buttons with primary first. Save callbacks and persistence are unchanged.
- Plan navigation uses Material3 scrollable tab row that moves the selected tab into view, rather than a manually scrollable row. Optional overview metrics become 2×2 and templates single-column on compact available widths or large font scale. Existing titles and project data stay untouched.
- All other app surfaces remain as-is pending follow-on V197 device audits: Insights, home, onboarding, reminders and settings must not be called certified yet.

## Regression gates
- Historic V161–V196.6 safety guards and unit tests, lint, preview APK build and emulator acceptance suites.
- `V197AdaptiveWorkspaceUiTest` checks Plan/Script/Publish footer action bounding rectangles inside root, large-font stacked footer, and access to the far-end Notes tab.
- Run these five UI tests on Android emulator at default and a 900×1600 pixel override + font scale 1.5. Continue running historic reminder/Insights/Workspace suites after resetting screen configuration.
- Manual device check still required: exact user screenshot resolution, display scaling, gesture and three-button navigation, Android 11–16, IME visible, rotated/split-screen and 200% font scale. Measure the root and actual control bounds, not only screenshot snapshots.

## Release acceptance
1. Plan shows full SAVE and SAVE & SCRIPT buttons above the gesture bar, and no bottom click area falls outside screen at any target device.
2. Script shows full SAVE and SAVE & PUBLISH buttons; Publish shows its SAVE PUBLISH DETAILS button. The OS Back gesture still shows dirty-draft confirmation.
3. Tab navigation can reach last Notes tab and automatically reveals it on selection.
4. Template choices and statistics remain readable at 320dp widths and fonts up to 200%.
5. A user can focus each Plan/Script/Publish text editor without keyboard covering the actions. For very short landscape viewports, the content scroll area may shrink but saving and Back remain available.
6. Full V196.6 stability and actual on-device project data remain intact after in-place upgrade. Do not uninstall the previous app to sideload.
7. Do not mark V197 stable before the full screen-by-screen adaptive audit (Home, Insights, New Project, Reminders, Settings) and physical-device acceptance.

The V197 foundation intentionally separates layout ownership from business logic. The existing frozen stable branch remains a rollback option.

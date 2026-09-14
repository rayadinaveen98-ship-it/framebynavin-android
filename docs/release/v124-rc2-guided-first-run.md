# FrameByNavin v124 — RC2 Guided First Run

**Version:** `2.0.0-rc2-guided-first-run`  
**VersionCode:** `124`  
**Branch:** `feature/v2.0-rc2-guided-first-run`

## Purpose

Stage F of the RC2 real-user-feedback roadmap: teach the final product through the real product, not through a detached slideshow.

## Guided journey

The contextual first-run journey is:

1. **Today** — explains the creator command center and current-priority model.
2. **Ideas** — explains Idea Vault; creator may capture a real idea or continue without creating one.
3. **Project** — opens an existing project when available, otherwise launches the real sequential New Project wizard.
4. **Workspace** — teaches the real project workspace after the project is opened/created.
5. **Insights** — introduces Insights as the app's evidence/decision layer.
6. **Control** — introduces fast creator actions and opens the real Control sheet when the tour completes.

The tour is skippable. A completed/skipped tour does not reopen automatically. It can be replayed from Settings.

## Migration and state safety

- Existing creators who completed creator setup before v124 are treated as tour-complete on upgrade; they are not forced through the new tour.
- Brand-new creators remain eligible and the tour begins only after account onboarding and creator setup/profile are complete.
- Old backup snapshots that predate the guided-tour field are restored without forcing established creators through the new tour.
- Tour completion state is stored inside `CreatorOsSettings`, exported/imported with creator backup state and therefore participates in the same local/cloud restore path.
- Widget/deep-link launches bypass the tour for that external launch so requested actions remain immediate.
- No demo projects or fake creator data are created by the tour.

## Interaction safety

- Real dialogs/surfaces get first priority on system Back. Back inside Quick Capture or New Project dismisses that surface rather than silently skipping the whole guided journey.
- After Ideas, the tour moves to the real Create context before teaching Projects.
- Creating a new project continues to use the existing sequential wizard and opens the created project's Workspace directly.
- Existing projects can be used during replay; a replay does not require creating duplicate content.
- The final Control step marks the tour complete before opening the real Control sheet.

## Automated coverage

`CreatorGuidedTourPolicyTest` covers:

- existing-user upgrade behavior,
- genuinely new-user eligibility,
- setup completion gate,
- external-launch bypass,
- completed-tour suppression,
- exact Today → Ideas → Project → Workspace → Insights → Control ordering.

## Validation

Final validation workflow: **RC2 Guided First Run Final Validate**  
Run ID: `34881351355`

Passed gates:

- interaction patch applied cleanly / `git diff --check`,
- `:app:testDebugUnitTest`,
- `:app:compileDebugKotlin`,
- `:app:assembleDebug`,
- v124 version and guided-tour integration checks,
- debug APK artifact upload,
- validated interaction source committed back to the branch.

Artifact: **FrameByNavin-v124-guided-first-run-final**  
Artifact ID: `10363540417`  
Digest: `sha256:5975ab61e8941542356901773743b63f0776cd9bf35dd87b54e28888c1aaf54a`

## Acceptance boundary

This completes Stage F implementation and automated build validation. It does **not** claim physical-device acceptance. Stage G must still verify the entire RC2 product holistically, including new/returning creator flows, migration/restore, offline/cloud failure behavior, Google/YouTube account behavior, reminders/background execution, navigation/back stack, and production-release readiness.

# Backlot UX12 D3 — Companion stage and entry transition

**Date:** 2026-11-22  
**Status:** SOURCE STAGED; NOT TESTED; NOT MERGED; NOT RELEASED.  
**Founder-approved direction:** Film Editorial layout + Living Spatial motion + Personal Storybook warmth. Real Kotlin/Jetpack Compose, reuse existing approved Spark/Mira artwork. No generated promotional screen art.

## Continuity and branch fence

- Stable `main` at audit: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` (unmodified).
- RX01/UX11 integration PR #460 remains an unmerged draft.
- UX12 D1 draft PR #461, unchanged by D3.
- UX12 D2 staged source baseline: `2a39b227e4c4fd136f0cefe10b59ceb0078cb514`.
- UX12 D3 branch: `feature/backlot-ux12-d3-companion-arrival-20261122`, stacked on D2, NOT main.
- No account backend, data migration, creator/project/reminder engine, Google sign-in, UX09 cloud or launcher asset changes.

## D3 deliverables — SOURCE STAGED

### Companion selection

Files: `V144GuideSystem.kt`, `UX12D3CompanionArrival.kt`.

- Keep `V144GuideChoiceGate` and its saved selection behavior unchanged: one deliberate Select button for Spark or Mira.
- Replace the flat black stage with a native, restrained warm/violet gradient atmosphere: two diffuse Compose Canvas lights, 6.8-second gentle drift, no decorative heavy rendering.
- Keep clear, short copy and aligned 48dp Select buttons, improve visual breathing room.
- Reuse `guide_funny_present.webp` for Spark and `guide_mira_welcome.webp` for Mira during selection. Existing `V144RasterGuideCharacter` supplies the original subtle breathing motion.
- Never alter, crop, redraw, reshape or recolor approved character images.
- Stop ambient motion in Android reduced-animation or Battery Saver conditions. Ambient light is not microphone activity.

### 2.25-second workspace arrival

Files: `UX12D3CompanionArrival.kt`, `FrameByNavinV101BApp.kt`.

- New `UX12D3WorkspaceArrival` is an in-place Compose overlay, not a navigation Activity, auth prompt, alarm alert or loading spinner.
- Trigger only after the real `completeCreatorSetup(profile)` succeeds OR the Personal first-run guide successfully persists completion.
- On Creator's existing page-five **Enter Creator OS** action, create a `CREATOR` arrival and defer guided-tour overlay until the transition finishes. On completing/skipping the Personal guide, create a `PERSONAL` arrival above the existing Personal hub.
- Duration is ~2250 ms (360ms fade in + 560ms hold + 1080ms character-present stage + 250ms fade out); selected Spark/Mira changes from an existing welcome pose into an existing presenting pose.
- Back ends the decorative overlay without discarding completed setup.
- Pointer input is blocked while the entry artwork is visible, so the underlying screen cannot be accidentally tapped during the animation.
- Android no-animation/low-battery route stays static and finishes after ~180ms.
- Ignore/clear the arrival for external launches (notifications, reminder deep links and widgets); original deep-link routing remains authoritative.
- This is not shown on every cold launch or for already-completed returning users. No new persistence flag is introduced for this purely transient presentation.
- The existing guided Creator tour remains eligible and starts once after the overlay ends. No setup or completed profile is fabricated.

### Source-only policy tests (NOT EXECUTED)

`app/src/test/java/com/framebynavin/app/ui/UX12D3ArrivalPolicyTest.kt` stages tests for persisted-success admission, external entry bypass and motion/reduced-motion bounds. These tests MUST NOT run as a per-stage operation.

## Final QA acceptance — perform only after founder's explicit final-stage authorization

1. Fresh local setup: launch → account choice → Spark/Mira select → permissions → Personal guide → brief arrival → Personal hub. No additional `Make Backlot yours` screen.
2. Fresh Google setup: same without a redundant mode question; no changed sign-up/back behavior.
3. Creator setup: all five existing pages preserve state and validation; tap Enter Creator OS, then one brief arrival, then the original guided onboarding (not before).
4. Personal guide Skip: still records completion; entry shows only once and is not repeated on process restart.
5. Existing authenticated profile: no entrance overlay; login and saved data unchanged.
6. Widget/reminder/deep link: entry bypasses all decorative animations as previously intended; no delayed alarm response.
7. Back during arrival: closes the visual overlay and reveals correct committed destination, without re-running setup.
8. Rapid taps and lifecycle recreation: no duplicate selection, double-completion, hidden actionable controls, or overlay trapping.
9. Redmi Pad 2 / Android 16 HyperOS 3: observe actual character fidelity, gentle gradients, no GPU stutter. Also review narrow Android phone and tablet landscape.
10. Accessibility: readable large text, TalkBack, high contrast, no flashing, reduced motion; prevent confusion between ambient animation and recording.
11. Regression: full Personal/Creator/21-day reminder response and P0 delivery reliability must be certified independently; D3 makes NO changes to alarm/snooze/retry subsystems.

## Founder testing policy

**Do not run tests, lint, builds, APK assembly or release validation for D3 or another UX12 stage yet.** Stage source and report status only. Run the cumulative final integrated QA after the explicit founder command: **“Done bro, start next steps and give APK.”** Existing earlier isolated D1 automated run was prior to this instruction; no new D3 workflows or automatic checks created.

# Backlot UX modernization — 01 Welcome Animation Collection

Status: **IMPLEMENTED ON ISOLATED FEATURE BRANCH; NOT TESTED OR CERTIFIED**
Baseline: stable/backlot-v337-premiere-owner-testing-20261114 at 678502f4626150ea1945b81eed1593a362377454
Branch: feature/backlot-ux01-welcome-collection-20261117
Release: none; do not merge or publish without later integration review and owner approval.

## Approved functionality

- Preserve the existing V199 3-second Particle Signature as the default; its particle renderer and timing stay unchanged.
- Add three alternate three-second animated reveals: Living Frame, Prismatic Bloom and Spark's First Light.
- Spark's First Light reuses approved, existing guide_funny_welcome.webp (Spark/FUNNY), without replacing the companion's artwork.
- Keep the standard launch-sound preference. Each normal launch uses the existing three-second sonic ident if enabled; previews are silent.
- Present all four options in Appearance & Sound. Preview is not selection. A separate Choose action persists the selected style under the existing visual experience preference file.
- Unrecognized saved IDs fall back safely to Particle Signature.
- Preserve widget/deep-link launch bypass, first-run flow and unrelated Settings.
- Respect system animator-disabled setting with static, legible artwork while maintaining launch timing.

## Scope

Code changes:
- ui/theme/FrameByNavinTheme.kt: stable IDs, default selection, preference persistence.
- ui/V401WelcomeCollection.kt: three style renderers, shared timer and sound lifecycle.
- ui/V401WelcomePicker.kt: list, explicit selection and dismissible, silent three-second previews.
- ui/V199ParticleWelcome.kt: optional sound suppression for preview; particle renderer unchanged.
- ui/V131LaunchGate.kt: select style for regular launch, preserve external-launch fast path.
- ui/V127AppearanceSettings.kt: embed picker in Appearance & Sound.

No new permissions, new cloud storage, third-party services, project/reminder data mutations, login changes, theme removals, signing changes, Play release or production merge.

## Deliberately deferred

In accordance with the owner's implementation-first plan, **no unit, UI, integration, emulator, physical-device, performance or full-suite tests were executed for UX01**. Full certification happens after all ten stages have been implemented; failures must be repaired and affected checks rerun before release.

Later verification criteria:
1. Existing and fresh installations default to Particle Signature; saved style survives restart and in-place upgrade.
2. Preview runs for three seconds, can be closed early, remains silent, and never changes the saved choice.
3. Each style reveals the correct brand and tagline, is distinguishable, and returns control after three seconds.
4. App Icon, launch-sound toggle, themes and companion picker retain current behavior.
5. Widget/deep-link actions bypass the cinematic sequence.
6. Animation-off/reduced-motion produces readable static imagery and never leaves launch stuck.
7. No audio track or animation survives a dismissed preview, stopped composition or completed launch.
8. Compact phones, tablets, landscape, high font scale and both themes remain usable.
9. Confirm no severe frame drops, memory pressure, hangs or crashes.
10. Full regression suite, physical acceptance and release checks remain blocked until Stage 10.

## Dependencies

- UX04 Settings overhaul may reorganize placement but must preserve selection and the existing sound preference.
- UX09 cloud-sync work must explicitly decide whether this local visual preference joins portable user preferences; it is currently device-local.
- UX10 container-density refinement may polish selector spacing without changing selection or preview semantics.
- Future app changes to the launch gate must preserve targeted-launch bypass.

## Change control

Only code and this documentation are staged on the feature branch. Stable V337 and main remain untouched. Before integration, reconcile with concurrently developed Backlot branches and the final verified repository state.

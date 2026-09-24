# Backlot V146 — Guide Stability & UX

## Locked scope

1. Fix Cute guide onboarding crash and harden guide asset decoding.
2. Preserve guide artwork aspect ratio and remove deformation.
3. Persist YouTube monetary access across 7D / 28D / 90D / This Month.
4. Final guide roster: Funny, Cute, Kitty, Cute Girl. Frame/Navi remain compatibility-only persisted values and migrate to Cute.
5. Rename bottom navigation label Today -> Home.
6. Improve guided tour: 25% larger guide, useful step-specific speech, stronger target pulse and stronger outside dim/blur.
7. Fix automation next-step reminder deep link so notification opens the exact project/automation step.

## Release gates

- Unit tests and lint green.
- Instrumentation APK compiles.
- Guide asset decode coverage includes every selectable raster pose.
- Full onboarding smoke for all four guides.
- Revenue range switching never clears monetary access by itself.
- Notification deep link works from foreground/background/cold start.
- Connected release-blocker smoke green (unrelated flaky legacy failures must be fixed or isolated with evidence, not ignored).

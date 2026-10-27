# Backlot V207-D — Four-step contextual guided tour

Build **229**, `2.7.9-v207-guided-tour-preview`. Stacked after V207-C (three-screen creator setup). Draft; not a stable promotion.

## Why
The former guided first run showed six lessons, under-10sp labels and two separate spotlight renderers calculating approximate percentages of the screen. That could highlight empty spaces or wrong controls depending on Samsung aspect ratio, font scale, navigation bar height and dynamic content.

## New experience
- Four nonmandatory lessons: Home → Ideas → Projects → Insights + Quick Actions.
- Navigation items report their measured `LayoutCoordinates.boundsInRoot()` to the root. `V137GuideSharpWindow` receives the matching `Rect`; unavailable targets yield a simple dim overlay rather than a fabricated highlight.
- The coach no longer draws a second hardcoded spotlight hole; its title, instructions, buttons, and speech bubble use readable larger fonts (11–17sp). Primary/secondary buttons have 48dp minimum height.
- **Next** only navigates to the next lesson. Capturing an idea, creating a project, opening a saved project, or Quick Actions are explicitly optional secondary demonstrations.
- **Back** returns to the previous lesson. From the first lesson it closes this session without marking the tour completed, allowing later reentry. Explicit **Exit guide** and final **Finish tour** use the existing complete marker.
- Old `CreatorGuidedTourStep` values, persisted version semantics, Settings replay, deep-link safety and old policy remain; legacy WORKSPACE/CONTROL states map to nearest safe lesson.
- No user projects, ideas, notification state, YouTube access, profile preferences or backup schema changes.

## Tests / release gates
- JVM `V207GuidedTourPolicyTest`: four route steps, safe Back, legacy step handling.
- Android emulator `V207GuidedTourUiTest`: real nav target rectangles in root coordinates, four-step content and explicit exit.
- `tools/check_v207_guided_tour.py` layers on every V200–V207-C safeguard, enforces no percentages and non-forced data creation.
- Full Android build, lint, unit tests, signed APK, checksum resource audit, inherited emulator suite on exact PR head.
- Physical Samsung test: high font scale, 3-button navigation and gesture nav, replay from Settings, new-user setup to tour, close/reopen, Back, optional capture/project cancellation and returning creator path. Verify all four highlight positions match actual visible controls.
- Keep branch draft until CI and real-device review. Stable and original reminders unchanged.

## Next
V207-E Revenue: correctness of top-50 coverage and simple estimated-earnings interface; V207-F consolidated stabilization candidate.

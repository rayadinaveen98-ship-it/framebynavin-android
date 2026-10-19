# Backlot V195.4 Phase 4A — Spark alarm / voice expression prototype

Stacked on the user-tested V195.3 Motion & Vibrance build. This is a **character presentation change**, not a new character or a new illustration.

## Character consistency
Spark's approved transparent WebP artwork remains the source of truth; his existing POINT and PRESENT raster poses are used for alarm and voice respectively. Neither asset is redrawn or recolored. This first pass adds mode-specific presentation and movement around the approved character, not newly painted faces or bespoke source sprite images.

## Implementation
- Alarm: distinct POINT pose; gold radiating alert arcs, highlight sparks, soft cinematic halo, slightly alert hovering and tilt
- Voice: distinct PRESENT pose; violet aura and four independently oscillating speaking bars with gentler vertical movement
- Fixed three-second-class animation loop, bounded by pure JVM-tested functions; no sudden character flips
- Battery saver: no extra character-motion transforms; reduced-motion/animations off: stable pose
- Sprite preserved via `CinePulseGuide` so the existing selected Spark identity cannot silently switch
- Deliberately no new visible explanation text, dialogs or onboarding

## Regression protections
- V195.3 particle field, manual reveal, scheduling, full-screen intent permissions, reminder audio durations, actions, token ownership, follow-ups and notifications untouched
- Original v195 voice/alarm guide test tags are kept
- Dedicated source guard, JVM motion tests and 2 UI tests; run the full inherited Android build, lint and emulator suites

## Next phases
4B: polish reminder layout, responsive spacing/buttons/contrast in device screenshots
4C: action transition micro-interactions respecting accessibility
4D: Samsung battery/performance, unlocked/lockscreen, text scaling acceptance
4E: APK/release-candidate gate. Dedicated **new illustrated Spark face/expression sprite sheets** require a separate approved art pass and are not claimed in this prototype.

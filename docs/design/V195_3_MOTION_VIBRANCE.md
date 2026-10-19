# Backlot V195.3 — Motion and Vibrance (stacked on V195.2)

User verified the V195.2 reminder visuals on Samsung and requested stronger density, smoother/faster movement, independently changing particle colors, richer vibrance, depth/bokeh, and more legible blinking clock/microphone.

## Implemented
- Ambient density 748 -> 1092 particles, battery-saver 375 -> 493; icon dots 300 -> 420, low power 160 -> 220.
- Six vivid warm/cool colors (alarm/voice distinct) with individually phase-offset continuous hue blending.
- Three ambient size/depth tiers with near-particle extra glow at normal power, keeping power-saver 2 circles per ambient particle.
- Motion loops every 3.8s (was 5s) with integer harmonics 1x/2x/3x, avoiding discontinuities when the period repeats; richer x/y drift and gentle twinkle.
- Stable light-gold alarm and light-violet microphone glyphs with pulse .67..1.0, luminous halo, and central radial contrast backdrop.
- Retains user-tap reveal, icon removal upon reveal, ambient background with contrast scrim behind controls, no sound/services/occurrence scheduling/permission logic changes.
- Geometry cached with Compose remember, single Canvas, no GPU blur shader; exact frame-rate still needs device acceptance.

## Verification
- Existing historic contract scripts; new V198 source and unit tests for budgets, loop continuity, hue bounds, glyph legibility, reduced motion; same Android 11 emulator UI suite, build and lint.
- Needs real-device feedback for GPU performance / subjective quality in locked and unlocked contexts, and any Spark-specific visual work in V195.4.

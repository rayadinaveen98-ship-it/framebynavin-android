# Backlot V195 — Living Reminder Scenes (locked direction)

Base: V194 Insights acceptance preview. This is a stacked, **draft** preview; stable main remains V183 until separate acceptance.

## User-approved interaction
1. ALARM: approximately 300 uniform-size dots form a legible analog clock; VOICE: the same dot system forms a microphone.
2. Dots move quickly but remain constrained to their respective target glyph; scatter-to-glyph entrance, no unbounded randomized rendering on each frame.
3. Tapping the particles reveals the controls. Automatic reveal after a short bounded delay; animation must never block reminder actions indefinitely.
4. Particles recede and fade to a subtle background while controls appear; Spark enters with an alarm/voice pose. New production-grade speaking/alert artwork is a follow-on art phase; existing Spark poses are placeholders.
5. Respect reduced-motion and battery-saving preference; no third-party animation SDK or remote asset needed.

## Performance and architecture
- Geometry targets generated once per scene and reused. Compose Canvas draws all dots in one surface.
- Same dot radius for every dot, with adaptive total count (300 normal / 160 power save).
- Animations are VISUAL ONLY; do not touch alarm/voice services, playback duration, reminder occurrence tokens, snooze, stop, reschedule, backup, notification fallback, or escalation.
- Keep original functional reminder controls and Android foreground/lifecycle behavior.
- Stage accessibility remains available: tap anywhere on dot field to reveal, and auto reveal after a short time.
- Reminder UI might close soon after short voice playback under current V178 policy. Device acceptance must include fast voice completions, not only long phrases.

## Phases
P1: native deterministic particle geometry, tests, scene spec (first implementation).
P2: Compose drawing/tap/reveal stage, reduced-motion and fallback, emulator UI coverage.
P3: actual Spark alert and speaking poses, exact approved art continuity.
P4: performance/accessibility/device validation and candidate APK, without stable merge.

## Safety gate
Do not claim physical-device QA, new poses, or a finished UI until separately implemented and demonstrated. Avoid adding more explanation text to reminder screens.

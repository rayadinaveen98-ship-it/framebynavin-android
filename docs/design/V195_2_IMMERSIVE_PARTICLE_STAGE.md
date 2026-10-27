# Backlot V195.2 — immersive reminder particle stage

User feedback from V195: the isolated 300-dot symbol on a black screen is unacceptable; its 2.1-second automatic reveal is unacceptable; symbol must not remain blurred behind the action controls. The screen should fill with independent moving particles, while a distinct, constantly-colored flashing clock/mic symbol is visually identifiable.

## Source of truth
- `V197AmbientGeometry`: independent, screen-covering ambient particle layer (748 normal / 375 battery saver), four cohesive ambient colors, deterministic jittered positions.
- `V195ParticleGeometry`: separate, stable, recognizable clock/mic targets; original shape geometry preserved.
- `V195ReminderParticleScene`: single full-screen Compose Canvas for both systems, no per-particle UI nodes; ambient particles continuously animate on both stages. Icon gently pulses in opacity using a single persistent color.
- After tapping anywhere during the intro, the symbol fades out completely, not down to a blurred icon; background particles dim and turn into soft bokeh halos. Reminder controls and Spark use the previous functional view hierarchy.
- The screen does not auto-transition. Android reduced-motion/disabled animation shows controls directly and renders the static backdrop; no mandatory tap for accessibility.
- Audio ending and response-screen closure are independent in the stacked V195.1 reliability branch.

## Performance/safety
- Stable per-particle geometry cached via `remember`, no per-particle Composables, blur layers, global blur shaders or remote animation dependencies.
- Glow = low-alpha concentric circles; no costly per-dot gradient brushes.
- Decor receives clicks only during intro; after reveal it is not interactive so no hit-testing interference with Done/Snooze.
- No changes to app data, notifications, scheduled alarms, voice playback, service routing, existing actions, or full-screen permission behavior.

## Still pending
- Real Samsung device: unreadability, low-power frame rate, medium/large text scaling, talkback, locked/unlocked system behavior; GPU performance and actual alert behavior.
- New Spark-specific alert/speaking artwork (separate art phase), and any further visual tuning from user feedback.

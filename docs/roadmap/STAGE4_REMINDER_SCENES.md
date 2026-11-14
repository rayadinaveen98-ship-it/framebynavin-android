# Backlot Stage 4 — Independent Voice / Alarm scene engine

State: **implemented in source; awaiting final combined test pass, GPU review and physical devices**.

## Implementation
- The legacy Director's Cut remains default for both Voice and Alarm; no surprise theme migration on upgrade.
- Adds six independent animated native Compose Canvas scenes: Aurora Silk, Liquid Chrome, Celestial Drift, Crimson Pulse, Glasswave, Ember Field.
- A Settings > Appearance & sound > Reminder scenes picker stores independent Voice and Alarm preferences.
- Creator check-ins and Personal alert surfaces now resolve a shared visual scene; existing controls, privacy, scheduling, playback, reminders and Snooze routes are untouched.
- The visual engine never owns an Activity, service or notification; it is an inert background composable behind click targets.
- All motion uses a bounded 20/30 fps frame clock, deterministic particle positions and static fallback when Android animations are disabled.
- Long-lived Canvas previews are rendered with motion disabled, for reduced unnecessary battery use in Settings.

## Acceptance criteria (final integrated QA only)
1. 7 scenes each in Voice and Alarm at normal/large fonts, compact/tablet device profiles and in dark/bright ambient conditions.
2. Both Creator and Personal show independently chosen Voice/Alarm backgrounds; settings persist after process death and restart.
3. Gestures, Done/Snooze/Replay/Working/Dismiss and Android full-screen eligibility behave identically to existing code.
4. Reduced-motion setting produces a stable still composition; battery saver reduces visual complexity.
5. Performance: no crashes, no clipped controls, no sustained high GPU/CPU load, no frame jitter on the owner device.
6. Lock screen retains private Personal content protection. Neither a scene nor companion animation requests permissions or changes the screen response window.

New profile art direction is **visual proposal implemented in code**, not yet verified visually on an emulator or physical phone. Never call it owner-approved until screenshots and device review.

# Backlot V195.4 Phase 4C — Tactile action feedback

Stacked on user-approved V195.4 Phase 4B minimal reminder layout, V195.4 Phase 4A Spark and V195.3 particle engine.

## Scope
- Material3 buttons keep their **existing onClick callbacks directly**. No delayed, dropped, queued or debounced actions; this is critical for active alarms/voice.
- One shared Compose modifier reads each button's MutableInteractionSource and scales it very slightly on press (primary 0.976, secondary 0.985). Existing Material ripple, tap target size, screen reader semantics and action tags remain.
- More Options uses expand/shrink plus soft fade so secondary controls enter and leave without an abrupt jump.
- Android reduced motion/disabled animators: target button scale stays at 1, transition duration zero.
- No changes to reminder service, delivery, audio timing, FSI permissions, Snooze and Stage Done callback semantics, persistence, or navigation.

## Quality gates
- New deterministic motion-policy JVM tests and two Android emulator UI interaction tests (alarm callback immediacy, voice expand/collapse); retain Phase 4B action-wiring test suite and inherited contracts.
- Stable install and subjective haptics not assumed; no vibration has been added, since vibrating actions could interfere with real alert behavior.
- Phase 4D: physical-device contrast/responsiveness/battery, TalkBack and font scaling, locked/unlocked reminder delivery.

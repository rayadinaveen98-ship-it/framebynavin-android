# FrameByNavin v127 — Visual Experience V2

Status: LOCKED IMPLEMENTATION CONTRACT

## Goal
Elevate FrameByNavin from a functionally complete creator OS into a cohesive premium visual product without reducing any existing capability.

## Non-regression rule
Visual work may change presentation, motion, guidance and access surfaces. It must not remove, hide or simplify existing project, reminder, creator-intelligence, voice, cloud, rewards or workflow functionality.

## V127 scope

### 1. Runtime theme system
- Five themes at launch: Director's Cut, Midnight, Ember, Violet Neon, Ivory Studio.
- Themes apply to existing semantic color aliases used throughout the app, not only MaterialTheme widgets.
- Theme selection persists locally and applies immediately.
- Theme picker lives in Settings / Appearance.
- Motion/orb/guide effects derive accents from the selected theme.

### 2. Premium 2D Guide
- Original minimal FrameByNavin guide character rendered from lightweight Compose/vector primitives.
- Guide has idle, point, walk/slide and celebrate states.
- Existing six-step first-run journey remains behaviorally intact.
- Coach becomes visual, target-oriented and concise rather than paragraph-heavy.
- Skip/replay behavior is preserved.

### 3. Widget suite
Keep existing Compact and Large creator widgets and add dedicated widgets for:
- Quick Idea
- Current Project
- Next Reminder
- Content Calendar shortcut/status
- Daily Brief
- Creator Progress / Insights shortcut

All dedicated widgets deep-link into the existing canonical surfaces.

### 4. Motion + feedback
- Reusable stage-completion microinteraction: short frame pulse + check + haptic-safe visual feedback.
- Reward/achievement feedback: restrained gold/accent pulse, icon emphasis and XP motion.
- Respect Android animator-duration scale / reduced motion where possible.

### 5. Voice capture orb
- Existing SpeechRecognizer/transcription behavior remains source of truth.
- RMS amplitude is surfaced from IdeaVoiceTranscriber.
- Quick Idea listening state transforms into a responsive Frame Pulse Orb.
- Theme-derived glow/ribbons, amplitude response, partial transcript beneath, tap to stop.
- Audio remains unpersisted.

### 6. Cinematic welcome refinement
- Increase stripes from 12 to a denser field (target 26).
- Slow and smooth stripe travel, richer accent palette, stronger center glow.
- Keep total cold-launch ident compact (~3 s).
- Add optional original lightweight sonic ident obeying system media volume; launch sound preference can be toggled from Settings.

## Validation gates
- Existing v126 New Project parity contract remains green.
- Unit tests green.
- Kotlin compile green.
- Debug APK builds.
- Theme IDs and palette mapping covered by tests.
- Widget providers declared and deep-link actions resolve.
- Voice listener forwards RMS without altering transcript behavior.

# Quiet Concert Studio — production UI contract

The approved Option C is the visual source of truth for playing modes. This document records implementation requirements, not a new mockup.

- Browsing is portrait-first; piano, lesson, practice and performance are dedicated landscape experiences. Restore the originating screen on exit.
- The instrument is the primary visual and interaction surface. Instructions and feedback must not consume the keyboard's usable area unnecessarily.
- The keyboard viewport is stable during playing. Only deliberate octave navigation or panning may move it. The initial range may be selected for a lesson before playback begins.
- All modes share note geometry, source-aware pressed-note state, audio ownership, controls and design tokens. The final player consolidation belongs to P2.
- Sound events must not wait for UI animation. All active notes must be released on cancellation, route exit and audio interruption.
- No hardcoded achievement or practice statistics. Show actual persisted data or an honest empty state.
- Guided exploration is not a graded performance. Feedback is short, contextual and non-punitive. Unsupported scoring dimensions must not be presented as measured facts.
- Use safe insets and available-window constraints. Never depend on one fixed phone height. Instrument content is fixed; auxiliary settings/content may scroll.
- Interactive controls have accessible hit targets and meaningful semantics. Test font scaling, small landscape displays, tablets, keyboard input and TalkBack.
- Keep one production theme, one navigation source, one data implementation and one music/audio contract. Delete superseded implementations after migration, not before preserving their functionality.
- No version/debug/probe copy on ordinary product screens. Version details belong in About.
- CI compilation alone is not a release gate: run actual activity/navigation smoke tests, native symbol checks, and inspect screenshots at representative dimensions. Record what was tested versus what still requires physical-device validation.

P0 is architecture and regression stabilization. P1 delivers the portrait shell, Home and Learn redesign. P2 delivers the shared Option C player and stable physical-keyboard geometry. P3 upgrades the instrument sound and meaningful event-based practice scoring. Kids Mode follows approval of the adult core.

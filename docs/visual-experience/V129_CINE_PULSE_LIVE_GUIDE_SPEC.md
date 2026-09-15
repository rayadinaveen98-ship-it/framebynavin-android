# V129 — Cine Pulse Live Guide + Experience Cleanup

Status: LOCKED for implementation
Base: v128 Premium Experience V3

## Product contract

1. Replace the amateur Frame Guide renderer with Cine Pulse, a live vector mascot with an explicit state machine: IDLE, WALK, POINT, THINK, LISTEN, SUCCESS, CELEBRATE, REST.
2. Preserve the existing setup and guided-tour flows. Cine Pulse is a renderer/experience upgrade, not a workflow reduction.
3. Guided-tour spotlight remains a real cutout over the live product and gains restrained breathing/motion.
4. Voice Quick Capture becomes two-step: mic button opens Frame Pulse in IDLE; only tapping the orb starts listening. Tapping while listening stops capture.
5. Remove Ivory Studio completely. Existing devices that stored IVORY_STUDIO safely fall back to Director's Cut.
6. Add Lumen Flow: a structurally different dark visual personality with living translucent ribbons, vibrant coral/aqua/gold light, and theme-aware floating surfaces—not a palette-only skin.
7. Keep Aurora Glass and all other working v128 themes.
8. Welcome ident keeps the five-second launch experience and increases inter-thread spacing by 25% while retaining smooth motion.
9. Add Google Calendar in Settings using Android Calendar Provider. Request READ_CALENDAR/WRITE_CALENDAR only when the user chooses to connect. Show writable Google calendars, let the user select one, and sync project deadlines without replacing FrameByNavin reminders.
10. Calendar event ownership is local and explicit: app-created event IDs are stored and updated; no arbitrary calendar events are modified.
11. Preserve v126 New Project feature parity: Off/Light/Guided/Urgent/Custom, reminder styles, custom reminder time/importance, and notes.
12. No app rename in v129. The product name will change only after the user selects a final name. Cine Pulse is the mascot name.

## QA gates

- versionCode 129
- Kotlin compile + unit tests + debug APK green
- no IVORY_STUDIO source references in the active theme/widget path
- LUMEN_FLOW selectable and widget-safe
- Cine Pulse state enum and renderer compiled
- spotlight animated without hiding the target
- mic button cannot start recognition directly
- calendar permissions + visible Settings surface + sync engine compiled
- 1.25 thread-gap scale present
- v126 New Project parity assertions retained

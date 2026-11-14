# Stage 4 — Shared Voice/Alarm visual scene engine

Status: **source implemented; integrated tests and owner APK deliberately deferred**.

## Approved scope
- Independent, persisted Voice and Alarm background selection used across Creator and Personal reminder screens.
- Preserve every existing Done, Snooze, Pause, Replay, Dismiss, Reschedule and permission callback.
- Keep the V195 initial Creator clock/microphone reveal and existing Director's Cut scene as the default on upgrades.
- Six new animated Canvas backgrounds: Aurora Silk, Liquid Chrome, Celestial Drift, Crimson Pulse, Glasswave, Ember Field.
- Settings > Appearance & sound > Reminder scenes opens the Appearance page with two scene selectors and live previews.
- Low-power and OS reduced-motion behavior respected by the shared frame clock; backgrounds do not intercept touch events.
- No new paid libraries, services, persistent data migrations or external APIs.

## Non-goals
- Audio amplitude visualization, alarm scheduling changes, TTS changes, per-reminder scene override.
- Full-screen intent policy modifications or changes to lock-screen privacy.
- APK build/test before all remaining stages are integrated (owner explicit instruction).

## Final QA checklist
- Creator voice / alarm + Personal voice / alarm choose respective scene across app restarts.
- Old installs retain Director's Cut until user picks another background.
- Voice and Alarm defaults independent. Selected previews visibly update.
- Animation disabled / battery saver / tablets / small screens do not block action controls.
- Locked Personal alarm stays private. Text legibility WCAG AA through real screenshots.
- Check performance and battery on actual Android device, especially long alarms.

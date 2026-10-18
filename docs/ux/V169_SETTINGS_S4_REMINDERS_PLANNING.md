# Backlot V169 — Settings S4 Reminders & Planning Clarity

**Base:** V168 S3 `d63e424b6cd2e03f47c6a1dd30794217c3dd5fa9`.
**Scope:** Settings experience and explanatory presentation. **No scheduler, alarm, voice engine, worker, stored data model or default changes.**

## Reminders & Alerts
- The top status distinguishes **notifications permission** from additional exact-alarm, full-screen and battery permissions. No global "Everything is ready" claim based on unrelated options.
- Standard notification permission is always shown. Advanced Android alarm/background permission rows are available via a clearly labeled optional disclosure, retaining the original callbacks.
- Snooze and alarm auto-stop choices include existing historical/custom saved values, rather than making them appear missing or unselected when not in the common presets.
- Default text explains that these choices do not rewrite saved project-specific reminder choices.
- Quiet hours explanation matches the actual `QuietHoursPolicy`: only standard and gentle Smart notifications may become silent; voice, ringing alarms and later Smart escalations can still make sound. No exact-delivery promise.
- Helpful project check-ins remain a separate optional toggle and correctly require notification permission.

## Planning & Calendar
- Keep S1 consent-first Auto Plan switch, 14-day read-only preview, and S1 Google Calendar opt-in unchanged.
- Add direct **Edit weekly plan** action to the already-existing Weekly Plan overlay. This does not add new templates, change slots or generate projects.
- Add direct **Automation** action to the existing Automation overlay for background check status and optional routines.
- Explain that Auto Plan creates drafts and never publishes videos, and that Google Calendar export is separate.
- Never automatically enable Auto Plan, change weekly templates, change content calendars or touch existing project data.

## Automated acceptance
1. Inherited checks V161–V168 plus V169 preservation guard.
2. JVM tests for status language, advanced permission counts, legacy/default value retention, quiet hours semantics and Auto Plan wording.
3. Compose instrumentation: advanced permission action, nonstandard stored values, navigate to Weekly Plan and Automation without project generation.
4. Android lint, AndroidTest compilation and separate V169 preview APK build.
5. On-device: check Android permission revocation, optional alarm permissions, 1.3x / 2x font, small portrait layout, screen-reader labels, quiet hours across midnight and actual reminder delivery using existing system.
6. Check both Settings and Automation toggle confirmation; verify all existing tasks, reminders, Voice Ideas and customized weekly schedules remain identical.

**Remaining phase:** S5 — Backup/Sync status clarity, restore tests, end-to-end device acceptance and final release decision. Settings S4 is not a stable release until all gates pass.

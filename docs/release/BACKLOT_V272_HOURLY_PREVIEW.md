# Backlot Personal V272 — hourly / weekend scheduling (DRAFT, not installed)

Independent feature branch based on Build270 Phase1. This proposal changes only the local Personal reminder recurrence and UI, never CreatorTask, cloud, widget definitions or Play signing. No production data is touched.

## Implemented

- Additional repeat types `EVERY_N_HOURS` and `WEEKENDS` without changing historical enum names.
- Every 1–24 hours using local wall-clock slots, with day-of-week filters (all days, weekdays, weekends, custom), optional every-N-days filter (1–365), and an inclusive end of active daily window.
- For example 08:00 start + 3-hour step + 20:00 end: 08:00, 11:00, 14:00, 17:00 and 20:00 each eligible day.
- Personal reminder editor allows hourly step, day selection, date-step, active cutoff time picker and a **next-five-occurrences preview**.
- Personal reminder records and v1 JSON backups add optional fields with safe defaults (`everyNHours=3`, `hourlyUntil=23:59`) so old personal data remains readable. Existing Creator fields and backups are separate and unchanged.
- Unanswered hourly notifications now retain their actionable state for roughly half the next interval before being archived as MISSED; not auto-Done or immediately retired because the next slot is soon.
- Strict reminder title/recurrence validation, occurrence lease tokens and reboot recovery stay under the existing Personal scheduling coordinator.

## Scope limits / important release gates

- A user trying an active window ending before the starting time gets a validation error; an overnight window spanning midnight isn't supported yet.
- Inexact Android alarms can be late due to Doze, alarms permissions or vendor power management. Active time slots are deterministic schedule intents, not a guarantee of exact playback every X hours.
- A frequent repeated voice reminder is subject to device audio/notification policy and existing single-occurrence lease rules; integration with Phase 1A audio must be separately regression-tested.
- Unit tests cover daily 3/6-hour windows, weekends, every-N-day + weekday intersection, legacy schedules and safe stale-notification grace. Android11 integration checks Personal exported v1 JSON roundtrip/migration, using emulator-only data.
- No app APK delivered or merged from this branch. Before distribution integrate with the separate V271 voice/music PR and re-run all existing Personal and Project tests, mobile/tablet UI accessibility, reboot, quiet hours, backup recovery and physical owner-device acceptance.
- Personal self-care category redesign, custom file music and widget preview work remain future phases.

No physical owner data migration and no production account deletion, Play Store action or paid infrastructure are authorized by this review.

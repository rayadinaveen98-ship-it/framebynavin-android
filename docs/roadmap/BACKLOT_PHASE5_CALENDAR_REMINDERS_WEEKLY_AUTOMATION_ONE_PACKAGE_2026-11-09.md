# Backlot Phase 5 — Calendar, Reminders, Weekly Plan and Automation

**One locked phase, one branch/PR.** Owner explicitly deferred full Android,
emulator and signed-device test runs until final Phase 7 integration. No Phase
5A/B/C or new unrelated features. Do not claim test-passed, APK ready or
remote backup complete.

**Source branch:** `feature/backlot-phase5-calendar-reminders-weekly-automation-20261109`
**Parent:** Phase 4 Creator Workspace draft #354, which is stacked on
integrated Home/Focus candidate #342. Phase 3F's previous Android15 UI-test
issue remains unresolved; this package is NOT an owner release.

## Original roadmap vs implemented feature code

| Locked Phase 5 requirement | Source implementation and behavior |
| --- | --- |
| Real deadlines vs previews vs posted content | `CreatorContentCalendarEngine` adds `PUBLICATION` events **only** from recorded, dated PUBLISHED deliverables or legacy project publication timestamp fallback. It does not infer publication from READY, due date or future weekly slots. Actual project deadlines and ungenerated weekly slot previews retain distinct sources and colors. |
| Publication history without polluting today's action queue | The Calendar presents the previous 7 days of real publication alongside the next 14 days of projects/weekly previews. Archived records can still appear as historical publication. `CreatorDailyBriefEngine` explicitly requests upcoming-only items to exclude past publication events. |
| Explain the planner relationship | Calendar and Automation explain: Weekly Plan templates recurring slots; enabled Auto Plan may create eligible missing projects; Calendar displays real projects and still-uncreated previews. No posting or arbitrary rescheduling. |
| Prevent duplicate generated projects | Existing deterministic `scheduleOccurrenceKey` and `CreatorAutoPlanEngine.merge` are preserved. Skipped or archived generated projects still reserve their occurrence key; editing a template does not silently recreate them. |
| Confirm recurring-slot changes | `V08WeeklyScheduleScreen` confirms enable, disable and edited/new template saves, including future-only consequences. It refuses a stale edit if the underlying slot has changed while the confirmation was open. Delete and reset already had confirmations and remain intact. |
| Actual attempt, last confirmed run, failure/retry | `CreatorAutomationStateStore` now persists a restore-generation-fenced worker attempt, confirmed run count/time, disabled state and retry attempt/time separately. Worker cancellation is rethrown instead of treated as failure; other errors are recorded as RETRY_PENDING. UI reports these independently and refreshes with explicit Check Status. |
| Preserve reminder acknowledgement and quiet rules | No changes to `ReminderScheduler`, `SmartEscalationScheduler`, acknowledgement tokens, Snooze, bounded nudges or `QuietHoursPolicy`. Automation screen explains that gentle notifications observe quiet hours but explicit Voice/Alarm and ringing Smart stages are not muted by that policy. A timer never completes the project. |
| Optional creator-supplied workload | Added editable `estimatedMinutes` to existing `WeeklyScheduleSlot`, persisted in its existing JSON export/import with 0 meaning unknown. Weekly Plan lets the creator set local available minutes/week (restore-generation-fenced), and displays a 7-day estimate, unestimated occurrence count, and an overload warning when known planned effort exceeds that creator-provided limit. No estimates are invented and no dates are moved. |

## Data preservation and security boundaries

- **No new reminder authority.** In-app reminders still require existing
  explicit Done/Snooze/check-in actions. No worker is allowed to post content,
  mark an asset published or set project completion based on planned time.
- Historical WeeklySchedule JSON lacking `estimatedMinutes` reads as
  unknown (0). No sample schedules are seeded or existing creator templates
  replaced by this feature.
- Weekly slot estimates are carried by the existing portable WeeklySchedule
  JSON. Optional per-week availability is **LOCAL ONLY**, not an asserted
  verified Creator/Personal cloud backup; restore epoch mismatches reject
  the old local availability.
- Auto Plan's **confirmed** state means the worker committed a count of
  generated projects. It is NOT proof that Android delivered any alarms or
  that a platform received a post. When scheduling/IO fails after a project
  commit, the worker retains existing project records and reports a retry.
  Full recovery/reconciliation of alarm delivery after interruptions must be
  independently verified at Phase 7.
- The prior T1 Trust branches are still parked and **not** included. Do not
  reset, uninstall or overwrite the owner's signed V322 device with 16
  reminders and 21-day walking receipt.
- The existing incomplete Personal cloud coverage blocker #327 stays open.
  No new cloud, Supabase, backup, restore or account deletion is introduced.

## Consolidated QA to execute only at release integration

Source regression cases are added under
`Phase5CalendarAutomationWorkloadPolicyTest`, covering real vs preview vs
publication, archived publication, no Daily Brief history pollution,
deterministic skipped/archived occurrence keys, optional overload math and
retry evidence. **These tests have not been run.**

At Phase 7 perform one deliberate multi-feature test pass: full Gradle unit
suite, Android11/15 emulator UI tests, signed V322 in-place upgrade,
locked-screen/reboot/permission-change reminders, overlapping Voice/Alarm/
Smart notifications, quiet hours and snooze/acknowledgement, WorkManager
failed/late/retried runs, process kills mid-project generation, two-device
restores, draft and 21-day receipt preservation, accessibility at 1.5–2×
font scaling, and owner-approved final screenshots. Also verify a user edit or
disable races correctly with a running planner and no duplicate projects.

**Current status:** one Phase 5 source package implemented but **not
compiled, executed, accepted on a real phone, merged or released**.

## Next original roadmap step (no new phase)

Phase 6 — Insights and Analytics. Deferred Phase 2 — Visual Polish.
Then finish release-critical Trust defects and Phase 7 full integrated QA,
followed by Phase 8 closed testing and controlled Play release.

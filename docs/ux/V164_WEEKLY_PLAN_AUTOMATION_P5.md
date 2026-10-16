# Backlot V164 — Phase 5 Weekly Plan & Automation UX

Base: V163 phase 4 (`7635ff3539d93f4df37df9fe440700b1a7d3287d`). Preview only, no merge without creator approval.

## Behavior contract
- A weekly slot is a future template, not a created project, published post or guaranteed Android notification.
- The read-only 14-day preview lists upcoming occurrences and missing keys; it must not create, delete or modify projects.
- Skipped, archived and creator-edited projects retain ownership. Never recreate a matching scheduleOccurrenceKey.
- Creating missing projects requires a preview and confirmation; enabling automatic planning also requires confirmation.
- Existing project titles, times, notes, reminders, workflow and voice recordings must remain untouched after editing, disabling, deleting or resetting templates.
- Editor prioritizes name, day, time, platform, format and cadence; reminders and priority stay under More options.
- Deleting a template or resetting defaults requires confirmation. An explicit reset uses legacy-compatible film slots for film profiles and simple generic slots for other creator categories. No automatic migration or replacement.
- Automation must distinguish background work requested from a confirmed recorded run. Do not claim a guarantee of exact timing, posting or delivery.
- Routine preferences are separate optional notifications; Android may delay execution and permission can prevent alerts.
- Reminders and Settings designs stay out of scope.

## Acceptance
1. Prior V161/V162/V163 protections plus V164 source checks.
2. JVM tests: immutable preview, profile-specific presets, skipped/archived key preservation, empty schedule, 14-day date range.
3. Android tests, lint, APK, API 30 emulator test for preview/cancel/confirm.
4. Physical phone: changed generated projects untouched, original audio preserved, optional settings persist, deleting a slot never deletes projects, verify permissions and actual worker execution.
5. No stable release before all gates and explicit creator approval.

## Deliberate limits
- Preview describes eligible future slots. Android WorkManager may not run immediately.
- Existing WeeklyScheduleEngine's generator horizon is retained; the preview filters to exactly 14 local calendar days. This may differ for a boundary-day slot until later engine harmonization.
- Preset titles for general creators are generic and are freely editable.

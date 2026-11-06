# Backlot V307 — Compact Creator Reminder Inbox

**Development draft. Stacked on V306 PR #304. No release APK.**

## Implementation
- Replace V181's always-visible **NEEDS YOUR RESPONSE** heading, empty-state box, full pending-list cards and separate **VIEW RECENT REMINDER ACTIVITY** toggle on the main Creator Reminders screen with `V307CompactCreatorActivity`.
- New `V307CreatorActivityPolicy` reads current Creator acknowledgement evidence with the SAME `V181ReminderInboxPolicy` rules and exact `ReminderOccurrenceStore` token checks; never infer delivery or owner acknowledgement.
- When no valid pending responses or delivery failures exist, render **nothing**. When pending, display one compact row with pending count and a **RESPOND NOW** expander.
- The expander reuses the original `V181PendingReminderCard` including authoritative token/action handler and project navigation — **not a new shortcut action path**.
- Current delivery failures remain explicitly shown with a link into the V306 Activity Center. The header toolbar V306 Activity Center affordance remains visible for history, even when no tasks are pending.
- Hide the old generic `Reminder alerts ready` surface; only real Android permission blockers generate a warning card.
- Existing history, projects, alarm behavior, reminder completion, scheduling, data stores, backups, cloud code, and signing are not changed.

## Testing
- `V307CreatorActivityPolicyTest` verifies no fabricated empty state, pending/failure count, stale token filtering, completed project exclusion, de-duplication and deterministic ordering.
- `V307CompactCreatorActivityUiTest` runs only on disposable Android 15 emulator, checks the old large text and history toggle are absent, ensures Activity Center still opens, and captures a real Compose screenshot.
- Dedicated `backlot-v307-creator-cleanup.yml` enforces an exact allowlist; runs full Kotlin unit suite, assembles debug+instrumentation APKs and runs the Android UI test.

## Pending before owner installation
- V306 tests and V307 exact-head green checks must be reviewed.
- Android screenshot must be manually visually reviewed for spacing, safe navigation and legible text.
- A non-debuggable same-signing in-place upgrade must preserve V294/V302 real synthetic data, followed by physical-device reminder and notification acceptance.
- V308 Spark multi-item Activity Center integration is separate; V307 doesn't alter Spark.

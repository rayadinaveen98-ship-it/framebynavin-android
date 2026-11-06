# Backlot V308 — Spark multi-activity preview

**Draft only. Stacked on V307 (#305) and V306 (#304); no owner APK release.**

## Implemented
- Spark's home-sheet presentation now queries the same read-only, evidence-backed `V306ActivityCenterPolicy` fed by `ReminderAcknowledgementStore`, `ReminderOccurrenceStore`, and Personal DataStore's explicit Done receipts.
- Current action-needed activities appear ahead of historical entries, at most **three** independent records. If no current actions exist, show at most two real historic acknowledgement/completion records. No synthetic alerts, no new reminder delivery.
- A **See All Activity** action opens the V306 Activity Center, and Creator/Personal row actions route into their original spaces.
- User-defined Personal titles/details are deliberately **not shown in Spark's broad preview**; explicit navigation to Personal or Activity Center can show the user's own records.
- Spark's prior single evidence-based suggestion remains available as a fallback when no Activity Center items exist. Existing Ask Spark offline conversation and voice features remain unchanged.
- Owner-disabled Spark still displays nothing; Activity Center remains accessible through Reminders outside Spark.
- No changes to scheduling, full-screen alerts, response receipts, alarm media, Google/cloud, data model/schema, backup/restore or signing.

## Tests
- `V308SparkActivityPolicyTest` (6 JVM cases): empty evidence, three-item cap, history-only fallback, duplicate suppression, privacy, Creator title handling.
- `V308SparkActivityDigestUiTest`: Android 15 emulator two-domain Compose screenshot, no Personal title disclosure and correct three navigation callbacks.
- `backlot-v308-spark-multi-activity.yml`: exact stacked-branch source allowlist, debug compile/full JVM suite and synthetic Android UI gate.

## Release hold
V306 and V307 tests must first be reviewed. V308 exact-head build, Android screenshot QA, upgrade preservation and owner-device validation remain mandatory; **do not distribute or claim a stable release** before all pass.

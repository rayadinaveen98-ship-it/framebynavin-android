# Backlot V306 — Unified Activity Center, initial read-only implementation

**Status: development branch. Not an approved owner APK or Play Store release.**
Base: `feature/backlot-v305-rollback-escrow` at `eac4b00d8a38b25c27a7ef819a1af2d1f7ba3614`.
V294 owner-preview stays untouched.

## Actual V306 slice

- One shared read-only `V306ActivityCenterScreen` available from **Creator Reminders** and **Personal Reminders**.
- `V306ActivityCenterPolicy` projects *actual* Creator acknowledgement records from `ReminderAcknowledgementStore`, using the latest immutable occurrence token's validity from `ReminderOccurrenceStore` and the existing `V181ReminderInboxPolicy` action rules.
- Personal actions require the existing `V288PersonalTodayPolicy.canRespond`: a presented current Personal occurrence; queued reminders never become fictitious unread notifications.
- Personal history only uses durable `PersonalCompletionJournal` Done receipts from `PersonalReminderStore.completionHistoryFlow`. User response and delivery are not inferred from schedule or notification posting.
- **Needs action / All / History** filters; current delivery failures prioritized; bounded historical rows, deterministic stable row IDs and duplicate suppression.
- Tapping a row opens the existing owner-controlled original reminder or editor; this screen performs **no Done, Snooze, scheduling, mutating persistence, cloud sync or alarm action**.
- A refresh button rereads existing Creator evidence. Personal flow collection updates from canonical DataStore.
- Loading and corruption have explicit messages; a failed source cannot silently become verified zero.
- V306 source tests cover no invented records, stale tokens, Creator completion, delivery failure, Personal presented lease, verified Done only, no orphan activity, stable identity and bounded history.

## Deliberate limitations

This is V306 **first implementation slice**, not the finished Spark Activity Center system:

1. Does not yet provide a persistent unread/read state, dismissal policy, deduplicated permission-problem events or cross-device activity sync.
2. The Creator Reminder page's big Needs Your Response / Recent Activity blocks remain pending V307 cleanup.
3. Spark's single suggestion selector is unchanged; V308 will consume the same evidence model after privacy checks.
4. Personal confirmed history contains Done receipts only; existing Personal response store is still authoritative for other actions. Do not pretend a sent Android notification was read.
5. V306 does not import/export new activity records or change V300–V305 encrypted recovery.
6. The 80-row cap applies only to recent History, never to current Needs Action.

## Mandatory gates before any installation

- Run exact-commit `backlot-v306-activity-center.yml` allowlist, Android debug compilation and full unit regression suite.
- Add Android 15 UI navigation/screenshot acceptance and inspect resulting captures, including small viewport and large font.
- Signed in-place upgrade from V294 owner preview (and known installed V302, if applicable), preserving Creator/Personal/Spaces/photos.
- Physical-device reminder test, including locked-screen full-screen alarms, voice 3x, music ducking, Done, Snooze, token expiry and clock changes.
- Never overwrite installed owner records or touch cloud/live user data as part of synthetic CI.
- Play Store production signing, policy and all-domain backup remain separate blockers.

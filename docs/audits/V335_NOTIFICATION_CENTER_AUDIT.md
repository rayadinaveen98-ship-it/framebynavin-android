# Backlot V335 — Notification Center and Companion Audit

Status: source changes committed on `feature/backlot-v335-independent-activity-inbox-20261111`; Android signed APK/device acceptance pending.

## Scope and evidence
Audited V333's Home companion entry, V306 Activity Center, V308 Spark digest, V157 Creator response hub, V2232 Personal hub, Creator `ReminderNotifications` and Personal `PersonalReminderNotifications`, plus the ledger and Personal delivery health policies. V335 changes are presentation/navigation/read-only projection only; **scheduling, delivery receivers, alarm audio, acknowledgement tokens, stored data and the restored V333 reminder screens are not modified.**

## Confirmed findings
1. **Entry hidden when Spark disabled.** V333's entire Home top-right control returned early when companion suggestions were off. V335 separates an always-accessible Material outlined notification bell from the optional Spark outlined icon/sheet.
2. **Activity badge blended multiple purposes.** V333 highlighted the shared icon for a suggestion *or recent history*, which is not an unread notification. V335 red indicator is reserved for evidence-backed `NEEDS_ACTION`, gold indicator for Spark suggestions, and no dot for history alone.
3. **Creator activity opened the project editor.** V335 opens and focuses the existing Creator reminder hub so token/fingerprint-validated actions remain reachable. It neither sends acknowledgement on navigation nor changes project records.
4. **Personal activity opened only the generic Personal hub.** V335 supports a focused Personal row with its existing response buttons (active only after a presented lease), expandable details, and a clear SHOW ALL action.
5. **Personal overdue delivery evidence was missing from the Activity Center.** V335 adds `LATE_UNCONFIRMED` and `EARLIER_MISSED` to the read-only projection when time evidence is present. These are *attention* items, **not delivery failures**. `DUE_UNCONFIRMED` within the 5-minute grace period does not generate an alarmist new entry.
6. **Delivery-issue context was easy to miss.** V335 adds a count/summary for verified current Creator delivery-failed events, linking back to the Needs action filter. History and current actions remain separate.

## Existing safeguards retained
- Creator standard and quiet notification channels; Android permissions and channel checks.
- Personal separate high-importance notification channel and private/public lock-screen presentation.
- Occurrence-bound action intents; old notification tokens cannot acknowledge newer occurrences.
- Explicit response records; posted notification is not proof of reading/hearing; audio started is not proof of hearing.
- Quiet follow-up policy and no unsolicited loud escalation.
- Spark dismissals are a presentation preference, not a Done/Snooze/acknowledge.

## Remaining gaps and limitations
- No true app-wide read/unread notification model. Do not label badges as unread until user-view events are explicitly and safely defined.
- The Activity Center is a **local ledger projection**, not a mirror of the Android notification shade; it intentionally cannot list every OS notification or verify that a sound was audible.
- Creator historical records are capped to 80 recent entries; Personal history depends on available saved completion receipts and currently retained reminder records.
- Current delivery health covers relevant evidence but does not provide a full diagnostic of OEM background restrictions, battery optimization, DND/volume or all channel settings in one place.
- User-triggered test alert and permission-repair flow are not included in V335; design and test these separately before release.

## Acceptance requirements
- Spark on/off: notification bell always available, Spark independent, 48dp touch controls, readable at narrow widths and large font.
- Idle/history-only: no unread/red action dot. Active reminder: accurate red action indicator. Spark suggestion only: optional gold dot on Spark.
- Creator + Personal Activity Center items open the correct existing controls and do not respond automatically.
- Past-due, non-posted Personal lease is labelled **delivery not confirmed**; no Done/Snooze before a real presented token.
- Android 13+ permission revoked, disabled channels, reboot, locked screen, exact alarm access, process restart and large-font behavior need emulator/device QA. Unit and Android test jobs are configured, not automatically presumed passed.
- No change to V333 reminder visuals, reminder scheduling, stores, notifications, action receivers or services.

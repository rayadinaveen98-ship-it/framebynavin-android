# FrameByNavin v124 — production physical-device acceptance

Version: `2.0.0-rc2-guided-first-run`  
Version code: `124`  
Package: `com.framebynavin.app`

This checklist is the final runtime acceptance gate for the v2.0 RC2 line. It must be executed against the **exact production-signed APK generated from the release candidate commit**. A debug/prototype-signed APK may be used for exploratory testing, but it cannot close this gate because Google OAuth, Play Integrity/App Check, package signing identity, and some OS behavior are certificate/build dependent.

## Evidence to record before installation

Record these values in the release verification record before testing:

- source commit SHA;
- version name/code;
- production certificate SHA-1;
- production certificate SHA-256;
- APK SHA-256;
- AAB SHA-256;
- Android OAuth registration confirmed for the production SHA-1;
- Firebase/Play Integrity App Check registration confirmed for the production SHA-256;
- device model;
- Android version/API level;
- test date/time;
- whether installation is fresh, upgrade, or reinstall/restore.

The signing key/passwords themselves must never be copied into this document, Git, screenshots, or issue comments.

## A. Clean install and first-run journey

1. Install the exact production-signed APK on a device where FrameByNavin is not currently installed.
2. Launch normally and confirm no startup crash, blank frame, or indefinite loading state.
3. Complete account onboarding and creator setup.
4. Confirm Guided First Run begins only after the prerequisite setup is complete.
5. Walk the guided path through Today -> Idea capture -> Project -> Workspace -> Insights -> Control.
6. Confirm Skip works and does not erase creator data.
7. Relaunch and confirm the tour does not repeatedly interrupt a completed/skipped user.
8. Use Settings -> replay tour and confirm replay is available without resetting creator data.

Pass condition: first run is understandable, dismissible, persistent across relaunch, and does not block normal app use.

## B. Google identity and YouTube authorization

1. Sign in with the intended Google account from the production-signed build.
2. Confirm account identity resolves correctly and the app does not fall back to a debug-only credential path.
3. Connect YouTube using the explicit user-initiated authorization flow.
4. Confirm Channel/Insights data can refresh after consent.
5. Kill and relaunch the app; confirm the connected state recovers correctly.
6. Revoke Google/YouTube access externally, return to the app, and confirm failure is surfaced safely rather than silently presenting stale authorization as valid.
7. Reconnect explicitly and confirm recovery.
8. Sign out and confirm no previous account's cloud-owned data becomes visible under a different account.

Pass condition: production OAuth succeeds with the registered release certificate, consent remains explicit, revoked credentials fail safely, and account boundaries hold.

## C. Automatic Supabase creator sync

Use a small identifiable fixture: one idea, one project, creator-profile change, and one workflow-stage change.

1. With network available, make the fixture edits locally and confirm automatic sync reaches a settled/healthy state.
2. Go offline, make another local edit, and verify the app remains usable.
3. Reconnect and confirm the offline edit synchronizes without deleting newer local data.
4. Force-stop/relaunch during or shortly after a sync and confirm data remains consistent.
5. On a fresh install/reinstall using the same account, confirm cloud restore/reconciliation recovers creator-owned data.
6. Exercise an account switch A -> B and verify A's data is not exposed to B.
7. Return B -> A and verify A's state is recoverable.
8. Delete an item through the supported product flow and verify deletion does not resurrect from an older cloud snapshot.
9. If a conflict/reconciliation surface appears, confirm it does not allow an older cloud revision to silently overwrite newer local work.
10. Confirm Google Drive remains a manual import/export/recovery path and does not behave as a second automatic sync authority.

Pass condition: local-first use survives network loss, cloud recovery works, account ownership is isolated, and stale snapshots/deletions do not corrupt creator state.

## D. Reminders, permissions and lock-screen behavior

Test at least one SIMPLE reminder and, where supported by the current product, one SMART/escalating reminder.

1. Notifications permission denied -> app explains/setup path remains recoverable.
2. Notifications allowed -> scheduled reminder is delivered.
3. Exact-alarm/precise-timing permission path opens the correct system surface where required.
4. Full-screen-intent permission path behaves correctly on Android versions that gate it.
5. Reminder delivery while app is foregrounded.
6. Reminder delivery while app is backgrounded.
7. Reminder delivery with screen locked.
8. Snooze/reschedule action affects the intended task only.
9. Mark done/skip/cancel removes future delivery for that occurrence/task as designed.
10. Reboot the phone with an active future reminder, then confirm reminder recovery/rescheduling.
11. Force-stop/reopen and verify reminder state does not duplicate.
12. Change relevant permission state after scheduling and confirm the app recovers or explains the limitation instead of losing task data.

Pass condition: reminders are attributable to the correct project, survive normal lifecycle/reboot scenarios, and never deliver stale/orphan actions after cancellation/restore.

## E. Project, workflow and creator-loop smoke

1. Capture a new idea.
2. Convert or create a new project using the seven-step New Project wizard.
3. Confirm project creation opens the real Workspace immediately.
4. Advance and move back through workflow stages.
5. Exercise Focus from Today/Workspace.
6. Edit project metadata and confirm it persists after relaunch.
7. Complete/publish a project and confirm the relevant post-publish/creator-loop state updates.
8. Archive/unarchive where exposed.
9. Verify Today, Ideas, Studio/Create, Calendar and Insights navigation/back behavior.
10. Confirm no duplicate project/task is created by repeated taps or activity recreation.

Pass condition: the core creator journey remains coherent and persisted end to end.

## F. Insights and data-honesty smoke

1. Open Insights with cached data available and confirm it renders before/while a background freshness check occurs.
2. Exercise 7/28/90-day ranges.
3. Open Views, Watch, Subscribers and Avg View drill-downs.
4. Tap Daily Views chart points and confirm exact date/value interaction.
5. Open 24H Pulse evidence and confirm it is presented as sample-to-sample counter evidence, not an invented hourly analytics curve.
6. Open top-video/channel signals and confirm measured values remain distinct from descriptive heuristics.
7. Confirm average-view-duration UI does not fabricate daily AVD data when only period-level AVD exists.
8. With network unavailable, confirm existing cached Insights remains understandable and manual refresh failure does not destroy cached data.

Pass condition: Insights remains usable, honest about evidence, and resilient to refresh/network failure.

## G. Firebase AI / App Check production verification

Only execute after production SHA-256 is registered and the release build uses the intended Play Integrity App Check provider.

1. Confirm App Check token acquisition succeeds on the physical device for the production-signed build.
2. Exercise the user-facing AI path that depends on Firebase AI/App Check.
3. Confirm successful requests are not relying on a debug App Check token/provider.
4. Test a controlled failure/no-network path and confirm creator-owned local data is unaffected.
5. Before enabling strict enforcement, confirm expected production traffic appears as valid in the Firebase/App Check console.

Pass condition: production AI requests use the intended App Check trust path and fail safely when unavailable.

## H. Android 16 / modern-system behavior

On an Android 16/API 36 physical device when available:

1. Verify edge-to-edge layout does not hide primary actions under status/navigation bars.
2. Exercise system Back from Today child surfaces, project editor/workspace, Settings, Insights drill-downs, Quick Capture and the guided tour.
3. Confirm predictive/back behavior does not accidentally mark the guided tour complete while merely closing a real child surface.
4. Recheck notification/exact-alarm/full-screen settings on the OS-specific permission surfaces.
5. Rotate/recreate where practical and confirm critical transient UI does not corrupt persisted state.

Pass condition: no Android-16-specific navigation, layout, permission or lifecycle regression blocks creator use.

## I. Upgrade and recovery

Where a prior validated FrameByNavin build exists:

1. Record its version and local fixture data.
2. Upgrade in place to the production-signed RC using a signing-compatible path.
3. Confirm creator profile, ideas, projects, workflow state, settings and supported metadata remain intact.
4. Exercise portable backup validation/restore using the current schema.
5. Exercise a supported legacy backup fixture and confirm absent legacy sections preserve current values according to the documented compatibility contract.
6. Confirm restore invalidates orphan reminder occurrences/ledgers and does not leave stale actionable notification state.
7. Confirm interrupted restore/recovery journal behavior can recover safely if the dedicated test path is exercised.

Pass condition: upgrades/restores preserve supported creator data and fail closed on corrupt or integrity-invalid backups.

## J. Performance and accessibility sanity

1. Observe cold launch and normal navigation for obvious freezes/ANRs.
2. Scroll Today, Studio, Settings and Insights for obvious jank on the target phone.
3. Verify primary actions remain tappable at common font/display scales.
4. Check important icon-only controls have usable content descriptions where applicable.
5. Verify light/system surfaces opened by permissions/OAuth remain understandable when returning to the dark cinematic app UI.
6. Check text truncation on long project/idea titles and creator names.

Pass condition: no blocker-level performance or accessibility problem prevents normal use.

## Failure recording rule

For every blocker or release-significant failure, record:

- exact source commit and APK SHA-256;
- device/API;
- preconditions;
- numbered reproduction steps;
- expected vs actual behavior;
- screenshot/screen recording if it does not expose credentials or personal tokens;
- relevant non-secret log excerpt;
- whether creator data was at risk or actually changed.

Do not reuse a passing result from a different APK hash after source/signing changes. A production-significant code or signing/config change requires the affected acceptance slice to be rerun.

## Release decision

This checklist is **PASSED** only when all applicable blocker-level sections pass on the exact production-signed candidate. Emulator CI supplements this evidence but does not replace physical-device acceptance.

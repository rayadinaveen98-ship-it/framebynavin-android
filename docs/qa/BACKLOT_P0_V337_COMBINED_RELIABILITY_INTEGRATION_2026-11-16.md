# Backlot P0 V337 — combined reliability integration, not a release
Date: 2026-11-16

## Reference and scope
- Frozen owner-tested V337: `stable/backlot-v337-premiere-owner-testing-20261114`, exact SHA `678502f4626150ea1945b81eed1593a362377454`.
- Isolated integration branch: `integration/backlot-v337-p0-reliability-combined-20261116`.
- Parent candidate: draft PR #385, SHA `f0fd36be2aa85d2225e316f0657519697d5091c9`; includes approved malformed-recovery fail-closed and Alert Health fixes; no Merge to V337/main.
- Included unit-checked Snooze millisecond-precision correction and regression cases from draft PR #388.
- Five stale Compose Android instrumentation source repairs copied from separate QA-only PR #386.
- Real Android 15 10-minute actual-Snooze test and failure diagnostics copied from draft PR #387.

## Technical constraints
- No changes to Android alarm scheduling, 21-day DataStore schema, notification appearance, audio services, reminder text, permissions, owner cloud data, Backup/Restore Settings removal, or Advanced > Erase local data.
- Existing removed Backup/Restore navigation must NOT be reintroduced to make legacy UI tests green; the existing non-exported activities are left intact pending reachability and ownership assessment.
- Old Android instrumentation sources are **not excluded** from compilation on this branch.
- Tests operate on disposable GitHub Actions emulator only. CI clears synthetic app state on emulator, never owner phone.

## Required evidence
1. New exact-head combined CI full JVM suite, with XML report and SHA.
2. Compile every tracked Android instrumentation Kotlin test on same combined SHA, no quarantine.
3. Run corrected missing-channel Android 15 contract.
4. Run actual scheduled alarm, notification Snooze PendingIntent, ten-minute OS alarm and notification post; inspect delivery ledger events and logcat, no forged Done or duplicate OS fire.
5. Physically certify locked-screen, audible alarm, TTS, music, reboot, OEM idle and exact permissions on owner-approved physical device; check data migration with non-destructive upgrade and rollback.
6. Separately approve and validate six-stage app features, visuals, 21-day parity and accessibility before any release.

## Evidence interpretation
- Green source/unit gates are not proof of physical notification delivery or reliable sound.
- Green 10-minute emulator test is not proof of locked-screen OEM behavior.
- Any failed Android occurrence gate is a release blocker; never bypass with conditional skipped assertions, fabricated alarm broadcasts or relaxed matching windows.
- Draft PR; no merge to stable/main, no signed APK, no deployment or changes to owner data without founder decision.

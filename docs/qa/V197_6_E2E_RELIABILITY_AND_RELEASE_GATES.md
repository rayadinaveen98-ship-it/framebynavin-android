# Backlot V197.6 — Reliability and release acceptance gates

**Status:** Preview-only QA branch. No functional/data schema changes. Do not merge to `main` or move `stable/backlot-v195-5` on the strength of emulator tests alone.

## Automated end-to-end contracts

On the isolated Android 11 CI emulator, after inherited V197.5 source checks, JVM regressions, lint, APK assembly and UI suites, execute:

| Test suite | Contracts / failure cases |
| --- | --- |
| V185IdeaProjectConversionTest (7) | Conversion of real saved ideas into one project; interrupted writes resume exactly once; retries do not duplicate projects or reward XP; save/backup respects restore generation |
| CreatorPublicationRecoveryV183Test (7) | Publication survives process interruption without inventing events, moving stages or rewarding twice; checkpoint correction and data-restore boundaries |
| V176ProjectPersistenceInstrumentedTest (1) | Notes, voice choices and overdue reminder configuration survive editing and reload |
| CreatorBackupV183Test (3) | Current backup manifest, tamper rejection, safe legacy restore, interrupted restore recovery |
| V170LocalBackupScreenUiTest (2) | Backup and voice recovery UI remains discoverable |
| V159ReminderScheduleAcceptanceTest (5) | Two reminders remain independent; resume reconciliation does not duplicate schedules; cancellation, pending-only cleanup and smart escalation preserve action tokens |

Each class runs with `hardeningTestEnvironment=ci-emulator` in a disposable emulator and must emit `OK (N tests)` and no instrumentation failure marker. Test fixtures must clean up. **No tests are to be run on the user's installed device or live production data.**

## Actual device / human sign-off (separate and still required)

1. Install the existing V197.4 preview or later on a non-destructive install path. Do **not** uninstall a populated app to overcome signature mismatch.
2. At default, 150% and 200% font scales with gesture and three-button navigation: inspect Home, Projects, Idea Vault, Calendar (tab and overlay), Insights, Quick Actions, Settings, Backup, Profile and Reminders.
3. Capture an idea (text and a recorded audio note), close/reopen, convert to a project, edit workspace plan/script/assets, record a publication, return to analytics, and verify related idea/project links and all saved data.
4. Set independent Simple and Smart reminders on different projects, check notification and alarm acknowledgement/snooze, device restart recovery, and that unrelated reminders are not cancelled. Notification timing is OEM/device-dependent; automation cannot guarantee a particular alarm delivery time on every phone.
5. Create a portable backup and **validate** it, do not restore over live data without separate user approval. Exercise actual restoration on a disposable/test install only; check photos/audio references and deadlines.
6. Verify app update-in-place retains account, projects, ideas, reminders, publishing history, and backup access; compare APK signer before any release statement. Observe crash-free navigation on the user's device.
7. Keep the working stable V195.5 branch protected. Merge stacked V197.1→V197.6 only in dependency order after all QA and release gate checks.

## Exit criteria

- CI build and all targeted emulator checks green on exact head commit
- No known data loss, duplicate project, reminder cross-cancellation or clipped primary action
- Android phone smoke test and in-place upgrade/signing verified
- Stable sign-off only after the user confirms device acceptance

**Known limitation:** The CI emulator verifies code-level behavior, not real OEM background execution, account consent, cloud service availability, actual alarm arrival or non-destructive upgrade of the user's populated app.

# Backlot V170 — Settings S5 / Backup, Sync and Release Acceptance

Stacked on V169 S4 `a664fc0ee14e497427d6474d37116aa1bf5d826b`. This is a **preview**: no direct main branch merge or forced data migration. The entire Settings S1–S5 UX is now represented in the stacked draft PR chain.

## Implemented in S5
- **Cloud status:** differentiate signed out, checking, conflict, latest failed/skipped request, genuinely recorded last success, and never successfully backed up. Display previous success time separately; old timestamps never imply recent changes are backed up.
- **Concurrency:** a manual sync or conflict-resolution request is blocked while an existing cloud check runs. The initial account check cannot silently declare an unconfirmed backup successful.
- **Manual Drive list:** a failed list request never displays **No copies**. Only a successful query can establish zero saved copies. Drive copy saved but failed list refresh is reported as two distinct results, without a false failure of the completed save.
- **Local Backup:** refresh the retained recovery-copy count after returning to the screen. Validation errors never imply a restore was attempted. Export/restore copy continues to explain unencrypted files and which device preferences are excluded.
- **Recovery:** explicit warnings before destructive deletion or replacement. Recovery JSON is on-device and not encrypted; export it before deleting if it is the only older copy.
- **Data engines unchanged:** no changes to the backup schema, signed-in account models, core restore journal, voice media integrity checks or actual backup scheduling/remote APIs.

## Cross-phase Settings matrix
| Phase | Workflow | Acceptance |
|---|---|---|
| S1 V166 | Enable Auto Plan from Settings / Calendar consent | Dialog before creation/export; cancellation inert; private project notes never copied |
| S2 V167 | Settings home → 6 categories → Back | All functions preserved; Appearance/Spark/Voice nested correctly |
| S3 V168 | Profile → edit/save/cancel | No onboarding reset, no unexpected schedule/template or project mutation |
| S4 V169 | Reminders & Planning | Permission tiers honest, custom values preserved, quiet-hours caveat, Weekly Plan and Automation reachable |
| S5 V170 | Local & cloud backup/export/restore and recovery | Distinguish confirmed sync vs old success; conflicts never overwritten; media/restore safety retained |

## Automated build gate
1. V161–V169 source compatibility scripts plus V170 protection script; all JVM tests.
2. Android lint, instrumentation APK compilation, debug preview APK uploaded to build artifacts.
3. Android 11 Compose tests: V170 backup destination and disclaimer; S1 Auto Plan confirmation, S2 navigation, S3 profile edit, S4 reminders/planning. Also run existing backup/restore JVM regression suite.
4. Failed macOS emulator dependency downloads or ADB setup are **infrastructure blockers**, not evidence of passed on-device tests. Record failures separately and retry in a controlled environment.

## Physical-device release gate (manual and destructive tests isolated)
- Create a backup of your existing phone's Backlot app before any test update; never uninstall the production app to try a preview.
- Verify projects/ideas/customized weeks, reminder rules, voice recordings and per-project voices survive an in-place update and app restart.
- Export portable backup to a separate location; verify file exists, non-zero size and expected name. Confirm the file can be opened in read-only validation and preview counts match.
- On a **separate test device/profile** or expendable data, test restoring a backup, canceling after preview, restoring a broken backup (should change nothing), and retrieving a pre-restore recovery copy.
- Test audio restoration and app relaunch; validate the voice file is playable and old stored data is recoverable.
- Test no internet/Google sign-out, expired token, cloud conflict, manual backup failure, old success timestamp with new failure, and optional Drive list failure.
- Confirm Drive's empty list only after a successful list request, and separately report a saved Drive copy whose refresh failed.
- Test Calendar provider security (S1), OS permission revocation, exact vs basic alarms, quiet hours across midnight, and a completed background automation run.
- Test small screen, large font, TalkBack, back-navigation and Settings page scroll; verify labels and touch targets.
- Inspect workflow and device reports. **Do not merge or label as stable without creator sign-off.**

## Release decision
V170 is a candidate for the **full Settings update preview**, not an approved stable release. The Android emulator and real-device checks have not been certified by writing this checklist. If any gate fails, create a follow-up repair commit on this branch before promoting.

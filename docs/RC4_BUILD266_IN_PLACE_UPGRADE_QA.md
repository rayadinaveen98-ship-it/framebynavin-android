# Backlot RC4 — Build266: true in-place install upgrade and saved data

## Why RC4 exists
The previous emulator suites checked cold-start UI, TaskStore persistence, CreatorBackupManager restore journals and separate private Personal backup. They did **not** demonstrate that an existing installed Android package could be updated with `adb install -r` while retaining both creator and personal data. RC4 adds that missing end-to-end boundary.

## Deterministic disposable-emulator proof
Two independent CI jobs on Android 11 (API 30) and Android 15 (API 35) use the committed public `prototypeStable` DEBUG signing identity and the **real APKs** produced by prior green RC3 Build265 and new RC4 Build266.

1. Install **Build265 debug APK** on a fresh emulator. Install the newest compatible test APK but run its prepare test against the older app process. Explicitly verify installed versionCode 265.
2. Write two creator projects, including one creator-customized record with historical `starter-frame-breakdown` ID, directly into Android TaskStore. Add a paused future Personal Reminder to its separate private DataStore; never schedule, alert or display it.
3. Verify creator backup contains the projects but **never contains private Personal Reminder notes**. Write a small atomic fixture to the app-owned files directory with baseline serialized project and private reminder snapshots.
4. Force-stop the app and use **`adb install -r`** (same package ID and signing certificate) to install Build266 **over Build265**. No uninstall, `pm clear`, backup wipe or data directory replacement.
5. Run a NEW instrumentation process against Build266: assert versionCode 266, the same atomic fixture, both saved creator projects and their scripts/notes, the edited legacy starter, and the private reminder fields. Recheck no private data leaks into Creator backup.
6. Launch the actual new `MainActivity`, force-stop again, then reverify saved data in a third instrumentation process. Clean up only the uniquely named disposable test fixtures/records after successful checks.

## Standard suite
The normal Build266 job also runs complete JVM tests, Android lint, signed full owner APK and upgrade-signing comparison against QA-green Build265; Android 11/15 repeat the previous Spark voice, personal follow-ups, workspace Publish guidance, cold-start Home and backup UI tests. The new upgrade jobs are additive, not substitutes.

## Release boundaries
- Passing emulator upgrades is strong evidence but **not owner physical-device proof**. OEM battery management, alarms, original data density, external documents, backed-up media, OS permissions, real personal repeat schedules and Play Store distribution still need human acceptance.
- The newest debug APK and previous debug APK use the same checked-in **public prototype signing certificate**, just as current owner sideload `personalStable` APKs use it. This is NOT a production signing identity.
- CI intentionally refuses to execute the data-writing test outside a disposable CI emulator.
- Keep `main` untouched. This is a stacked draft on RC3 #177; see release tracker issue #174.

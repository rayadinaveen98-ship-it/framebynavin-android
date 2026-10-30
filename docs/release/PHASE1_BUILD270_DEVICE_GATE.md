# Backlot Phase 1 — Build270 stability, QA and owner acceptance gate

**Candidate:** Full signed, nondebuggable Backlot Build270 owner APK, SHA-256 `74ec1aa31fd9cf9083103423bc6040e2451db1fb454419823150f5cd1b568780`, versionCode 270, package `com.framebynavin.app`. Feature source is strictly immutable at base `9b21e9631c2a19a44c4bb98787387ee10e428b7e`.

## Goal: finish stability QA, not add feature phases

The user asked to finish Phase 1. This branch adds **only instrumentation/smoke orchestration and evidence**. No app source, Gradle settings, APK signer, permissions, cloud backend, privacy website or Play Store state is changed. Repeat prebuilt debug/unit-tested artifacts; validate their versions and run installed UI tests plus **install the exact final signed 18MB APK** on *disposable* Android emulators for cold launch and relaunch.

## Automated matrix (two emulators)

- **Android 11 compact phone, high font:** 900×1900 px at 400dpi with 130% font, Android API30.
- **Android 16 tablet-style display:** 1600×2560 px at 320dpi with 115% font, Android API36. Before normal tests this device runs a real **Build269 → Build270 in-place upgrade without uninstalling** the debug test app, validating synthetic Creator project+workspace and Personal reminder persistence.
- Both devices run the **same 17 test classes** for cold project load, Save Idea footer/keyboard/navigational obstruction, slideshow and Home order, companion UI, Personal editor/backup, Creator portable backup integrity, corruption and IO failure, reminders/denied full-screen permission, Privacy and widgets.
- After instrumented tests, a **disposable CI emulator only** uninstalls its synthetic debug/test package, installs the **real unchanged nondebuggable signed Build270 owner APK**, checks process startup, captures a screenshot, force stops and launches again. This does NOT simulate in-place user signing migration; the debug vs prototype signer are intentionally different.
- Collect CI logs, device geometry, screenshots and stable SHA evidence as artifacts for review. Never use `pm clear`, uninstall or reset on a real user phone.

## Exit gate

- [ ] Automated Build270 artifact checksum and package verified.
- [ ] Android 11 compact UI+data tests pass.
- [ ] Android 16 tablet UI+data tests pass.
- [ ] Existing Build269→270 synthetic in-place upgrade passes.
- [ ] Exact full signed owner APK launches and relaunches on both emulators.
- [ ] No release-blocking defect revealed in tests; if any, fix and rerun.
- [ ] **Owner physical phone acceptance**, separate and mandatory: install only over matching prototype signer after exporting Creator+Personal backups; exercise own projects, cold reopen, Save Idea with keyboard, personal alarms and notification permissions, slideshow, widget pins, backup/restore and device-specific UI. No automation can claim this is done without owner report or equivalent device evidence.

**Phase 1 automated exit can be reported independently from owner physical-device gate.** Do not mark the entire Phase 1 complete or claim Play approval until the last checkbox is fulfilled.

## Known release gates that are NOT Phase 1

Phase 2/3 production account deletion + external working website; Phase 4 signing and branch protection; Phase 5 Play policies and review. These are not UI stability defects and do not justify inventing another RC number.

# Backlot V309 — Direct V294 installed-owner upgrade QA

**Testing branch only. This does not approve, install, or ship an APK.**

## Why this gate exists
The owner previously validated V294. A signed V305→V309 upgrade passing would NOT necessarily prove data preservation from V294. This second, separate QA branch deliberately tests the direct V294→V309 path on synthetic disposable Android 15 devices.

## Frozen production source
Base: `qa/backlot-v308-owner-preservation-309` at `727d0dd815a622d9b6fcfee9ec64b7ef81b0db92`. All production files and versionCode 309 remain unchanged. Only new direct upgrade tests, shell runner, workflow and this documentation are included.

## Acceptance
- Build identical `personalStable` V309 non-debuggable preview signer; compile matching Android test APK.
- Download verified signed V294 artifact from successful CI run `42048375479`.
- Verify original V294 and new V309 APK SHA-256 and actual V2 certificate matching pinned public preview signer, package name and increasing Android version.
- Scenario A: install V294, seed synthetic Creator project, Daily Personal Voice reminder with custom settings, confirmed Done receipt, Friday weekly slot; install V309 with `adb install -r` without clearing seeded data and check every expected record.
- Scenario B: fresh emulator scenario, install V294, seed synthetic Space, private JPEG and journal check-in; install V309 with `adb install -r` without clearing seeded data and check Room records, dimensions and verified private bytes.
- Both scenarios verify Android package UID remains unchanged. No owner device, account, cloud or photos are accessed.

## Release holds
- Direct upgrade CI passing and visual review of V306–V308 screenshots, clean Creator layout and locked-screen physical testing still required.
- V308.1 Spark individual-dismissal feature is deliberately not in this frozen V309 candidate. It requires its own test/release candidate if accepted.
- Play Store private signing and real full-coverage cloud restore remain separate unfinished tasks.

# Backlot V309 — Signed Activity Center owner-acceptance candidate

> **QA-only, not approved for installation or Play Store.**
> Source pinned to V308 draft SHA `552fc74d3cd154bbc3d4fc1fad9ec0af4c559ff0`.
> V294 owner's installed app remains untouched.

## Scope
- V306 Activity Center, V307 compact Creator Reminders, and V308 Spark's multi-item activity digest combined **without new scheduling/storage/cloud behavior**.
- Keeps the exact package `com.framebynavin.app` and known **public sideload preview** signing certificate, not a Play production signing identity.
- Candidate `versionCode=309`, `versionName=2.29.0-rc1-activity-owner-qa`. This permits an in-place V294 or V305 preview upgrade only when certificate identity matches and the build is formally approved.
- No owner phone or connected account is ever operated on during automated QA.

## Gate 1: source/compile
- V306, V307, V308 dedicated test workflows must pass on their exact merged/verified heads, and their Android screenshots require human visual inspection. The V309 branch does NOT override those requirements.
- V309 workflow has a strict allowlist: version metadata, two new synthetic upgrade fixtures, QA workflow and shell script, and this note.
- Build a non-debuggable `personalStable` app and matching test APK on Android CI.

## Gate 2: signed identity and real data-preservation scenarios
- Download prior *verified* signed V305 preview artifact (code 296).
- Check both SHA-256 file digests, Android package names, increasing version code, and exact public V2 signer certificate.
- **Scenario A**: on disposable Android 15 emulator, install signed V305, seed a synthetic Creator Task, custom Friday weekly-plan slot, Personal Voice reminder, and confirmed Done receipt; install V309 with `adb install -r` without clearing any data; verify every relevant field and receipt.
- **Scenario B**: independently install V305, seed Space, journal and private JPEG with verified image hash and dimensions; install V309 with `adb install -r` without clearing; verify Room rows, note and private JPEG byte integrity.
- Both scenarios check Android package UID continuity. Uninstall/clear only on the disposable emulator **before seeding** or **between** independent scenarios; never after a seeded upgrade.

## Still not proven
- Full physical Android/One UI behaviour: locked-screen full-screen alerts, 3x voice repeats, music ducking, music files, snooze, follow-ups, and battery restrictions.
- Backlot's full all-domain cloud restore or V305 escrow commit/rollback.
- Production signing, Play Store requirements and any release approval.
- Visual match to design prototype until screenshots are inspected.
- Direct V294 to V309 full synthetic upgrade (V305->V309 is the two-domain signed fixture here); source-specific V294-to-V309 coverage should be added before declaring universal compatibility.

## Decisions
Until every gate is green and owner acceptance completes, the **V309 APK remains a private QA artifact**. Do not announce it as stable or ask the owner to uninstall the existing app.

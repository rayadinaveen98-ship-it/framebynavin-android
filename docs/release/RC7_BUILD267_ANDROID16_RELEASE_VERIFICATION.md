# Backlot RC7 — Build267 Android 16 and Play-release verification

**Engineering evidence only, 29 October 2026.** The app source is frozen at `5b0682140ae19ec698c0c84aa2be8a9939844500` (RC6, Build267). RC7 adds documentation and CI **only**: no new APK, Kotlin changes, images, package ID, data migration, public store upload or signing-key changes. Stable `main` remains untouched.

## Proven baseline

- [RC6 Build267 five-job SUCCESS](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40922583197): full JVM/lint/APK, Android 11 and 15 regression, real in-place Build266→Build267 Creator/Personal preservation on Android 11 and 15.
- Exact QA-green signed preview artifact: `Backlot-RC6-Owner-Full-App-APK` from run `40922583197`. Full file: `Backlot-RC6-Build267-Full-App.apk`, SHA-256 `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d`; certificate is the **public prototype test signer**. A passing certificate-verification test does not authorize Play production use.
- UI refinements: 48dp companion entry icon matching the adjacent plus control, Home images unobstructed until tap reveals the slideshow controls, and post-touch Voice/Alarm action scenes with diffuse moving color waves. Mic/clock particle intro before touch is preserved. Existing user data and reminder action logic unchanged.
- Version: application ID `com.framebynavin.app`, `versionCode 267`, `versionName 2.15.4-rc6-focused-ui-preview`, `targetSdk 36`, `compileSdk 36`; Android system backup disabled (`allowBackup=false`).

## RC7 automated gates

1. **Immutable source and owner APK**: Git diff permits *only* this document, `README.md`, and `backlot-rc7-build267-release-verification.yml` relative to exact frozen RC6 commit. Assert package/version/SDK, Separate Creator and Personal backup policy and risky permission declarations; download successful run's **existing** Build267 full owner preview APK and check its artifact hash, signature validity, app identity and target API.
2. **Android 16 API36 app/UI**: Install the exact Build267 debug and AndroidTest APKs from the RC6 successful run onto an empty, disposable Android 16 emulator. Check Home cold start, Creator/Personal backup UI, private reminder Hub, Workspace, Spark, compact companion header interaction, clean slideshow 1/3-image visible controls, and the original mic/clock vs post-touch wave for Voice/Alarm.
3. **Android 16 deeper reliability**: Exercise alarm schedule/action isolation/notification channel, separate Personal portable backup, Save Idea keyboard footer, personal reminder editor, and YouTube Top Performers order using the same unchanged APKs.
4. **Android 16 true in-place upgrade**: On a disposable API36 emulator seed Creator and Personal records under *Build266*; install *Build267* **without uninstall/clear**, then assert exact records remain and validate post-cold-relaunch. This is only a same-debug-signer upgrade, **not** evidence for a different Play certificate.
5. **Unsigned production AAB structural dry-run**: Gradle `bundleRelease`, inspect container, confirm no embedded production signature, upload **only hashes and buildability evidence**. Never publish an AAB or handle private signing secrets inside this CI.

The RC7 CI result cannot substitute for a tester's device experience. Emulator success, Play compliance and user acceptance are independent gates.

## Owner phone acceptance (required before stable or Play)

- [ ] Export & verify **Creator backup** and separately export/verify **Personal Reminders**; attach source media separately, because project attachments may only be references in backup.
- [ ] Confirm current installed package certificate is compatible with prototype-signed Build267. Install over existing preview (not uninstall), then compare project details and private reminders across cold launch and restart. Stop if signature mismatch.
- [ ] On real Home, confirm companion icon is 48dp and visually aligned with adjacent plus; companion sheet opens; Spark still operates when supported.
- [ ] With 1+ photos, confirm images start completely free of Pause/Play and vertical dots; tapping an image reveals both controls, and they disappear after inactivity; swipes, autoplay, saved pause state, change, reorder and removal still work. Test phone at default/large font sizes.
- [ ] On Voice and Alarm, verify pre-tap mic/clock still animates with particles, and tapping swaps **only background** to cinematic motion with no remaining bokeh/particles behind crisp action buttons. Test reduced-motion and battery-saver modes, lock screen, brightness, normal alarm delivery, snooze/done/dismiss.
- [ ] Cold-start Home projects and top performers order, Idea Save button (keyboard open, gesture/3-button nav, small screens), Creator vs Personal reminder data separation, backup/restore, Workspace publishing state, widget visibility and permission fallback.
- [ ] External project assets, YouTube OAuth integrations, restored private data, notifications/quiet hours and reboot/Doze recovery on the actual owner device.

## Unresolved Play release blockers (remain **OPEN**)

- [Issue #174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174): integrated stacked-PR audit; owner phone acceptance; production signing; Data Safety, privacy policy, support/metadata, permission eligibility and staged Play internal testing.
- [Issue #180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180): migration between public prototype signing identity and private Play key. A differently signed APK with the **same package ID generally cannot install as an in-place update**; don't advise uninstall or data wipe. Choose explicit owner-approved strategy (e.g. side-by-side distinct Play ID, or protected export/restore migration), verify on disposable second device/profile, then decide private upload/Play App Signing identities.
- Permission eligibility still needs independent Play review: `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, foreground media playback and privacy declarations; `targetSdk 36` alone is insufficient.
- A private production signing key must never be placed in source control or automatically inferred from the public prototype signer. Build267 preview signing is **not** production signing.
- **No main merge or Play submit without explicit owner approval.**

## RC7 completion criteria

All five independent RC7 jobs green, generated JSON and sha256 evidence available; updated acceptance checklist; Build267 app source still exactly pinned to `5b0682140ae19ec698c0c84aa2be8a9939844500`. This advances engineering confidence only; keep release PR **draft** pending physical-device and Play gates.

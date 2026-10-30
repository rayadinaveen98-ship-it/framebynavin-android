# Backlot RC5 — frozen Build266 source: audit, Play preflight and owner acceptance

> **Engineering gate only, 29 October 2026.** RC5 changes documentation and CI checks, **not** Kotlin, Android resources, backup schemas, signers, runtime behavior, or the APK version. No automatic merge or Play submission.

## 1. Immutable source / ancestry inventory

| Fact | Evidence / value |
| --- | --- |
| Repository | `rayadinaveen98-ship-it/framebynavin-android` |
| Source snapshot for owner full Build266 APK | `7fdda27b22dfe7e971ce370ab366a33520ee691a` |
| Frozen Android application ID | `com.framebynavin.app` |
| Frozen version | `versionCode 266` / `2.15.3-rc4-inplace-upgrade-data-preview` |
| GitHub CI | [40889471748](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40889471748), all 5 jobs successful (Build/JVM/lint, Android11, Android15, API30 and API35 in-place upgrade) |
| Base branch | `main`, HEAD `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb` at inventory time |
| Git graph | frozen RC4 1,241 commits ahead / 0 behind of `main` |
| Pull-request chain | 30 contiguous **draft** PR dependencies, starting [#148](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/148), ending [#178](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/178); no missing link found at snapshot |
| Build certificate | `personalStable` is non-debuggable but uses publicly committed `prototypeStable` **development** key, not a Play production signing key |
| Release candidate state | **NOT stable / NOT production approved** |

### Invariants

- Release preflight checks must pin the exact Build266 app source SHA. Any `app/` or `gradle/` modification after that requires a **new** full build and device matrix; a documentation-only branch may not silently become a new APK.
- Never merge 30 draft PRs to `main` by blindly clicking “merge”: independent integration review and a recorded rollback strategy are mandatory. Compare lists may be truncated by GitHub at 300 files; this inventory is **not** a complete code security review.
- Preserve creators' existing notes, projects, edited historical starter IDs, local media, active reminder journals, personal reminders, and cloud authentication. No `pm clear`, uninstall or restore/import without owner authorization.

## 2. Current app / policy facts

- `app/build.gradle.kts`: `minSdk 26`, `compileSdk 36`, `targetSdk 36`. Google Play requires Android 16 **API 36** for new Android mobile apps/updates from 31 August 2026: [official policy](https://support.google.com/googleplay/android-developer/answer/11926878). This *single* requirement is met, but is **not** Play readiness by itself.
- AndroidManifest declares `SCHEDULE_EXACT_ALARM` and `USE_FULL_SCREEN_INTENT`; these need a Play Console permission declaration/eligibility review and actual behavior tests. Full-screen intent automatic access is restricted to core alarm/call use cases since Android 14: [Play Help](https://support.google.com/googleplay/android-developer/answer/16965181); users must be given proper permission/notification fallback paths. **Do not claim approval without a Play Console decision.**
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, microphone, calendar and notification permissions require least-privilege review, in-context consent, behavior validation and accurate Play Data Safety declarations.
- The manifest has `android:allowBackup="false"`: Android system backup is intentionally disabled. Verified in-app Creator + separate Personal export/restore and user education are therefore mandatory.
- Creator backups contain Creator project reminders, saved Voice Idea recordings and Best Frames where present, **not** private Personal Reminders or source project attachment files (only attachment references). Verify these separately before migration.
- A signed personal APK is **not** a Play upload AAB, and the prototype keystore is public. Production signing credentials must be privately provisioned, backed up securely and kept outside the repository.

## 3. Automated RC5 checks (no new APK build)

**Preflight job:** compare source tree against the frozen RC4 commit (allow only this document, README, and the RC5 CI workflow), assert version/package/target/manifest/privacy contracts and prototype-signing warning. Export a machine-readable JSON evidence file and verify the exact complete Build266 owner APK file/checksum/certificate/version using GitHub Actions artifact from run `40889471748`.

**Android 16 job:** download exactly the QA-green Build266 *debug* + AndroidTest APK artifacts from run `40889471748`, install on a disposable API36 emulator, then exercise cold-start project loading, Creator/Personal backup UI, private reminder hub, Content Workspace and Spark user-facing functionality. This does not replay new build code, change owner data, or test a Play production signer. Failure is a blocker; log the RCA without claiming data corruption.

**Already verified:** the RC4 Android11/15 normal test suites and real 265→266 in-place upgrades, including saved customized historical starter and separate private reminders.

## 4. Owner-phone acceptance (pending, no automated claim)

Use the actual Build266 owner preview **after** separately exporting a Creator backup and a Personal Reminders backup, both reopenable/valid. Keep the existing installed app; **do not uninstall**.

- [ ] **On-phone identity / upgrade:** confirm current package signature is compatible; in-place install over prior preview; check no “App not installed” error. Note Android version, phone model, font/display scaling and battery restrictions. If signature mismatch, stop—do not wipe.
- [ ] **Creator data:** before and after upgrade/restart, compare project counts, titles, scripts, notes, workflow stage, scheduled reminders, linked YouTube records and published/archived statuses.
- [ ] **Personal private reminders:** compare records, recurrence, local time zone, upcoming occurrences and notifications; private records must stay independent of creator/cloud data.
- [ ] **Backup round-trip:** export Creator and Personal files separately, verify each file by reopening in preview, confirm media included vs referenced, and prove restore on a **disposable second device/profile** before risking primary data.
- [ ] **Cold launch:** force-stop and relaunch; initial project loading state must not falsely show “no projects”, and Hero/YouTube slides must not drop below Up Next when enabled.
- [ ] **Save Idea and editing layout:** test system 1×/1.5×/2× font, small phone, landscape/portrait where supported, keyboard open, gesture navigation and 3-button navigation. Full Save Idea control must remain visible/tappable.
- [ ] **Creator reminder delivery:** permission grant/denial, exact alarms, full-screen intent approval/denial, quiet hours, snooze, Done, Dismiss, timeout, late reminder recovery; test screen locked and battery-restricted state where permitted.
- [ ] **Personal delivery:** notification, voice and alarm; no false “Done” after no response; opt-in follow-up, quiet hours, missed/retired reminders, recurrence after restart and reboot.
- [ ] **Companions / Spark:** Spark first onboarding only; other companions do not prompt setup; 50% scaled artwork without clipping; offline speech + text fallback; mic permission refusal; cancel/stop; no inadvertent cloud speech.
- [ ] **Visual polish:** compare intended vibrant wave/alarm glow and contrast on low/high brightness, dark-mode screen, reduced-motion, high-density and accessibility font scaling.
- [ ] **Widgets:** prior widgets do not silently disappear without migration support; modern widgets refresh on schedule without exposing private reminder notes on an unlocked shared home screen.
- [ ] **Workspace / Publish:** incomplete fields and thumbnail guidance; never auto-mark posted/published without creator confirmation; deep links and offline state.
- [ ] **Privacy/security:** Play Data Safety audit, OAuth scopes, consent, permissions and source project attachment limitations are accurately disclosed.

## 5. Production and integration gates (all still open)

1. [ ] Review all 30 draft dependency PRs **and** source delta from `main`. Capture merge-order proof/rollbacks and any required migration strategy; consider moving acceptance-tested app snapshot to a dedicated protected candidate branch. Do not merge to stable without owner approval.
2. [ ] Complete Android 16 emulator matrix and physical-device acceptance above; resolve any findings without reusing the older “frozen Build266” evidence after code changes.
3. [ ] Validate new `targetSdk 36` background/notification/full-screen changes on actual API36 device, permission prompts, Firebase/AppCheck and SDK behavior.
4. [ ] Resolve permission declarations and eligibility in Play Console: `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, and foreground services.
5. [ ] Provision privately controlled production signing/upload key and Play App Signing; generate and verify an `app-release.aab` that is **not signed with the public prototype key**. Compare package identity against proposed app listing.
6. [ ] Privacy policy, support contact, screenshots, content rating, Data Safety declarations, testing track, staged rollout and rollback/incident plan.
7. [ ] Explicit owner sign-off for the candidate merge and separately for Play submission.

**Green preflight ≠ Store approval. This gate explicitly records outstanding blockers rather than silently claiming completion.**

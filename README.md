# Backlot Android

Backlot is an Android-first, offline-first creator workspace built with Kotlin and Jetpack Compose. Capture ideas, develop scripts/projects, manage production and reminders, record publication and review authorized channel results.

## Latest verified candidate — RC7 audit of Build267 (29 October 2026)

**Build267 RC6 source** `5b0682140ae19ec698c0c84aa2be8a9939844500` has a [five-job green Android 11/15 and real 266→267 upgrade matrix](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40922583197). The compact Home companion icon, tap-to-reveal slideshow controls and post-tap cinematic Voice/Alarm wave background are covered by the updated tests. The mic/clock particle introduction is unchanged. Latest owner preview file `Backlot-RC6-Build267-Full-App.apk`, SHA256 `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d`.

**RC7 is only a release verification branch.** It adds Android 16 API36 device, 266→267 in-place upgrade, unsigned AAB structure and source/hash/certificate integrity checks against **unchanged** Build267 artifacts. [RC7 acceptance dossier](docs/release/RC7_BUILD267_ANDROID16_RELEASE_VERIFICATION.md). `main` stays untouched. No new Build268 or production key is implied.

**Do not equate QA-green preview with Play ready.** Owner physical-device validation, separate Creator and private Personal backup/restore, reliable exact alarms, private Play signing and [signer-migration issue #180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180) remain open. Do not uninstall an existing prototype-signed app to swap certificates without an explicit safe migration plan and verified off-device backups.

## Current engineering gate — 29 October 2026

**Frozen candidate:** **Backlot RC4 Build266** (`2.15.3-rc4-inplace-upgrade-data-preview`, application ID `com.framebynavin.app`) from exact source commit `7fdda27b22dfe7e971ce370ab366a33520ee691a`. [RC4 five-job GitHub QA](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40889471748) passed JVM/lint/build, Android 11 + 15 regression, and real 265→266 `adb install -r` persistence on disposable emulators. **This is not Play Store release approval and has not been accepted on the owner's physical phone.**

**Release consolidation, not a new app version:** RC5 freezes the Build266 app source and adds an Android 16 (API 36) emulator smoke matrix, an auditable Play-preflight gate and a physical-phone acceptance checklist. [RC5 release audit](docs/release/RC5_BUILD266_RELEASE_ACCEPTANCE.md) records the remaining blockers. Stable `main` is unchanged. The RC4 commit is **1,241 commits ahead of `main`** and includes a contiguous **30-draft-PR** stack (#148 through #178, not every PR number contiguous), not one small isolated PR.

**Different release states:** historical V195.5 personal-sideload stable, historical creator-approved V208 phone preview, engineering QA-green Build266, and an eventual Play production-signed binary are **not interchangeable**. The Build266 `personalStable` APK uses a publicly checked-in prototype signer and **must not** be submitted to Play. Creator and Personal reminder backups are separate; external project attachment files are not packaged in Creator backups. Never uninstall Backlot to upgrade without protecting user data.

## Historical release notes — 23 October 2026

**V210 P4.6, Build 237 is CI-green but not stable.** [GitHub Actions run 39934884089](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39934884089) passed the Android build and device-emulator suites, including the previously failing Best Frames checksum tests. The source is on [draft PR #100](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/100), not merged into `main` or any stable branch.

**Three different statuses matter:** the frozen personal stable is **V195.5**; the last creator-accepted personal phone preview is **V208**; the newest fully CI-tested **engineering candidate** is **V210 P4.6**. None is a commercial Play Store release. Next gate: non-destructive on-device V210 upgrade and backup preview checks, with low-memory/disk-loss stress separately tracked. See [current release ledger](docs/release/CURRENT_STABLE.md) and [P4.6 acceptance checklist](docs/qa/V210_P4_6_STREAMED_FRAME_EXPORT.md#p46-engineering-acceptance-and-release-freeze-23-october-2026).

## Release baseline

The latest frozen personal-sideload stable snapshot is **V195.5 / 2.5.3-v195.5-stable** on `stable/backlot-v195-5`. The `main` branch retains the older V183 stable lineage; these are distinct and neither is a Google Play production release. See [current stable and candidate ledger](docs/release/CURRENT_STABLE.md) and [V195.5 release record](docs/releases/BACKLOT_V195_5_STABLE.md).

**Latest creator-accepted personal phone preview:** **V208 / 2.8.2-v208-setup-backup-preview** (versionCode 232, commit `05a082d`). The [V208 PR](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/87) is still a draft; CI and the creator's personal-install feedback do not make it a Play Store release.

**Historical maintenance work:** V209 P1 is a **non-functional trust/release hygiene phase** based on V208. It documents release and backup coverage, ignores future generated build files, and verifies the existing package/signing identity. It creates no replacement APK. V184–V207 remain historical milestones. See [V209 P1 QA](docs/qa/V209_TRUST_RELEASE_HYGIENE_P1.md).

**Earlier personal preview candidate:** **V209 P2 / 2.8.3-v209-backup-coverage-preview** (versionCode 233) improves truthful portable-backup coverage and shows original Best Frames versus reference-only project attachments during restore preview. This **changes the app**; it is not automatically stable or installed on user devices. See [V209 P2 QA](docs/qa/V209_BACKUP_COVERAGE_P2.md).

## Development and acceptance

- [Approved master plan](docs/roadmap/BACKLOT_IMPROVEMENT_MASTER_PLAN.md)
- [V184 scope, preservation inventory and acceptance](docs/qa/V184_CAPTURE_SAFETY.md)
- V184 workflow: `.github/workflows/backlot-v184-capture-safety.yml`.
- Build gates: inherited source contracts, JVM tests, lint, instrumentation compilation and an install-compatible preview APK. Emulator execution and physical-device acceptance are separate gates.
- Do not reset creator data or uninstall as an upgrade procedure. Preserve package/signature, original images/recordings, existing schedules and publication records.

Core creator work remains local. Cloud, YouTube and AI access have their own configuration/consent. The public prototype signing identity supports personal previews; it is not a privately controlled commercial signing identity.


**V209 P3 draft candidate:** `2.8.4-v209-best-frames-portable-preview` (versionCode 234) moves original Best Frames image bytes from the 32 MiB portable JSON entry into separately verified ZIP media. Core backup schema, project attachment references and production signing remain unchanged. This version is **not stable**. See [P3 QA and remaining memory limits](docs/qa/V209_P3_PORTABLE_BEST_FRAMES.md).

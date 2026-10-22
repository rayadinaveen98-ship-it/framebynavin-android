# Backlot Android

Backlot is an Android-first, offline-first creator workspace built with Kotlin and Jetpack Compose. Capture ideas, develop scripts/projects, manage production and reminders, record publication and review authorized channel results.

## Release baseline

The current stable personal-install baseline is **V183 / 2.3.3-stable**. See [current stable](docs/release/CURRENT_STABLE.md) for source, CI and distribution scope. Historical V149 records remain available as rollback evidence; they are no longer the current development baseline.

**Latest verified personal preview:** **V208 / 2.8.2-v208-setup-backup-preview** (versionCode 232, commit `05a082d`). The [V208 PR](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/87) is still a draft; CI and the creator's personal-install feedback do not make it a Play Store release.

**Current maintenance work:** V209 P1 is a **non-functional trust/release hygiene phase** based on V208. It documents release and backup coverage, ignores future generated build files, and verifies the existing package/signing identity. It creates no replacement APK. V184–V207 remain historical milestones. See [V209 P1 QA](docs/qa/V209_TRUST_RELEASE_HYGIENE_P1.md).

## Development and acceptance

- [Approved master plan](docs/roadmap/BACKLOT_IMPROVEMENT_MASTER_PLAN.md)
- [V184 scope, preservation inventory and acceptance](docs/qa/V184_CAPTURE_SAFETY.md)
- V184 workflow: `.github/workflows/backlot-v184-capture-safety.yml`.
- Build gates: inherited source contracts, JVM tests, lint, instrumentation compilation and an install-compatible preview APK. Emulator execution and physical-device acceptance are separate gates.
- Do not reset creator data or uninstall as an upgrade procedure. Preserve package/signature, original images/recordings, existing schedules and publication records.

Core creator work remains local. Cloud, YouTube and AI access have their own configuration/consent. The public prototype signing identity supports personal previews; it is not a privately controlled commercial signing identity.

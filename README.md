# Backlot Android

Backlot is an Android-first, offline-first creator workspace built with Kotlin and Jetpack Compose. Capture ideas, develop scripts/projects, manage production and reminders, record publication and review authorized channel results.

## Release baseline

The current stable personal-install baseline is **V183 / 2.3.3-stable**. See [current stable](docs/release/CURRENT_STABLE.md) for source, CI and distribution scope. Historical V149 records remain available as rollback evidence; they are no longer the current development baseline.

**V184 / 2.3.4-v184-capture-safety-preview** starts the approved improvement plan. Its first package protects Quick Capture drafts and finalized voice takes. It is a preview, not a stable promotion.

## Development and acceptance

- [Approved master plan](docs/roadmap/BACKLOT_IMPROVEMENT_MASTER_PLAN.md)
- [V184 scope, preservation inventory and acceptance](docs/qa/V184_CAPTURE_SAFETY.md)
- V184 workflow: `.github/workflows/backlot-v184-capture-safety.yml`.
- Build gates: inherited source contracts, JVM tests, lint, instrumentation compilation and an install-compatible preview APK. Emulator execution and physical-device acceptance are separate gates.
- Do not reset creator data or uninstall as an upgrade procedure. Preserve package/signature, original images/recordings, existing schedules and publication records.

Core creator work remains local. Cloud, YouTube and AI access have their own configuration/consent. The public prototype signing identity supports personal previews; it is not a privately controlled commercial signing identity.

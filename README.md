# Backlot for Android

Backlot is an Android-first creator operating system built with Kotlin and Jetpack Compose. It is designed around the full creator loop: **Idea → Creation → Execution → Publication → Performance → Learning → Next Idea**.

> Repository and package identifiers still contain the historical `framebynavin` name for compatibility. The user-facing product identity is **Backlot**.

## Current stable daily-use build

Backlot **v139** is the current locked daily-use build.

- Version code: **139**
- Version name: `2.0.0-rc12-theme-system-finalization`
- Validated source commit: `a02057599e75e5af2fdc8f581074585c8587e1f9`
- Frozen release branch: `release/backlot-v139-final`
- Validation workflow: run `34986413539` — **SUCCESS**
- APK: `Backlot-v139-ThemeSystem.apk`
- APK SHA-256: `5414831f8ef40177cec3746c29b2d755cce180abcc489f3bad78cf1363eb62bd`

The exact stable state and hold policy are recorded in [`docs/release/CURRENT_STABLE.md`](docs/release/CURRENT_STABLE.md).

## v139 visual system

v139 is the current visual-personalization checkpoint. It keeps Director's Cut as the classic Backlot black/red identity and separates the previous gold direction into Studio Gold.

The ten available visual languages are:

1. Director's Cut
2. Studio Gold
3. Mono Ink
4. Ivory Atelier
5. Paper Quiet
6. Moss Studio
7. Terracotta Calm
8. Night Bloom
9. Blue Hour
10. Storyboard

Frame/Navi guide identity, current Backlot splash/launcher identity, widgets, and Quick Idea are included in the daily-use build. Quick Idea follows the active visual language.

## Product scope

Backlot currently includes creator planning and execution tools such as Idea Vault, projects and workflow stages, Project Pulse, reminders and alarms, Daily Brief, calendar and publishing flows, creator analytics and Insights, workflow intelligence, opportunity/context tools, account/profile and backup/cloud capabilities, widgets, Best Frames, Cine Pulse/guide experiences, and supporting creator utilities.

The long-term differentiator is not a generic assistant. Backlot should learn from the creator's own history: how an idea was researched, hooked, scripted, produced, published and performed, then turn that evidence into better next decisions.

## Build and verification

GitHub Actions is the authoritative Android build environment. The v139 validation completed successfully for:

- theme contract verification
- user-facing Backlot brand audit
- unit tests
- Kotlin compilation
- debug APK assembly
- artifact packaging/upload

Physical-device visual quality and daily-use UX are validated separately through real use; a green CI build is not treated as physical-device visual approval.

## Branch policy while v139 is being used

- `release/backlot-v139-final` is the frozen rollback/reference snapshot and must not be developed on directly.
- `main` is the current repository source-of-truth line.
- Future product work should start in a new feature branch after daily-use observations from v139 are reviewed.
- Do not rewrite compatibility identifiers such as the Android application ID solely for branding cleanup.

## Android compatibility

- Application ID: `com.framebynavin.app`
- Minimum SDK: 26
- Target SDK: 36
- Java/JDK: 17

The application ID and some internal class/theme/backup identifiers intentionally retain historical naming to protect compatibility and existing data.
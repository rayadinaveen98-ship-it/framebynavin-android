# Backlot Current Stable

## Status

**LOCKED FOR DAILY-USE VALIDATION**

As of 2026-09-15, Backlot **v139** is the current stable daily-use build. We are intentionally holding feature work on this snapshot while it is used on a real device for several days and practical issues are collected.

## Exact frozen snapshot

- App: **Backlot**
- Version code: **139**
- Version name: `2.0.0-rc12-theme-system-finalization`
- Validated source commit: `a02057599e75e5af2fdc8f581074585c8587e1f9`
- Frozen branch: `release/backlot-v139-final`
- Development lineage: `feature/v139-theme-system-finalization`
- CI workflow run: `34986413539`
- CI result: **SUCCESS**

The frozen release branch points exactly at the validated source commit above. Do not move or develop directly on `release/backlot-v139-final`.

## Validation completed

The v139 workflow completed successfully with:

- v139 theme contract verification
- user-facing Backlot brand audit
- unit tests
- Kotlin compilation
- debug APK assembly
- installable APK artifact packaging/upload

Physical-device visual and day-to-day UX validation remain intentionally in progress through real usage.

## Daily-use APK

- Artifact name: `Backlot-v139-ThemeSystem.apk`
- SHA-256: `5414831f8ef40177cec3746c29b2d755cce180abcc489f3bad78cf1363eb62bd`

## Product state locked in v139

- Backlot is the user-facing product identity.
- Director's Cut is restored as the classic Backlot black/red visual identity.
- Studio Gold remains a separate visual language.
- Ten theme systems are available for real-device evaluation.
- Frame/Navi guide identity and the current personalization system remain part of the build.
- The current splash/launcher Backlot identity remains part of the build.
- Quick Idea follows the active visual language.

## Rule for the next iteration

Do not change the frozen v139 branch. Collect observations from daily use first. When development resumes, preserve this branch as the rollback/reference point and make the next version from the current main lineage in a new feature branch.

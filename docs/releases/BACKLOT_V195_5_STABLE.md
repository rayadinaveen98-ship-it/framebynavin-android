# Backlot V195.5 — Stable snapshot

Date: 2026-10-20 (IST)

Source: feature/backlot-v195-5-premium-particles-reminder-confirmation
User-accepted source SHA: ba1d0ce0cdd53b21003ee6de31cf5877ff88e99b
Reference green CI: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39313436911
Reference verified APK SHA-256: b8fe7af47ab3dca1ad91a2881511559670ec1db7718addec912e0c6464622949

## Stable promotion policy

- The stable branch is a frozen source snapshot; only release metadata and this verification workflow/document differ from the previously approved preview.
- Stable versionCode: 203, versionName: 2.5.3-v195.5-stable.
- Rebuild the same code, run all inherited source contracts, unit tests, lint, signed personalStable APK assembly, and Android emulator acceptance.
- App package, signing configuration, data model, schedules, notification behavior, Spark art and premium particle engine are deliberately untouched.
- Do not change any feature code in this branch without opening a separate fix branch, testing it end-to-end, and checking device compatibility.
- Do not force-move the main branch from its existing stable generation while the earlier stacked integration PRs remain unmerged.

## User-approved behavior

- Hybrid crisp circular particles with bokeh, smooth infinite edge reflections and vivid colors; 3,024 particles normal mode; reduced density on low power.
- Spark visual treatments, manual reveal, alarm/voice response screens and response actions.
- Compact, immediate after-write reminder-set/update countdown chip.
- Existing project data and persisted reminder schedules must survive in-place updates.

## Release acceptance

- All CI checks successful for the stable branch.
- Verify stable APK download and SHA against build artifact.
- Physical device: install as an update; confirm saved projects/reminders persist, press feedback and screens render correctly, create/update project reminder, open both voice/alarm and confirm timing and normal interactions.
- Automated emulator acceptance cannot certify performance on every physical device or grant restricted Android full-screen permissions.
- No new features while this baseline is frozen; a separate branch is required for future work.

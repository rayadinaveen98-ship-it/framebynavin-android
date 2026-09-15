# Backlot v141 Automated Validation Report

Date: 2026-09-15

## Candidate under validation

- Product: Backlot
- Version code: 141
- Version name: `2.0.0-rc14-welcome-sfx-voice-inheritance`
- Frozen candidate branch: `release/backlot-v141-candidate`
- Validated product source: `86f3c28e1821c12905a7aee4e79809cc9d3b8eae`
- Candidate APK workflow: `35000888096` — PASS
- Candidate APK artifact: `Backlot-v141-welcome-sfx-voice-inheritance`
- Candidate APK SHA-256: `e5d54129da9c385a23efdc9ccf934d4ea72a22bd3fd9af3aa9174becb07c56d7`

The frozen candidate branch was not changed by the validation work below.

## Validation-only branch

- Branch: `validation/v141-emulator-readiness`
- Final validation commit: `aeb3f25ae070a96ba67e75704f387ab6b90a2111`
- Strict validation workflow: `35005576429` — PASS

This branch adds instrumentation tests only. It does not redefine the v141 product candidate.

## v141-specific automated proofs

### Settings voice persistence

`V141VoiceInheritanceInstrumentedTest.settingsVoicePersistsAcrossFreshStoreInstances`

The test writes `VoicePersona.ROBOT` through the production `CreatorOsSettingsStore`, creates a fresh store instance and verifies the persisted `defaultVoicePersona` is still `ROBOT`. The original settings payload is restored after the test.

### Settings -> New Project voice inheritance

`V141VoiceInheritanceInstrumentedTest.newProjectReadsSettingsVoiceAndSavesInheritedPersona`

The test uses the production New Project composer and a real settings store. It sets the global default voice to Robot, walks the progressive New Project flow to Project Support, verifies the UI exposes `VOICE · FROM SETTINGS` and `Robot`, exercises the `CHANGE IN SETTINGS` callback, completes project creation and verifies the resulting `PProjectDraft.voicePersona` is `VoicePersona.ROBOT`.

This proves there is no separate New Project voice preference in the v141 creation path and that the inherited Settings voice survives into the saved draft.

### MainActivity startup / welcome handoff

`V141StartupSmokeInstrumentedTest.mainActivitySurvivesRecoveryAndWelcomeHandoff`

The test launches the real `MainActivity`, holds the app through recovery and beyond the five-second welcome-ident handoff, and verifies the activity remains resumed and is neither finishing nor destroyed.

This is a runtime crash/survival check. It does not assess subjective animation or audio quality.

## Android emulator matrix

The existing Backlot regression suite plus the v141-specific tests above passed on all targeted Android generations:

- Android 13 / API 33 — PASS
- Android 14 / API 34 — PASS
- Android 15 / API 35 — PASS
- Android 16 / API 36 — PASS

Workflow: `35005576429`

Generated instrumentation evidence artifacts:

- `Backlot-v141-api-33-instrumentation-contract`
- `Backlot-v141-api-34-instrumentation-contract`
- `Backlot-v141-api-35-instrumentation-contract`
- `Backlot-v141-api-36-instrumentation-contract`

## What automated validation does NOT approve

Physical-device approval is still required for:

- subjective welcome sound / animation synchronization and perceived quality
- real speaker TTS quality for all eight personas
- mute/media-volume behavior as experienced on the target phone
- alarm sound volume, vibration and full-screen presentation
- lock-screen behavior
- exact reminder timing under real battery optimization / Doze conditions
- notification permission UX on the target device/OEM
- snooze and dismiss feel in normal daily use
- background audio cleanup in real use
- battery/performance/jank observations
- visual quality on the user's actual display

Track those observations in GitHub issue #5: `Backlot v141 physical-device validation log`.

## Release decision

Automated software/runtime readiness: **PASS**.

Physical-device experiential approval: **PENDING**.

Do not promote `release/backlot-v141-candidate` to final solely from CI/emulator evidence. If physical testing is clean, v141 can be promoted without product-code changes. If testing exposes a blocker/high issue, create a focused patch branch from the validated v141 product source rather than modifying the candidate branch in place.

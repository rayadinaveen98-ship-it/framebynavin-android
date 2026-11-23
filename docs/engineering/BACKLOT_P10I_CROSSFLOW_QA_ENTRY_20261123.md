# Backlot P10I — selected first-run and navigation policy regression

## Verified completed gates
- P10F source and Kotlin compile **PASS**: [GitHub run 45151511154](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45151511154).
- P10G P10ReminderVisualContractTest **PASS**: [GitHub run 45155679223](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45155679223).
- P10H Android 15 emulator **PASS** across two verified executions:
  - Creator Voice, Creator Alarm, Personal Voice, Personal Alarm **4/4 PASS** in [run 45163457237](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45163457237), with fifth accessibility case failing because test used physical single tap.
  - Corrected accessibility semantic-Open **1/1 PASS**, and debug+AndroidTest APK build success in [run 45171826200](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45171826200). All five cases now have passing evidence; Android run not a complete first-run or real-device certification.
- P10H test APK artifacts are QA-only and **not** a founder-preview signed PersonalStable APK.

## Why this specific stage
The founder previously observed first-run/onboarding loops, wrong navigation destinations, reminder/Personal switching inconsistencies and an unresponsive Voice Orb in preview builds. Before handing off another APK, check specifically affected pure policy contracts to reduce risk; do not claim policy tests alone prove real-device navigation.

## Exact scope — ten existing test classes
1. P3SignatureTimelineTest
2. P4AccountEntryPolicyTest
3. P5CompanionSelectionPolicyTest
4. P6CreatorEntryPolicyTest
5. P6aUnifiedFirstRunPolicyTest
6. P7PersonalHomePolicyTest
7. P8BLivingVoiceOrbPolicyTest
8. P8IdeaVaultPresentationTest
9. P9AppearanceScopePolicyTest
10. P1ReminderEntranceCompatibilityPolicyTest

The workflow executes only these via `:app:testDebugUnitTest` with explicit `--tests` selectors. It may compile Kotlin prerequisites in a fresh runner, but must not execute the full JVM suite, emulator suites, lint, APK build, signing, main merge, production services or user data operations. If a class fails, repair only affected code and rerun the narrowest group.

**P10I current state:** requested; no result until linked GitHub workflow passes.

## Remaining delivery gate
After P10I selected policies, verify a real first-run navigation journey on Android (not merely pure policy). Run one consolidated final regression only when the release candidate is frozen; create an isolated owner-preview APK and review it on a physical device before any stable promotion. These are not authorized as completed by this source document.

Accepted V337 SHA: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. `main` and stable stay protected. No merge authorized.

# Backlot P10F — First consolidated QA attempt

**Founder authorization:** "ok hopefully we dont mess up this time. go ahead. also remember if it fails in any stage do not run it completely until its necessary. continue"

## Exact protected sources
- Accepted stable baseline: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`.
- Candidate: cumulative P1–P10E source from `feature/backlot-v337-point10e-legacy-reminder-cleanup-after-p10d-20261123`.
- QA branch: `feature/backlot-v337-point10f-focused-qa-run-20261123`, PR targets only the isolated P10E branch **not main**. Never merge without separate founder approval.

## Founder-approved failure and retry policy

1. Run fast source-reference fence without a JVM; stop if it fails.
2. Only after stage 1 succeeds, compile Kotlin target `:app:compileDebugKotlin`. No Gradle clean, no full test suite, no APK at this step.
3. Inspect the **first actual error** (file, line and call), edit only directly affected source and rerun focused gate. Do not restart all completed suites for a narrow error unless dependencies require it.
4. Only after successful Kotlin compilation, stage focused P10 policy and Android UI-test **compilation**; then selective functional regression and PersonalStable preview APK. Stage separately to avoid waste.
5. Hardware/device visual inspection and founder acceptance remain after compilations and artifacts. Never claim a source check is equivalent to actual device testing.
6. No production signing, GitHub main merge, release publish, hosted QA database, credentials or real user data mutation.

## Entry status
- P10A–P10E: GitHub source-staged, not previously compiled/tested.
- P10F Stage 1 source gate: **REQUESTED**, evidence pending.
- P10F Stage 2 focused Kotlin compile: **DEPENDENT ON STAGE 1**, evidence pending.
- P10F Stage 3 targeted tests: **NOT STARTED**.
- P10F Stage 4 owner preview APK: **NOT STARTED**.
- P10F device/visual acceptance: **NOT STARTED**.

Workflow: `.github/workflows/backlot-p10f-focused-qa-gate.yml`, defined on the isolated P10E PR base. This QA branch adds documentation only so the PR can run the exact cumulative candidate without modifying app behavior.

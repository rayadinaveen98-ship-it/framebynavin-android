# Backlot D8 — Activate cumulative unit, lint, Lab APK and Android 15 gate

Date: 2026-11-22

**Founder authorization:** "okay bro go ahead" after approving incremental D8 implementation and conditional final Lab APK tests. Keep all work on the isolated D8 integration branch; DO NOT MERGE, promote stable, deploy UX09/cloud, or release production.

## Verified compile evidence and source identity

- Compile-only workflow: [44998165465](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44998165465).
- Both preflight and Lab Kotlin compile jobs returned **success**.
- Compiled SHA: `f16c4d45ce452730b1524af0db6ad5d3463d1399`.
- Current candidate before this marker: `ba2893d79e39f90b295291898304a27612703282`.
- Comparison from compiled SHA to candidate: one workflow-only file `.github/workflows/backlot-d8-candidate-final-verification.yml`. **Zero app/Gradle/source changes**; the compiled application source is the exact current application source.
- Previous initial preflight (run 44998108552) failed exclusively on old docs Markdown trailing-space formatting. Workflow-only preflight correction fixed scope handling without editing historical docs.

## Trigger meaning

This marker is the **only push path** activating `.github/workflows/backlot-d8-candidate-final-verification.yml`. The workflow runs source preflight, independently scheduled JVM unit regressions and Android lint. Lab APK + instrumented-test APK compilation occurs only if both pass. The optional Android 15 synthetic test follows package/signing identity and SHA validation.

On failure, use the exact failed diagnostic log and source SHA. Apply smallest isolated fixes, rerun compile-only on source changes, then amend this gate marker only after new compile success. Never reuse an APK after application/build source changes. No GitHub or signing secrets used.

**Lab package:** `com.framebynavin.app.lab`, public development preview signing only. This is neither the stable owner's upgrade APK nor a production release. Physical user testing and Personal Voice long-idle P0 certification remain separate.

## Stage state

Authorized D8 verification activation; not yet claiming unit pass, lint pass, APK created, emulator pass, physical owner acceptance or stable promotion.

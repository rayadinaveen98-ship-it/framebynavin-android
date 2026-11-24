# Backlot Lab R2 S6 — Corrected signer and verified preview request (2026-11-24)

User asked whether the owner preview APK was ready and to investigate a failed gate.

## Confirmed source and QA evidence
- Frozen successful  Android15 debug-tested application tree: `379a3412ca84bf4fe70671fe14cec0bb56b0d316`.
- `:app:compileDebugKotlin` PASS, three focused unit classes PASS, two Android15 first-run navigation attempts PASS in earlier Stage5.
- `:app:compilePersonalStableKotlin` PASS and `:app:assemblePersonalStable` with **full vital Lint** PASS in run `45352849659`; APK was assembled but GitHub did NOT upload it because the certificate comparison failed in post-build validation.
- Exact mismatch mechanism from no-build run `45358691790`: apksigner on current tools returns `V2 Signer: certificate SHA-256 digest: f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0` while the previous verifier expected `Signer #1 certificate SHA-256 digest:`. Therefore it captured an EMPTY actual digest and falsely flagged `incompatible preview signer`.
- The existing signed Lab debug APK's actual certificate digest EQUALS the public prototype keystore's expected digest `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0`. This does NOT independently prove the unpublished personalStable artifact: its exact certificate must be verified on the newly built artifact.

## Corrective scope and release gate
- This branch is forked from EXACT same tested source commit. Only CI verifier code and documentation differ, no application code, permissions, backend/data or signing config changes.
- Parser accepts current `V2 Signer:` and previous `Signer #1:` format; source-only no-Gradle preflight verifies with an exact sample **before** any expensive build.
- Build once with proven two-phase strategy: separate Kotlin 8 GiB daemon for personalStable compilation; preserve compiled output and run full Lint assembly with 6 GiB Gradle worker.
- Verify: exact package `com.framebynavin.app.lab`, versionCode 337, label Backlot Lab, manifest nondebuggable, one signer, strict valid SHA-256 certificate matching checked-in prototype keystore, archive full artifact SHA256 and source commit. **No bypass** of signing or lint checks.
- Only publish verified owner-preview artifact after all checks pass. Never promote stable automatically; physical reminder/audio/privacy validation remains pending. No live Lab data changes.
- This rebuild is necessary because the prior verified owner artifact was never uploaded due a false post-build verifier rejection. No previous successful debug/unit/emulator test gates are rerun.

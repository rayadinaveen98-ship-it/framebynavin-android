# Backlot Phase 1 — owner visual decision and screenshot gate

**Recorded:** 2026-11-07
**Baseline:** Build 320, source f5694e0ba4c64775c5a3135f1a3f4a08c65ed05a
**Visual-review source:** feature/backlot-phase1-trust-review, commit 43014b7b49b5cda5db03ffddb80882ae97c37520

## Locked owner decision
- Owner chose **Option A — Cinema / Warm Minimal** for continued Android prototype work.
- This approves the **direction**, not any uninspected Android screenshot and **not** a production release.
- Existing screen reference hierarchy: Cloud Vault; one-activity 21-Day Challenge Setup; Challenge Progress; Full-Screen Alarm; Account Deletion.
- Use existing Backlot charcoal surfaces, amber-coral warmth, clear high-contrast hierarchy and reduced visual clutter.
- Do **not** independently revise icons, color language, motion, typography, or navigation in the name of polish. Show the owner real Android emulator screenshots before finalizing.

## Required screenshot evidence
- Existing debug-only native Compose review Activity: app/src/debug/java/com/framebynavin/app/phase1/Phase1VisualReviewActivity.kt.
- Android 15 pixel_6 runner: .github/workflows/backlot-phase1-android-visual-qa.yml.
- Screenshot scenes: cloud, challenge-create, challenge-dashboard, alarm, delete-account.
- Workflow run: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42357210138
- **At record time:** workflow still compiling; no screenshots inspected or approved, no owner APK released. Do not mark the review as passed until images can be opened, compared, and owner approval recorded.

## Separate nonvisual engineering
- Draft PR #316 adds the standalone Phase1HabitChallenge policy, separate from V297 legacy per-day plans.
- Eight Kotlin policy test cases passed in a local pure-JVM harness using temporary JUnit-compatible assertions; no Android device, Gradle, alarm, persistence, migration or integration acceptance has been claimed.
- No version bump and no merge into main, release or owner data.
- A real recurring Android alert mechanism, persistence, cloud coverage, idempotent occurrence delivery, and reboot/power tests are still missing and must not be implied by preview UI.

## Release gates
1. Android emulator build + five actual screenshot files and source hashes.
2. Manual comparison with approved Option A mockups and **explicit owner signoff**.
3. Real feature integration only after UI signoff; test legacy challenge preservation and no false Done receipts.
4. Device alarm/voice/FSI fallbacks and full opt-in cloud restore/delete E2E.
5. In-place signed Build 320 upgrade/data-integrity matrix and owner acceptance.
6. Only then consider an owner preview/release; production UI stays unchanged until approvals.

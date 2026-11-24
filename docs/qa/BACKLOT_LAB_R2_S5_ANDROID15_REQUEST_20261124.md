# Backlot Lab R2 S5 — Android 15 Regression Request

Date: 2026-11-24.
Founder: continue Stage 5 after successful focused unit testing.
Verified prerequisite: repaired Kotlin compile GitHub Actions run 45337219034 SUCCESS (source fence + compile).
Verified prerequisite: focused policy run 45340227725 SUCCESS (source fence + exactly three policy test classes).
Frozen application source: 379a3412ca84bf4fe70671fe14cec0bb56b0d316.
This request triggers only the preprepared workflow `.github/workflows/backlot-lab-r2-stabilization-s5-android15.yml`.
Gate 1: ensure exact compiled/repaired app tree and Lab package `com.framebynavin.app.lab`.
Gate 2: single serial build of disposable Lab debug APK and AndroidTest APK; archive hashes.
Gate 3: reuse that exact pair to run two fresh Android 15 emulator first-run journeys via `tools/qa/backlot_lab_r2_android15_navigation.sh`, no Gradle in emulator gate.
Refuse production package, cloud writes, merges, release signing and personal owner data.
On any failure, stop downstream and inspect the exact step before rerunning anything.
The emulator gate validates navigation, not full lock-screen/media/audio behaviors nor an owner-preview release.

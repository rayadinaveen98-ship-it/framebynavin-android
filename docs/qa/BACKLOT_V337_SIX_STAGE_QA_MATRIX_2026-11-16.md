# Backlot V337 / P0 — six-stage end-to-end QA execution plan and evidence matrix
Date: 2026-11-16. **Status: QA test implementation on isolated draft branch; execution and result verification pending.** No production/source feature change, merge, deletion, signing, APK release or owner-data modification authorized.

## Immutable target
- Frozen V337 owner-reference: `stable/backlot-v337-premiere-owner-testing-20261114` `678502f4626150ea1945b81eed1593a362377454`.
- Exact integrated P0 product SHA: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, draft PR #389.
- P0 full JVM + Android 15 notified challenge/Snooze passed GitHub Actions [43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050). Prior successful QA-signed artifact [43867609066](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43867609066), **NOT final release**.
- Source-level Stage 1–6 implementation already present in V337; past draft PR #377 is an **older version 336 ancestor**, not a set of unmerged features to replay. See PR #390 source audit.
- Device tests on disposable Android 15 emulator only; package is cleared per test class. Never run on a physical owner phone.

## Automated QA implementation (this branch; pending execution)
- New `P1SixStageQuickstartSceneDeviceTest` — 5 Android UI/asset tests:
  1. V393 first-run actually offers Creator, Personal, Both and a click returns the correct focus.
  2. V393 focus intent is synchronously persisted and survives new store instance; non-empty input state refusal, synthetic cleanup.
  3. V393 Creator quick-start does NOT enable save before three explicit fields; selected mode/platform/goal are saved into a complete profile.
  4. Exactly Spark/Mira selectable, legacy CUTE/NAVI names remain parsable, all six real Mira WebP assets decode.
  5. V391 Voice and Alarm scene picks change independently and original selections are restored after the synthetic test.
- New `P1SixStageWidgetProviderDeviceTest` — 3 Android tests:
  6. Manifest registers exactly the three V392 replacement providers, no old providers.
  7. Android AppWidgetManager actually discovers those three for the package.
  8. Each of the three real RemoteViews layouts inflates and exposes its touch target.
- Re-run relevant existing UI tests against same SHA: `V207FirstRunUiTest` (legacy setup compatibility), `V195ReminderSceneUiTest` (alarm/voice controls), `V159ReminderAccessibilityAcceptanceTest` (2x text essential actions), `V2215CompanionAccessibilityUiTest` (existing companion Settings/card accessibility), `V294FirstRunEmptyStateDeviceTest` (no synthetic data creation).
- CI compiles **every** AndroidTest Kotlin source but runs only this explicit regression subset. Do not represent this as execution of the entire instrumented suite.
- Fail closed for any setup/permission/instrumentation/signer/registration failure. On failure, retain actual failure and logs for diagnosis; do not remove failing tests or change production source to force a green run.

## Human E2E acceptance matrix (each row must be explicitly recorded)
| Track | Real behavior / expected result | CI proxy | Physical owner evidence |
| --- | --- | --- | --- |
| S1 voice cadence | Original 21-day voice plays full announcement, correct repeat count and pause; no early cutoff | JVM policy previously green; real audio NOT automated here | PENDING |
| S2 retiring widgets | Only 3 new widgets in picker; old pinned tiles migrate without data loss | Manifest/manager discovery | PENDING |
| S3 companion | Spark/Mira image selection persists across cold launch, notification and orientation; no locked-screen private text | Asset decode + existing accessibility UI | PENDING |
| S4 reminder scenes | Classic vs six Voice/Alarm scenes independently selectable; Done, Working, Snooze, Replay, Dismiss reachable; no unintentional TTS/audio change | V391 selection + existing V195/V159 UI | PENDING |
| S5 three widgets | Today Hub, Next Reminder, Challenge Pulse pin/resize/refresh correctly; never show Personal text, never mark Done via tap | Provider registration + layout inflation | PENDING |
| S6 onboarding | Fresh Creator, Personal, Both paths; Personal skips creator profile, Creator requires 3 fields; returning and partial legacy preserve original data | New quickstart composable tests + old V207 + fresh-store QA | PENDING |
| P0 reminder reliability | Real 10-minute Snooze, 1 notification, no forged Done; correct Alert Health state | **PASSED** on exact P0 SHA 815ba697, independently [CI 43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050) | Owner reported general “all good”; case-specific timestamps unrecorded |
| Premiere proof | Creator/Personal opt-in safe, callbacks unchanged; 21-day challenge **preview-only** | No new V394 full visual tests in this matrix | PENDING |
| 21-day voice + music | **Feature not implemented**; design and separate approval required | Not testable as parity | NOT IMPLEMENTED |
| Accessibility | TalkBack, 200% type, reduced motion, contrast, small/large screens, touch targets | Existing V159 + V2215 subset | PENDING |
| Data migration/permissions | Same owner package/signer, synthetic in-place 337 to future 338+; no destructive restore/backup UI, Advanced local erase retained; deny-permission and low-power state | Existing P0 isolated denied/Doze tests, not rerun on this branch | PENDING |
| Reliability | Long notifications, process death, reboot, voice focus, OS Doze, multiple simult alerts, low-end performance, tablets | Isolated P0 #381/#382, not full E2E | PENDING |

## Run sequence
1. Checkout exact P0 SHA and verify all tracked added/changed files are **only** AndroidTest Kotlin, CI Bash, GitHub Actions and QA documentation.
2. Full Android + AndroidTest APK compile, **without excluding any existing classes**.
3. On disposable Android 15 emulator, for each selected test class in named order:
   - install both debug and test packages; clear synthetic app state before class, grant synthetic notifications/exact alarm appop; pass `-e hardeningTestEnvironment ci-emulator` and `-e backlotVisualProfile default`.
   - use `am instrument -w -r -e class` to run the actual instrumentation test; require `OK (N tests)`, no `FAILURES!!!`.
   - archive class log and RESULT table; if any class fails, fail job, preserve artifacts.
4. Separate clean full-JVM run against same head (no test exclusions) and attach XML results.
5. Review unmodified screenshots / UI tree, any failures and differences with current product contract; do not assume "tests passed" means visual quality or physical OEM reliability.
6. Produce precise issues and ask founder before any production code change; no automatically upgraded version, signed APK, merge or release in this audit.
7. For future owner device test, record package/version/certificate, device & Android API, actual installed state, selected task data counts, screen on/off, due and observed time, expected/reported visual/audio/callback results. Keep owner data private.

## Restrictions
Never change `app/src/main/**`, `app/build.gradle.kts`, production code, data migrations, normal Settings navigation, widget providers, signed APK, developer roles or release branches as part of this audit. Freeze P0 source. A new QA-only test may fail; document it instead of treating it as permission to patch product code.

## Release gating
This is **P1 six-stage QA**, not production readiness. Founder release approval remains separate. Critical user-facing 21-day cinematic route, audio parity, full-screen lock screen, 3 launcher widgets, fresh installation, and accessibility do not get certified by an emulator smoke subset alone.

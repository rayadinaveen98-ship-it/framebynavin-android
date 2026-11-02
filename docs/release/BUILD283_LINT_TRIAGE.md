# Backlot Build 283 — Android Lint Triage (Release RC1)

**Source:** GitHub Actions run [41422459238](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41422459238), artifact `Backlot-Build283-Lint-Diagnostics` (ID 13133745643), `lint-results-debug.xml`, 2026-11-02.

**Result:** `lintDebug` completed successfully in 44m 1s. **0 errors, 307 warnings, 1 information.** A green Lint task is **not** a waiver for policy, security, or release-specific Lint.

## Counts by diagnostic

| Check | Count | Disposition |
|---|---:|---|
| UseKtx | 175 | P3 API modernization; avoid churn during RC1 stability |
| HardcodedText | 30 | P2 localization/accessibility: widget layout labels, include content descriptions |
| GradleDependency | 18 | P2 dependency/security audit (version recommendations are not automatically safe upgrades) |
| SmallSp | 18 | P2 accessible font scale review on-device |
| ApplySharedPref | 12 | **Triage by call site:** alarm ledger commits may deliberately require synchronous persistence; changing to `apply()` indiscriminately could break stale/duplicate alarm protection |
| UnusedResources | 12 | P3 cleanup, confirm referenced by resources/variants before removal |
| ObsoleteSdkInt | 11 | P3 remove redundant >= API 26 guards only with regression tests |
| UnusedAttribute | 9 | P3 compatibility review |
| InlinedApi | 6 | P2 review guarded saved-recording speech APIs on API 26–32 and 33+; verify real behavior |
| ModifierParameter | 4 | P3 Compose lint style |
| DefaultLocale | 3 | P2 user-facing formatting across locales |
| ImplicitSamInstance | 2 | P3 allocation/performance |
| CredentialManagerSignInWithGoogle | 2 | P2 inspect Google ID token handling in CloudSyncActivity and V23AccountOnboarding; both call `GoogleIdTokenCredential.createFrom`; likely lint heuristic, but verify sign-in on-device |
| MutableCollectionMutableState | 1 | P2 Quick Create production-styles state in V101BReminderUi; verify UI mutation/rotation and save/restore |
| DataExtractionRules | 1 | P1 privacy audit: manifest `allowBackup=false`; inspect Android 12+ extraction behavior and explicit extraction rules, avoiding accidental local/private backup |
| AutoboxingStateCreation | 1 | P3 Compose state efficiency |
| NestedWeights | 1 | P3 UI layout |
| UseOfNonLambdaOffsetOverload | 1 | P3 Compose layout |
| SetTextI18n | 1 | P2 localization |
| **Total** | **308** | **0 fatal/error reports** |

## Action and guardrails

1. **Do not autofix `ApplySharedPref` in AlarmLedger.** The file is explicitly a synchronous, cold-start delivery ledger used to reject stale/duplicate alarms; review data durability and threading first.
2. Highest priority: inspect manifest backup/extraction declarations and verify no private Personal record is OS-auto-backed up contrary to the privacy promises. Test Android 12–16 device backup behavior.
3. User-facing P2: font scale 200%, screen reader, widget labels, locale-sensitive formatting. Address controlled issues once owner APK verification and data preservation gates pass.
4. Release AAB must pass its own Lint/Play policy checks. Preview-only `-x :app:lintVitalPersonalStable` in workflow is **strictly for the public-debug-key sideload APK**, not a release exception.
5. Every code change to a flagged area requires focused unit/device test and audit trail. **No broad dependency update or style-only rewrite in release freeze.**

Do not declare all 307 warnings resolved. This file classifies them and preserves explicit pending work.

# Backlot 1.0 Recovery — executable QA release-gate plan (not activated)

Date: 2026-11-22
Status: **PLANNED AND SOURCE STAGED; NO COMPILATION, CI, INSTRUMENTATION, APK OR PHYSICAL TEST HAS RUN ON THIS RECOVERY BRANCH.**
Owner contract: "continue" continues source work, **does not authorize test/build**. Wait for explicit "done bro start next steps and give apk" or equivalent.

## Frozen-source decision and test provenance

- Initial owner-reported broken APK source: `0971dc6cfa7750726f33f08cdc08cedac2ffd485`.
- Recovery branch: `feature/backlot-v1-recovery-onboarding-reminder-looks-20261122`. Record exact HEAD when authorizing; any source change thereafter invalidates only affected verification results.
- Stable branch `main` not to change. No hosted UX09, Google/Supabase credentials or databases, backup/restore, old-device data, Play rollout or production signing changes.
- VersionName should remain `1.0`, label `Backlot`. Owner-preview internal versionCode must exceed already-installed 338 (if installing over that build). Must verify exact same package and compatible signing certificate before upgrading.
- Use `-PbacklotLab=true` only for the disposable CI Lab package `com.framebynavin.app.lab`. The owner preview uses `com.framebynavin.app` and is *never* tested with destructive fresh-app automation.
- The old APK's green compilation/JVM/lint/Android15 startup result belongs to a different source SHA and **cannot certify this branch**.

## Stage A — source identity and focused compile

Source checks: main unchanged; app source only intended recovery edits; absence of new cloud/SQL/secrets/hosted changes; no rejected Classic/Cinematic/Premiere Settings selector; reminder delivery/receipts/alarms unchanged.

On an isolated GitHub runner using JDK 17 / Gradle 8.11.1, compile `:app:compileDebugKotlin -PbacklotLab=true` **once** with durable stdout/stderr artifacts, timeout, and source SHA. Compile Android test sources `:app:assembleDebugAndroidTest` after main compilation succeeds. **No full JVM suite at this step.**

Failure branch: if a missing import, overload, source compatibility, Kotlin or Compose API error occurs, patch **only** the reported file(s). Rerun `compileDebugKotlin` or the affected Android-test compilation; preserve other passing evidence. Do not launch the remaining gates while red.

## Stage B — targeted JVM contracts

Run first:
- `com.framebynavin.app.ui.BacklotV1RecoveryRoutePolicyTest` — account gate vs opt-in Creator setup, including partly-restored complete profile.
- Relevant existing `V149CreatorOnboardingPolicyTest`, `UX11IntegrationPolicyTest` and reminder visual-policy unit tests, without rerunning unrelated tests for each small correction.
- Validate account state `completeAccountEntry` atomicity and failure behavior with disposable local preferences/fault injection once a deterministic fixture exists. Source callback alone is not that proof.

Failure branch: fix root cause; rerun failing test class only; do not reflexively rerun all JVM tests. A final cumulative suite can follow once source is frozen and specific targeted gates are green, if release certification requires it.

## Stage C — REAL Android navigation on disposable API35 emulator

Runner preflight **before any application Activity launches**:
- Confirm `/dev/kvm` usable; ensure Android 15 emulator online and boot complete.
- Install Lab APK and Android-test APK; verify Lab package `com.framebynavin.app.lab`, SHA256 and public test signer.
- Verify emulated hardware (`ranchu`/`goldfish`) and supply instrumentation argument `hardeningTestEnvironment=ci-emulator`.
- Fresh-install UI test must start from a clean **disposable Lab package**. Never clear, uninstall, reset or attach physical owner's `com.framebynavin.app` installation.
- Run only the approved classes first, using `-e class`: `com.framebynavin.app.ui.BacklotV1RecoveryNavigationJourneyUiTest`, `com.framebynavin.app.ui.BacklotV1RecoveryAccountUiTest`, and `com.framebynavin.app.reminders.BacklotV1RecoveryAppearanceUiTest`.

Real navigation coverage: selected Welcome → Local account → Home; Ideas and Projects and Insights tabs → Home; explicitly open original Creator wizard → NOT NOW → Home; Home overflow → Settings → Appearance & sound → modern Reminder Appearance → Back → Home; Personal Reminders → Back, Creator Reminders → Back; recreate Activity → Home without repeated account gate, forced Creator wizard, or welcome reanimation. Capture screenshot/UI hierarchy and failed step name.

UI tests must target real semantics. Existing review corrected an invented Ideas subtitle assertion: use `backlot-ideas-screen` tag instead. If any test fails, first distinguish **wrong UI selector or test fixture** from **real app bug**; never alter production code to satisfy a mistaken assertion.

Appearance coverage: Classic/Cinematic collection and older Looks are absent. Each modern domain has Entrance and Main preview; Main labels are Studio Nocturne, Velvet Sanctuary and Ember Command. Gallery/background media remains **Entrance-only**. No preview may call Done/Snooze, create a response, schedule a reminder or upload a photo.

## Stage D — reminders, security and returning accounts

After the local navigation gate is green, on synthetic identities and disposable data:
- Returning Google account: credential UI and account state, successful local-save, post-process-recreation Home; failed local write returns actionable retry instead of permanent spinner.
- New Google account: remote claim succeeds once, local finalization fails via controlled fixture, retry **must not** claim username again; Google cancellation and unconfigured client ID remain recoverable.
- Existing user cloud restore/conflict is explicitly reviewed, never silently overwritten. Hosted network access must remain separate owner-approved gate; no destructive QA with real user records.
- Creator Voice/Alarm, Personal Voice/Alarm, 21-Day alarms: existing occurrence tokens and locked/invalid state gates; validated double tap → modern main, correct snooze/Done or challenge actions, audio/voice/music, stale token failure and notification-only fallback. Long-idle/overnight OEM reliability remains a physical-device test, not certified by emulator.
- Screen sizes, large text, TalkBack, dark mode, reduced motion and power saving inspected; track any remaining visual differences.

## Stage E — owner preview and physical acceptance (separately gated)

ONLY after prior checks pass: build and sign the same-package owner preview, verify label/versionCode/1.0/signer/SHA, package one clearly named artifact and preserve exact build SHA + tests/reports. An installability check must not recommend uninstalling existing Backlot or deleting saved data to work around mismatched signing.

The founder installs on an appropriate test device and confirms first-run, Google sign-in, touch destinations, notifications, sounds, 21-Day screen and long-idle reminders. **Owner sign-off is required** before proposing stable branch promotion or release.

## Incremental QA evidence ledger format

Maintain one row per gate with: `gate name | source SHA | run URL | job ID | status | checks actually executed | artifact/digest | failing assertion/stack line | fix commit | next invalidated gate`. A green parent job never silently overwrites a previously red historical run. Preserve independent passing artifacts when source/build inputs are byte-for-byte unchanged.

**STOP at any red gate** and correct only the affected source or test. Do not start independent downstream jobs that depend on it. No recurring or automatic CI runs during source staging.

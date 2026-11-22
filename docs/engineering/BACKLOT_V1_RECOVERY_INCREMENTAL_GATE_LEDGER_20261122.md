# Backlot 1.0 recovery — incremental evidence ledger
Date: 2026-11-22
Branch: `feature/backlot-v1-recovery-onboarding-reminder-looks-20261122`
Frozen app+androidTest source: `8b771a5287b92d33cc8341907fc7747447cfd29a`
Protected main at review: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`

## Founder-approved no-waste CI policy
Source is fixed on the isolated branch. Fail at gate N => inspect exact logs, correct only affected source and rerun **only invalidated checks**, retaining a prior passing artifact only when app/test/build inputs are unchanged by SHA. Do not call a completed CI job an app release, or a synthetic Google test a real sign-in. No stable merge, hosted UX09/cloud mutation or owner-data reset.

## Stage evidence
| Stage | Exact evidence | Status | Next condition |
|---|---|---|---|
| A Kotlin source compile | `45023567967` — source identity + `:app:compileDebugKotlin` successful; build in 24m 38s; SHA `8b771a52` | **PASS** | Freeze app source and proceed to B |
| B focused JVM + AndroidTest APK compile | `45026564764`; triggered by `BACKLOT_RECOVERY_START_STAGE_B.txt` at commit `f17fced80cf07ebb81d124f7b2181305f9807187` | **RUNNING / result pending at record** | Must be green and provide verified Lab app + androidTest APK artifact |
| C Android15 root UI | `.github/workflows/backlot-v1-recovery-stage-c-emulator-20261122.yml` staged, no trigger file | **NOT RUN** | Only start after B green; reusable Stage B artifacts, prove KVM; real root navigation first-run+account+appearance UI tests on disposable `com.framebynavin.app.lab`; save screenshot/log evidence |
| D reminder lifecycle policies + lint | `.github/workflows/backlot-v1-recovery-stage-d-reminder-regression-20261122.yml` staged, no trigger file | **NOT RUN** | Only start after C; focused tests + lint on same frozen source, not full JVM rerun |
| E owner APK build+signer | `.github/workflows/backlot-v1-recovery-stage-e-owner-preview-20261122.yml` staged, no trigger file | **NOT RUN** | Must prove run success A/B/C/D before building; verify `com.framebynavin.app`, versionName `1.0`, versionCode `338`, app label `Backlot`, public prototype signer, SHA-256; never Play release |

## Recovery acceptance invariants
- Original `Make Backlot yours.` three-way Creator/Personal/Both screen appears for genuine new users only, saves choice once, does not auto reappear on Home or on any navigation.
- New local continuation and saved/returning account routes must reach Home; Personal-first may open the existing Personal hub. Original five-page Creator setup is optional, dismissible, and does not intercept tabs or reminder hubs.
- Rejected Classic/Cinematic collections cannot appear from normal Reminder Appearance. Contemporary entrance and MAIN previews for all domains; no lost owner-validated Done/Snooze/callback state.
- UI tests are synthetic and local-only; real Google/new+returning session, OEM long-idle audio/scheduling reliability, physical tablet/phone accessibility, and all actual audio responses remain **separately unverified** until observed.
- Signing and APK build are separate from stable promotion. Founder physical device acceptance is required before any stable/release decision.

## Snapshot note
This ledger records the evidence known when written; GitHub Actions is the live source of truth for subsequent stage status, and new results must be appended with run IDs rather than silently revising historical failures.

## Incremental recovery checkpoint (2026-11-22)

- **Stage A PASS:** run `45023567967`, compile-only frozen application SHA `8b771a52`.
- **Stage B1 PASS:** job `137220769856` of run `45026564764` passed targeted JVM assertions. Entire run remained red because AndroidTest compilation subsequently failed; do not misreport whole Stage B as passed.
- **Stage B AndroidTest failure diagnosed:** job `137226886901`, `compileDebugAndroidTestKotlin`, four invalid top-level imports of `assertExists`/`assertDoesNotExist` in `BacklotV1RecoveryAccountUiTest.kt` and `BacklotV1RecoveryNavigationJourneyUiTest.kt`. Android debug app assembled successfully; warnings about Play Services D8 stack-map were not the compiler blocker.
- **Only two test files modified:** removed exactly four invalid imports, no production application source edits. The initial Stage B2 workflow `45032527848` was invalid YAML (no test jobs started), corrected YAML only.
- **Stage B2 PASS:** `45032575621`, job `137239172032`, prior green Lab app retained with SHA256, changed Android instrumentation test APK rebuilt. Artifact `14759176751` named `Backlot-V1-RECOVERY-STAGE-B2-TEST-FIX-AND-LAB-APKS`.
- **Stage C launched:** `45034910254` uses pinned B2 binaries on disposable Android 15 KVM, root UI suite; not certified until run and individual test evidence are green.
- Stage D and E workflow integrity gates updated to permit the exact two AndroidTest-only import changes and verify previous green focused JVM/B2 results without repeating earlier full JVM compilation.
- Protected `main` stayed at `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` when checked. No owner signed APK, hosted data modification or stable merge in this checkpoint.

## 2026-11-22 — Emulator gate certified; targeted reliability launched

- **Stage C PASS: 45034910254.** Disposable Android 15 KVM emulator, `com.framebynavin.app.lab`, run on immutable Stage B2 Lab app and new test APK. All seven instrumentation tests reported `OK`: four account/retry tests, two reminder appearance tests and one first-run navigation test. SHA-256 and emulator identity verified; screenshots and logs preserved in artifact `14759675618`. This is actual emulator evidence, not physical-device certification or live Google authentication.
- **Stage D started: 45035559601.** Only seven specific reminder and voice JVM policy classes and lint; no full JVM suite reset.
- Production application and JVM source remain byte-for-byte identical to frozen `8b771a5287b92d33cc8341907fc7747447cfd29a`. Only four invalid Compose assertion imports removed in two Android instrumentation test files.
- Stage E remains untriggered until Stage D is green. Owner-preview `com.framebynavin.app` signed APK has not been created. Keep all cloud and stable services untouched.

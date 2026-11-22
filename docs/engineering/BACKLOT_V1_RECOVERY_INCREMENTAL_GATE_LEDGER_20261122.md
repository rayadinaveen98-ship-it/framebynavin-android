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

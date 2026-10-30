# Backlot RC9 — stacked PR provenance and integration safety audit

**Date:** 2026-10-29. **Type:** Engineering review and read-only CI audit only; **no** app/source/manifest/Gradle/APK migration changes, merge, tag, Play upload, signing credentials, or user data operations.

## Verified frozen baseline

| Baseline | Evidence |
| --- | --- |
| Build267 RC6 application source | `5b0682140ae19ec698c0c84aa2be8a9939844500`, versionCode 267, package `com.framebynavin.app` |
| Full owner preview artifact | `Backlot-RC6-Build267-Full-App.apk`, SHA256 `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d` |
| Android 11/15 tests and same-signer 266→267 upgrades | [RC6 five-job success](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40922583197) |
| Android 16 tests, genuine 266→267 upgrade, unsigned bundle | [RC7 five-job success](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40926157504) |
| Different-signer negative test on disposable Android 16 | [RC8 two-job success](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40927510200) |
| Frozen predecessor for **RC9 documentation-only** changes | `66db640c6fcc3fee6dbfe0690d1e01750b85cf7e` (RC8) |

## GitHub integration graph (read-only audit snapshot)

- `main` SHA `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`; GitHub reported **`protected=false`** on 29 October 2026, and no repository rulesets were returned. **Do not merge to an unprotected main.**
- **161** open PRs were returned from the complete first two API pages. The current release path consists of **34 stacked draft PRs** following head→base names from [PR #183](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/183) back to [PR #148](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/148), whose base is `main`. Other open PRs must not be silently equated with this release chain.
- `main...`RC8 has **1,267 commits ahead and 0 behind** at this snapshot; GitHub compare API returned the first 300 filenames only. A capped compare is **not a full repository file-diff review**. RC9 workflow performs a local complete `git diff --name-status origin/main...HEAD` inventory and reports counts and security-sensitive paths.
- Later PRs in a stacked series can be forked from an earlier PR **before** the earlier PR received subsequent fixes. GitHub's current PR base SHA can differ from the current branch-head SHA. Three such discrepancies occurred, below.

### Three historical branch-head discrepancies — explicit reconciliation

| Parent PR | Child PR | Late commits after child fork | Reconciliation, checked against RC8 tip |
| --- | --- | --- | --- |
| [#167](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/167) | [#168](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/168) | 1, removing invalid Compose test import | `V2284StageCelebrationUiTest.kt` is byte-for-byte identical to the parent PR head; late correction is already represented |
| [#164](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/164) | [#165](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/165) | 2, including Settings Spark screenshot stabilization/imports | `V217LiveGuideScreensUiTest.kt` is byte-for-byte identical to the parent PR head; late correction is already represented |
| [#162](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/162) | [#163](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/163) | 5, including Personal editor safe/IME overlay and QA changes | Later current app code **does include** `CreatorAdaptiveFullScreen(onDismissRequest = onDismiss)`, and `CreatorAdaptiveFullScreen.kt` owns `WindowInsets.safeDrawing` and `imePadding()`. Current Android UI test uses `captureToImage().asAndroidBitmap()`. **However** old `backlot-v2272-personal-wave-qa.yml` and current workflow are not byte-identical. Keep the historical CI difference recorded; do not blindly replay/merge its old branch. Current relevant Android11/15/16 device tests are green. |

**Meaning:** All 34 current draft PRs make a navigable branch graph, but **not every parent branch's latest commit is part of the newest branch ancestry**. A full sequential merge could unexpectedly introduce old historical commits, code or workflows and thereby invalidate the already-tested Build267 app. The integration plan should use an immutable reviewed snapshot and audit each non-ancestral correction, rather than clicking “merge all” on the old PRs.

## What RC9 proves, and what it cannot prove

RC9 CI must:

1. Confirm the RC8 frozen predecessor and that **only this document and the RC9 QA workflow** have changed. Build267 source, version, APK and signed artifact are unchanged; no new Build268.
2. Fetch **all** open PR pages via read-only GitHub API; walk the actual `head.ref → base.ref` chain; preserve current PR numbers/base/head SHAs in the artifact; flag missing or unexpected links, merged, non-draft, or altered root.
3. Compare each child PR's base commit to its parent's *current head*. Enumerate discrepancies. Require exactly the three **known, separately reconciled** discrepancies until manually reviewed.
4. Verify parent-head source blobs match the current release tree for #164 and #167. For #162, explicitly check the current Activity-owned Personal editor, safe/IME insets, Compose pixel test, and retain old workflow-file discrepancy as a review item.
5. Fetch GitHub's `main` protection state, and compute a complete Git locally tracked file-inventory summary. Mark `merge_ready=false` even when this audit succeeds, because protected-main configuration, physical-device user approval, signing migration, Play permissions/credentials and coordinated PR review are pending.
6. Write machine-readable `rc9-pr-chain-audit.json` and `rc9-repository-file-inventory.json` artifacts. **No branch edits, merges, APK builds, file deletion or permission changes.**

CI passing is **audit evidence**, not permission to merge. If GitHub's latest PR graph or main SHA changes, the audit should be reviewed and refreshed rather than silently skipping it.

## Integration gate before any main merge — BLOCKED

- [ ] Owner confirms Build267 physical-phone UI, alarm/voice, background restrictions, cold start, Creator and Personal data, clean backup exports and external media transfer.
- [ ] Repository administrator configures a **main branch ruleset/protection**: require pull request and approval, required named checks, restrict forced pushes and branch deletion, and configure bypass permissions. Verify with GET rather than assuming it succeeded.
- [ ] Produce an **immutable release candidate** from tested Build267 app source, with every non-ancestral fix reconciled. Verify complete source/tree manifest, not capped 300-file API diff.
- [ ] Review 34 release-chain PR histories and the 127 other open PRs to decide which are superseded/active; do not close or merge anything automatically.
- [ ] Formalize one explicit, reviewed integration path. Do **not** sequentially merge stale stacked PRs or bypass checks. Keep `main` unchanged until this review is approved.
- [ ] Confirm Play signing/certificate strategy and safe export/restore of separate **Creator** and unencrypted **Personal** backups, plus original project media, per [#180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180). A production key must remain private and distinct from the public prototype signing key.
- [ ] Play permissions, Data Safety/privacy policy, content/store listing, exact alarm/full-screen/media foreground declarations, internal track, accessibility and staged rollout approved separately.

Track release gate [#174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174) and signer migration [#180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180). RC9 is intentionally **draft**, with no merge or production submission.

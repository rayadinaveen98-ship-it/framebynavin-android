# Backlot V337 — founder-promoted personal stable acceptance
**Date:** 2026-11-16  
**Status:** **STABLE FOR OWNER / CONTROLLED PERSONAL TESTING ONLY. NOT A GOOGLE PLAY PRODUCTION RELEASE.**

## Founder decision and scope
Founder explicitly directed: "promote this as a stable version and once done update me with our roadmap whether are we ready or not to publish." The owner reported the previously installed P0 physical-testing build was good. This authorizes recording an owner-accepted personal stability reference, **not** claiming complete physical device certification or publishing it to Google Play.

## Stable source / immutable prior reference
- **Promoted owner-stable snapshot branch:** [`stable/backlot-v337-p0-owner-accepted-20261116`](https://github.com/rayadinaveen98-ship-it/framebynavin-android/tree/stable/backlot-v337-p0-owner-accepted-20261116) at **exact product commit** `815ba69775f75b6e5f25bb838c2c17b4f09c203e`.
- **Previously frozen V337 owner reference retained without update:** [`stable/backlot-v337-premiere-owner-testing-20261114`](https://github.com/rayadinaveen98-ship-it/framebynavin-android/tree/stable/backlot-v337-premiere-owner-testing-20261114) at `678502f4626150ea1945b81eed1593a362377454`.
- **P0 integration draft PR #389** source commit remains `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; its branch and PR remain unmerged; owner-stable promotion does not merge to `main` or change the old source reference. Repository `main` remains historical `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`.
- **QA-only PR #391** `9ad7109197dcf737509874bda73fc645a389b2b9` changes Android tests, CI and QA documentation, **not production code**, so its 17 selected Android 15 tests validate unchanged product implementation at `815ba697`.
- This stable branch is a GitHub **reference, not branch-protection enforcement**. The branch is currently not protected; enabling protections is a separate repository-admin action before a public release.

## Exact tested binary identity (same files checked, not rebuilt)
- APK: `Backlot-V337-P0-Physical-Testing-ONLY.apk`, **19,667,094 bytes**, SHA-256 **`94fc7c0c9f9718378456571f439eea759824fc61ac990d43ff0b2b191a3309b2`**.
- Original downloaded artifact ZIP: `Backlot-V337-P0-Physical-Testing-APK.zip` SHA-256 `f7d9017f52a562dd63e1a11990a39a76f9e8d595dbb3bcef2c9fdaf0a56fd9a7`; its internal `SHA256SUMS.txt` confirms the exact APK SHA-256 above.
- QA artifact [GitHub Actions #43867609066 / artifact #14228797432](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43867609066/artifacts/14228797432), packaging source `3706a42d518618958f52c2d2d67936f947e45916`; artifact build metadata states its **verified product source** was `815ba69775f75b6e5f25bb838c2c17b4f09c203e` and only packaging workflow metadata differed.
- APK metadata: `com.framebynavin.app`; `versionCode=337`; `versionName=2.28.7-premiere-cinematic-enhanced-02`; `minSdk=26`, `targetSdk=36`.
- **Prototype-only public preview signer SHA-256 certificate:** `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0`. Verified V2 APK signature. **Public signing identity is not appropriate for Play production.**
- Do not uninstall or clear the current Backlot app on the owner device to change signing keys or chase installation errors. Android system auto-backup is disabled.

## Engineering evidence
- [Combined P0 CI #43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050) completed successfully at exact product SHA `815ba697`: full JVM tests, **all instrumented test sources compiled**, and Android 15 actual 10-minute Challenge Snooze + missing notification channel acceptance. All instrumented tests were **not** executed.
- [Six-stage QA #43909957313](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43909957313) completed successfully at QA head `9ad7109`; source-integrity passed with unchanged P0 production code; full JVM suite passed; **17/17 selected Android 15 instrumentation tests** passed across seven classes. Older stale test labels (Reschedule and Appearance & sound) were corrected in tests, not product code.
- Owner physically accepted the preview in conversation, and then explicitly promoted it to stable. Exact device/OS, lockscreen/TTS/music, launcher pin/resize, first-install variants, tablet, high-font TalkBack, OEM Doze, real-data upgrade and analytics were **not logged as individually certified**.

## Scope and stability limits
- The source contains Stage 1–6 work: 21-day voice pacing, removal of legacy widgets, Spark/Mira, V391 scenes, three V392 widgets, first-run Creator/Personal/Both flows. Existence in source and green emulator tests are **not** release-quality visual design acceptance.
- V394 Premiere is an opt-in experimental Creator/Personal experience; Challenge Premiere is **preview-only**, not a functioning automatic Challenge screen. Challenge voice+music parity is **not implemented**. Avoid misrepresenting demonstrations as live behavior.
- Owner's newer Settings/product feedback (restructure Appearance & sound, two themes Director's Cut + Light, remove redundant Reminder scenes route, drop Premiere/full-screen experiment, more app icons, Google sign-in egress failure) must be reconciled in a separately approved scope; owner-stable snapshot does not assert these requests implemented.
- No production signing credentials, Play track, hosted deletion service, account deletion URL, live privacy policy, Data Safety declaration, release AAB, staging backend migration, or Play listing was created by this promotion.

**Release decision:** owner-stable branch and existing QA APK are retained; Play publication **BLOCKED**. See the linked Play-readiness roadmap report. No PR merges, production source changes, public release, deployment, billing or destructive actions have been performed.

# Backlot V338 recovery — REJECTED by founder

Decision date: 2026-11-22
Decision: ABANDON / DO NOT USE AS BASE / DO NOT INSTALL AS STABLE / DO NOT CONTINUE OR BUILD.

Founder physically reviewed the Backlot 1.0 owner-preview APK and rejected it because the visible experience still resembled the previously rejected version and did not satisfy first-run and UI expectations. CI success was insufficient evidence.

Rejected branch: `feature/backlot-v1-recovery-onboarding-reminder-looks-20261122`
Rejected owner-preview run: `45040978231`; artifact `14762664414`; `versionCode 338`, `versionName 1.0`, package `com.framebynavin.app`.
Do not merge any commits, cherry-pick design work, resume staged CI, or promote this APK to stable. The historic branch and artifact may remain as read-only audit evidence, not accepted source. No production data or stable code changes authorized by this rejection.

Verified latest V337 source baseline at decision:
`main` and `stable/backlot-ux01-08-ux10-owner-accepted-20261120` both point to `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`.
Original V337 P0 owner-accepted reference `stable/backlot-v337-p0-owner-accepted-20261116` points to `815ba69775f75b6e5f25bb838c2c17b4f09c203e`.
Default owner package `com.framebynavin.app`, `versionCode 337`, `versionName 2.28.7-premiere-cinematic-enhanced-02`. Supabase Lab builds are separate package `com.framebynavin.app.lab`.

No fresh implementation, automatic tests, signing, CI, destructive deletion, reset, branch promotion or release until founder approves a new scope and chooses the accepted stable source. Preserve owner-installed data and all signers/credentials. Android downgrade from 338 to 337 is not a normal safe in-place upgrade.

Why CI-green V338 failed founder UX: the seven UI tests covered narrow fresh disposable-Lab navigation, account retry and appearance contracts; not the actual existing-owner upgrade, complete Welcome-to-Home and every-screen experience. Restoring one composable did not establish a visually new integrated application. A new plan must audit every screen and route, baseline against exact V337 source, and validate on actual owner device before stable promotion.

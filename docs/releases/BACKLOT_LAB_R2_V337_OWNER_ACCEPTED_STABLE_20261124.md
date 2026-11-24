# Backlot Lab R2 — V337 Owner-Accepted Stable / Main Promotion

Date: 2026-11-24. Status: OWNER-ACCEPTED LAB BASELINE; NOT PLAY/PRODUCTION RELEASE.

## Owner decision
Founder physically installed the signed R2 Stage7 Firebase-fixed owner-preview APK, reported all tested flows good, explicitly accepted this version and approved promotion to stable and main. The founder also reported the automatic guided tour does not appear for new users and deferred that issue. The manually replayable **Settings > About > Show app tour** route remains in source.

## Exact installed artifact (source of truth)
- Verified signed preview pipeline: `45369843748` (all jobs passed).
- Owner-ready signed runtime artifact: `14912885220`, `Backlot-Lab-R2-S7-Firebase-Fixed-SIGNED-RUNTIME-PASSED-Owner-APK`.
- Artifact URL: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45369843748/artifacts/14912885220
- APK inside ZIP: `Backlot-Lab-R2-S7-Firebase-Fixed-Owner-Preview.apk`.
- Lab package: `com.framebynavin.app.lab`, `versionCode=337`.
- Source SHA of EXACT tested app tree: `28a7000159554f254ab1ecc1aecc7792a1385ac2`.
- Established **public prototype** signer certificate SHA256: `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0` (NOT a production Play signing key).
- CI verified single-file Firebase startup guard and exact source fence, Kotlin personalStable compilation, full Android vital Lint, signed APK hash, package/manifest and non-debuggable identity. Exact signed APK survived fresh launch, force-stop restart and in-place upgrade on disposable Android15 emulator; owner separately confirmed installation/normal app navigation.
- Earlier source policy tests and debug UI navigation passed; physical 18-condition reminder/audio/lockscreen matrix and two-device sync are NOT wholly certified by this owner acceptance.

## Git promotion
- Previous default `main` commit: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`.
- Pre-promotion archive: `archive/backlot-main-pre-lab-r2-accepted-20261124`, preserving above commit.
- Accepted stable immutable branch: `stable/backlot-lab-r2-v337-owner-accepted-20261124` at `28a7000159554f254ab1ecc1aecc7792a1385ac2`.
- Repository `main` was fast-forwarded (NO force) from the exact previous SHA to the accepted source SHA. Subsequent docs-only commits on main do not change signed app tree.
- Earlier `stable/backlot-lab-v337-owner-tested-20261117` and `stable/backlot-ux01-08-ux10-owner-accepted-20261120` remain recoverable. No stable branch was deleted or mutated.

## Known issue (deferred)
**Guided tour missing automatically for first-time users.** Settings > About > Show app tour is present; auto-start/eligibility should be separately investigated (guest/login/returning state and true fresh-install). Creator setup is not automatically assumed missing; distinguish the five-step Creator setup from the six-lesson app tour.

## Next product direction (FOUNDER-APPROVED PRIORITY, NOT IMPLEMENTATION AUTHORIZATION)
Creator-first OS is the identity and product priority. Content Workspaces and the idea → plan → script → produce → publish → learn loop are the heart. Personal and Reminders are supporting features for creator wellbeing and execution, never the top-level app story. Inventory and prioritization are documented separately; no new feature source edits, migrations, paid services or releases approved by this decision alone.

## Safety and release boundaries
- DO NOT equate `main` source promotion with a Google Play production deployment, production package validation, or database migration. Default production package uses a different identity and has NOT received the same owner Lab acceptance proof.
- No cloud data deletion, account migration, remote writes or signing key rotation occurred in this promotion.
- Future owner APKs must checkpoint built bytes BEFORE independent signature and runtime verification; do not distribute debug-only-tested packages.

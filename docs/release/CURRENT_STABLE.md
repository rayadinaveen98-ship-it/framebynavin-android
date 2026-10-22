# Backlot current stable

The stable personal-install baseline is **V183 / 2.3.3-stable** (`versionCode` 183).

- Repository main checkpoint: `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`.
- Main-head CI: [39129657906](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39129657906), completed successfully.
- Scope: all V151–V182 features, non-debuggable personal-install packaging, same public prototype certificate as previous previews.
- Detailed provenance and limits: [V183 release record](../qa/V183_STABLE_PERSONAL_RELEASE.md).
- The creator reported the V182 functional baseline working on their phone. CI does not establish an exact V183 physical-device upgrade check.
- This is a personal sideload baseline, not a production/Play release. Preserve its certificate/package for compatible preview upgrades.

## Current development (not promoted to stable)

- **V208 / 2.8.2-v208-setup-backup-preview / versionCode 232** is the latest verified personal-preview candidate on `feature/backlot-v208-restore-setup-backup-validation`, at commit `05a082d73696892764e77fa12edf3df07985d582`.
- [V208 candidate PR #87](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/87) remains a draft. [Exact-commit CI run #39750705638](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39750705638) succeeded for build and emulator tests. The creator reports their V208 personal installation working.
- That report does **not** prove independent new-install testing across manufacturers, store-policy compliance, production signing, encrypted portable backups or complete personal-data export. These are separate gates.
- **V209 P1** is stacked on that exact V208 SHA solely for repository/release trust hygiene. It does not change the Android package, update versionCode, create another signed APK or claim production readiness.
- V184–V207 are historical development stages and must not be labeled as the latest candidate.

Changing a development branch, successful CI, or an informal device smoke test must never silently promote a preview to stable. Keep the existing personal-install signing/package path for current users. Do not uninstall a populated app or replace its signing certificate as a migration procedure.

Historical rollback reference: [V149 checkpoint](HISTORICAL_V149_STABLE.md). Preserve historical branches and records; start new implementation from current verified main.

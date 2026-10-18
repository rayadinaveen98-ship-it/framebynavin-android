# Backlot current stable

The stable personal-install baseline is **V183 / 2.3.3-stable** (`versionCode` 183).

- Repository main checkpoint: `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`.
- Main-head CI: [39129657906](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39129657906), completed successfully.
- Scope: all V151–V182 features, non-debuggable personal-install packaging, same public prototype certificate as previous previews.
- Detailed provenance and limits: [V183 release record](../qa/V183_STABLE_PERSONAL_RELEASE.md).
- The creator reported the V182 functional baseline working on their phone. CI does not establish an exact V183 physical-device upgrade check.
- This is a personal sideload baseline, not a production/Play release. Preserve its certificate/package for compatible preview upgrades.

V184 is a development preview for Phase 0 and the first Phase 1A capture protections. It is **not promoted to stable** by changing the development branch version. Each candidate must pass its own checks and device acceptance.

Historical rollback reference: [V149 checkpoint](HISTORICAL_V149_STABLE.md). Preserve historical branches and records; start new implementation from current verified main.

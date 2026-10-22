# Backlot current personal stable and active candidate

## Latest frozen personal stable: V195.5

The latest preserved personal-sideload stable **branch** is `stable/backlot-v195-5`,
currently at `957bfe0f17ca26e695371cd38f4a2bd29dea121e`.
Its release version is **2.5.3-v195.5-stable** (`versionCode 203`),
with package `com.framebynavin.app` and the intentionally public personal-preview
signing certificate. This is not a Google Play / privately signed production release.

- [V195.5 stable release record](../releases/BACKLOT_V195_5_STABLE.md)
- [Reference accepted V195.5 CI](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39313436911)
- Accepted source SHA recorded in the release record: `ba1d0ce0cdd53b21003ee6de31cf5877ff88e99b`
- Branch head also contains stable-only release metadata/verification.
- Preserve this branch and its compatible signing identity as the tested rollback reference.

## Historical main-line personal stable: V183

`main` and earlier documentation retain the historical **V183 / 2.3.3-stable**
(`versionCode 183`) checkpoint at
`aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`.
[Historical V183 CI](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39129657906)
was successful. V183 is **not** the latest frozen personal stable because a distinct
V195.5 stable branch was created later. Do not force-move `main` or stable refs
to conflate these lines.

## Current development: V208 preview and V209 maintenance

- **V208 / 2.8.2-v208-setup-backup-preview / versionCode 232** is the verified
  personal-preview candidate at exact commit
  `05a082d73696892764e77fa12edf3df07985d582` on
  `feature/backlot-v208-restore-setup-backup-validation`.
- [V208 draft PR #87](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/87)
  and [exact-commit green CI #39750705638](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/39750705638).
  The creator reports this personal installation works on their device.
- **V209 P1** is a *non-functional* hygiene/doc gate stacked on the V208 SHA.
  It does not change the APK, version, app ID or signing identity, and must not
  be represented as a newer installed app or commercial release.
- The V195.5 stable snapshot remains untouched. V197 was a release candidate,
  not a promoted stable branch; V208 is still a personal preview.

No CI result or single-device test alone certifies public release readiness,
full backup restoration, Play policy compliance, privately controlled signing,
cross-device alarm delivery or complete personal-data export. Do not uninstall
a populated app or change its signing certificate as an update procedure.

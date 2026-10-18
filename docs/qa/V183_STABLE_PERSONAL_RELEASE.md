# Backlot 2.3.3 — Stable personal-install build

**Release branch:** `release/backlot-v183-stable-personal`  
**Feature baseline:** Backlot V182, including all stacked V151–V182 changes.  
**Android package:** `com.framebynavin.app`. Version code **183**, version name **2.3.3-stable**.

## What “stable” means here

- Creator-confirmed functional testing on their device, V182 passing Android 11 instrumentation, unit tests, lint, and APK compilation.
- The source and functionality are inherited from V182. This release changes only version identity, build packaging/signing configuration, historical version guards, and stable CI/docs.
- This APK is **non-debuggable**, signed using the repository's **public prototypeStable** keystore. The public signing key deliberately matches Backlot's earlier personal sideload previews.
- **Not** a production/Play Store release: the prototype key is present in the public repository and can be copied. It does **not** provide a privately controlled upgrade identity or production distribution security. A future production key requires a controlled signing/migration plan and should never be committed.
- Do not promise seamless upgrades from any build signed with a different certificate. Android will reject the update instead of preserving/migrating it automatically.
- Installation: back up important creator data; verify current installed package and certificate match; install as an update without uninstalling. Do not downgrade to earlier version codes afterward.

## Release gates

1. V161–V182 safety/source guards, JVM tests, Android lint and instrumentation test compilation pass.
2. Gradle builds `assemblePersonalStable`; its manifest is non-debuggable; `apksigner verify` passes.
3. The personal-stable APK and V183 debug APK have **exactly matching SHA-256 signing certificate digests**, verified automatically. Higher version code ensures V182 previews can be upgraded.
4. Android 11 emulator end-to-end reminders test suite passes on V183 code, including archived project suppression, token authority, quiet-channel routing and inbox actions.
5. Real phone already working per creator for V182 functionality. Exact V183 installer update on their phone has **not** been physically witnessed by CI. Final app/device upgrade check remains a post-build rollout check.
6. No binary release asset needs to be committed into the repository. Use GitHub Actions build artifacts with SHA-256 verification and controlled distribution.

## What stays the same

- Reminder acknowledgement, voice and alarm lifecycle, bounded quiet follow-ups, creator controls, Reminders Center history.
- Original project creation, Quick Create, Settings, Quick Actions and local data schema; no destructive migration.
- The existing `main` branch receives the consolidated source only after the stable workflow has passed and the main merge is confirmed free of unrelated changes.

## Known limits

- Android WorkManager schedules are deferrable and subject to Doze, permission, and OEM battery restrictions.
- User data cannot be transparently migrated across unrelated Android package signatures.
- A public key cannot be considered a secure long-term commercial/production signing identity.

## Main history reconciliation

The release branch contains a merge commit whose parents include both the complete
V182 development lineage and the three latest V150 main commits. The historical
`.github/workflows/v150-final-candidate.yml` is retained without rewriting its
history. `app/build.gradle.kts` deliberately uses the newer V183 package version,
signing and release build-type configuration instead of the obsolete V150 settings.
No forced updates were used to resolve the main branch divergence.

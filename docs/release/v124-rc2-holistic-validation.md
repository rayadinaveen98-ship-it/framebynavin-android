# FrameByNavin v124 — RC2 holistic validation

Version: `2.0.0-rc2-guided-first-run`  
Version code: `124`

This checkpoint closes the automated holistic RC2 validation stage after the Insights V2, automatic creator cloud sync, and Guided First Run slices were integrated. It adds no creator-facing product feature. The purpose is to prove the combined RC2 source still satisfies the repository's release contract end-to-end.

## Source checkpoint

- Branch: `feature/v2.0-rc2-holistic-validation`
- Validated source commit: `ac0e714b95fae1f4d24b8ec563be0d7c5a052da6`
- Authoritative workflow: `RC2 Holistic Validation`
- Final strengthened run: `34883256567`
- Result: **PASSED**

A preliminary holistic run (`34882314118`) had already passed source integrity, unit tests, debug/instrumentation compilation, debug lint, debug APK, and release APK compilation. The final strengthened run is authoritative because it also enforces release lint, the instrumentation APK, the Play bundle path, 64-bit native counterparts, 16 KB ELF alignment, and 16 KB ZIP alignment.

## Automated gates passed

The final run passed all of the following from the same source commit:

- RC2 source-integrity audit;
- `:app:testDebugUnitTest`;
- `:app:lintDebug`;
- `:app:lintRelease`;
- `:app:assembleDebugAndroidTest`;
- `:app:assembleDebug`;
- `:app:assembleRelease`;
- `:app:bundleRelease`;
- release APK existence verification;
- 32-bit native library -> matching 64-bit counterpart verification;
- 64-bit ELF `LOAD` alignment >= `0x4000` (16 KB);
- Android build-tools `zipalign -c -P 16 4` verification.

The combined Gradle gate completed successfully with 144 actionable tasks.

## Source/security invariants passed

The RC2 source audit also verified that the combined product still preserves these contracts:

- `compileSdk = 36` / `targetSdk = 36` release foundation remains in force;
- production signing is an explicit optional configuration, separate from the prototype/debug signing key;
- Android automatic backup is disabled;
- cleartext traffic is disabled;
- release App Check uses Play Integrity while debug uses the debug provider;
- App Check installation remains wired before optional AI use;
- automatic creator cloud sync retains conflict protection and compare-and-swap expectations;
- cloud background work requires network connectivity;
- creator sync RLS migration exists and authorization derives ownership from `auth.uid()`;
- Google Drive remains an optional manual copy/import-export path, not an automatic second sync authority;
- Guided First Run still begins at Today, reaches Control, and is replayable;
- New Project still routes directly to the created project's real Workspace;
- 24H Insights evidence and Daily Views drill-down remain present;
- no Supabase privileged/service-role credential is present in Android source.

## Artifacts

### Installable QA APK

- Artifact: `FrameByNavin-v124-rc2-holistic-apk`
- Artifact ID: `10363484818`
- Artifact ZIP digest: `sha256:88f84f8c4ec19a85ee3fd2913a721205f9a8b3a27961ddf0fa16d7348257ede5`
- APK SHA-256: `4145624f8c18d912a281526da2b984c5f73cadad10143fe688649b1c8e2b6d4e`

### Unsigned release APK + AAB

- Artifact: `FrameByNavin-v124-rc2-release-unsigned`
- Artifact ID: `10364626483`
- Artifact ZIP digest: `sha256:3d472787459886d7e5d9c270fb06cf94d7ab25c956651e0c9fe3ff6cd4517c98`
- Unsigned release APK SHA-256: `ebaaee52fdb4d0d1d56c9aa0abb83b4b44706d5e6df2f4d7dcff373c6874d3f6`
- Unsigned release AAB SHA-256: `95685fe4cda85f938e7d667cd751b2a76a02d761f970978ebcbc1f9b48e766b1`

### Verification + lint

- Verification artifact: `FrameByNavin-v124-rc2-verification`
- Artifact ID: `10364725057`
- Digest: `sha256:94a8e13f0979768d6c36e270bd3fbb712fd339c137fcc7f89ed2cf351e7cafbe`
- Lint artifact: `FrameByNavin-v124-lint-reports`
- Artifact ID: `10364157455`
- Digest: `sha256:0d75bc768e6706fda3bf96116bf7144347f2fdf8e1ee22293918547ab778f93c`

Artifacts are retained by GitHub Actions for 90 days from the run.

## Live Supabase verification

The production Supabase project was checked separately from Android CI:

- migration `20260914174737_creator_snapshot_sync_v123` is present;
- `creator_sync_heads` has RLS enabled;
- `creator_sync_snapshots` has RLS enabled;
- the live tables retain their creator-owned policy set;
- no new RLS/security regression was reported by the advisor.

The security advisor still reports the pre-existing warning that leaked-password protection is disabled. This does not weaken the Google-ID-token creator sync authorization model, but should be enabled before any future password-based Supabase Auth surface is relied upon. Reference: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

The performance advisor reports two non-blocking INFO items:

1. `creator_sync_heads_snapshot_fk` does not have a covering child-side index;
2. `creator_sync_snapshots_user_created_idx` has not yet been used.

The first item is a legitimate hardening candidate for the next release-readiness stage. The second is expected to be low-signal immediately after a new sync schema is introduced and is not a reason to remove the index yet.

## Non-blocking compiler maintenance notes

The successful build still emits maintenance warnings, mainly:

- Material icon directional APIs that now prefer `AutoMirrored` variants;
- a few statically constant conditions in legacy reminder UI code;
- `YouTubeAnalyticsStore` data-class copy-visibility behavior that Kotlin warns will tighten in a future compiler release.

They do not fail current lint/build gates, but the Kotlin copy-visibility warning should be addressed before a Kotlin 2.2 migration.

## Gates intentionally still open

This checkpoint is **not** a public-production release. Repository CI cannot truthfully close these gates:

1. provide the real production/upload signing key through the external `FRAMEBYNAVIN_RELEASE_*` variables;
2. build the production-signed APK/AAB and record its certificate SHA-1/SHA-256;
3. register the final release fingerprints with Google/Firebase;
4. verify Google sign-in and YouTube authorization from that production-signed build;
5. verify Firebase AI through Play Integrity App Check on the production-signed build before enforcement;
6. execute the automated instrumentation suites on Android emulator/API coverage as a supplement to device QA;
7. complete physical Android acceptance, especially reminders/permissions, reboot/background behavior, Google/YouTube auth, automatic Supabase sync/restore, Android 16 edge-to-edge/back behavior, and destructive recovery scenarios.

## Next stage

Proceed to release-readiness hardening from this checkpoint. Keep production signing material out of Git, add repeatable emulator/API coverage, resolve safe backend/performance hardening, and prepare the exact production-signed + physical-device acceptance checklist. No new creator-facing feature should bypass those gates.

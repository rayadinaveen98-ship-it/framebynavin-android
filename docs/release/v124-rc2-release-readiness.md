# FrameByNavin v124 — RC2 release-readiness checkpoint

Version: `2.0.0-rc2-guided-first-run`  
Version code: `124`  
Package: `com.framebynavin.app`

This checkpoint closes the **automated** part of Stage H release-readiness hardening. It does not claim that FrameByNavin is publicly production-ready. The remaining gates depend on the real production signing certificate, external Google/Firebase certificate registration, and physical-device runtime acceptance.

## Source checkpoints

- Working branch: `feature/v2.0-rc2-release-readiness`
- Prior holistic RC2 source: `ac0e714b95fae1f4d24b8ec563be0d7c5a052da6`
- Emulator-matrix validated source: `5d6500fcb8e3dfc5f768d81c6a184067661b0329`
- Production identity audit source: `57624f3d73aac0ece83697d85588517589b23e0d`
- Production release-gate script commit: `512f8f3b1cf2e8c8b73d1455fbb2b90e77a11b08`
- Supabase covering-index migration commit: `84fcc706f59723081ba92356da8e6e99460adfc0`
- Physical-device acceptance checklist commit: `9c39a22ba776b4340d883a994deeefc48de1c9c4`

The commits after `5d6500fc...` in this branch are documentation-only and do not change the Android binary exercised by the emulator matrix.

## Android 13–16 emulator matrix — PASSED

Authoritative workflow: `RC2 Release Readiness Emulator Matrix`  
Run ID: `34885984674`  
Head SHA: `5d6500fcb8e3dfc5f768d81c6a184067661b0329`  
Result: **PASSED**

The same focused creator regression suite passed on:

- Android 13 / API 33;
- Android 14 / API 34;
- Android 15 / API 35;
- Android 16 / API 36.

The suite covers the existing creator persistence, cloud-local storage, portable backup/restore, content-project persistence, Project Pulse history, reliability, primary navigation, and core interaction instrumentation tests.

### Emulator artifacts

| API | Artifact | Artifact ID | ZIP SHA-256 |
| --- | --- | ---: | --- |
| 33 | `FrameByNavin-v124-api-33-instrumentation` | `10364129389` | `2cdbba9cebc60b2124fcaffc2c066f96a8307da13792d4372bc94b956ac2bfb3` |
| 34 | `FrameByNavin-v124-api-34-instrumentation` | `10365036147` | `8f56280c461be8be1cff2736eb2f598b9a2ee6a409cd43782501a42cdca52749` |
| 35 | `FrameByNavin-v124-api-35-instrumentation` | `10364714698` | `89213a842890607ed9e3a9592743cddf61f1129f10393e2ce72392dd66c5d1ee` |
| 36 | `FrameByNavin-v124-api-36-instrumentation` | `10365335926` | `682f6787480319350701bfc1ead9d8f4418bd8bc9073b37d33b4fc1591f7b3e8` |

Artifacts are retained by GitHub Actions for 90 days from the run.

## Failure found by the first matrix and how it was resolved

The first matrix run (`34884569223`) correctly failed on all four API levels. The failures were not platform-specific production crashes. They exposed three stale instrumentation expectations:

1. the backup hardening test still expected schema 5 while the production backup contract had deliberately advanced to schema 8;
2. the legacy-v3 restore test created an invalid legacy checksum by naively downgrading a current snapshot instead of constructing a schema-faithful legacy fixture;
3. the Today UI test still asserted the older `CREATOR FOCUS` / verbose publishing-progress presentation instead of the current `WEEKLY FOCUS` compact card.

Fixes:

- `b1032b356356720c0e2dc19f66afe3f48a36f807` — align backup hardening tests with schema 8 while preserving valid legacy-v3 semantics;
- `bf4637aca58875162a4b6e7d4ef0170ca140a0ec` — align Today UI assertions with the current weekly-focus contract;
- `5d6500fcb8e3dfc5f768d81c6a184067661b0329` — make the matrix rerun for app/test/Gradle changes instead of only workflow-YAML edits, and disable emulator metrics prompting.

No production UI or backup semantics were weakened to make the tests pass.

## Supabase release-readiness hardening — VERIFIED

The live Supabase Performance Advisor had reported that the composite foreign key from `creator_sync_heads(user_id, revision)` to `creator_sync_snapshots(user_id, revision)` lacked a covering child-side index.

The migration recorded in Git is:

`supabase/migrations/20260914190438_add_creator_sync_head_fk_covering_index_v124.sql`

It adds only:

`creator_sync_heads_snapshot_idx (user_id, revision)`

The live tables were empty when the change was applied. The change does not alter creator rows, ownership, RLS, RPC behavior, authentication, conflict semantics, or sync authority. After application, the foreign-key advisor warning was cleared. Remaining unused-index INFO notices are expected on the new/empty sync tables and are not a reason to remove the indexes yet.

RLS/security ownership remains unchanged. The pre-existing leaked-password-protection warning remains a separate future concern if password-based Supabase Auth is ever relied upon; the current Google-ID-token creator-sync model does not depend on password login.

## Production identity readiness — repository side PASSED

Authoritative workflow: `RC2 Production Identity Readiness`  
Run ID: `34885097293`  
Result: **PASSED**

Artifact:

- name: `FrameByNavin-v124-production-identity-readiness`
- artifact ID: `10364846142`
- ZIP digest: `sha256:64125f16fd2379285d98cb06d8834d84a37819b9373adae644af6829090d6a94`

The audit verifies that:

- Firebase config contains package `com.framebynavin.app`;
- the Web OAuth client required for Google ID-token flow is present;
- `CloudConfig` matches that Web OAuth client;
- all four production-signing inputs remain externalized;
- release signing is conditional on a complete production key set;
- release App Check uses Play Integrity;
- debug App Check remains isolated to the debug source set.

The audit intentionally does **not** claim remote certificate registration. Current `google-services.json` does not yet contain the final Android OAuth client (`client_type=1`) tied to the real production signing SHA-1. That remains a required final gate.

## Production signing/package gate — PREPARED, NOT EXECUTED WITH REAL KEY

`scripts/production-release-gate.ps1` is the final local production-signing gate. It requires the secure external values:

- `FRAMEBYNAVIN_RELEASE_STORE_FILE`;
- `FRAMEBYNAVIN_RELEASE_STORE_PASSWORD`;
- `FRAMEBYNAVIN_RELEASE_KEY_ALIAS`;
- `FRAMEBYNAVIN_RELEASE_KEY_PASSWORD`.

The script:

1. inspects the real production certificate and prints SHA-1/SHA-256;
2. requires the refreshed Android OAuth client unless deliberately running a pre-registration diagnostic mode;
3. runs release lint and builds the production-signed APK + AAB;
4. verifies the APK certificate/signature with `apksigner`;
5. verifies the AAB JAR signature with `jarsigner`;
6. records source commit, version/package, certificate fingerprints, APK/AAB SHA-256 values and remaining runtime gates in `verification/production/production-release-manifest.txt`.

No keystore/password/signing secret belongs in Git or chat.

## Physical-device acceptance — DEFINED, PENDING EXECUTION

The authoritative checklist is:

`docs/release/v124-production-device-acceptance.md`

It requires the **exact production-signed APK** and covers:

- clean install + Guided First Run;
- production Google identity and YouTube authorization;
- automatic Supabase sync, offline edits, reinstall/restore and account switching;
- reminders/permissions/background/lock-screen/reboot behavior;
- project/workflow/creator-loop smoke;
- Insights interaction and data-honesty checks;
- Firebase AI through production Play Integrity App Check;
- Android 16 edge-to-edge/back/permission behavior;
- upgrade/portable backup/recovery behavior;
- performance/accessibility sanity.

Emulator CI supplements this checklist but does not replace it.

## Automated Stage H status

Completed:

- safe Supabase covering-index hardening;
- repository-side production OAuth/App Check/signing audit;
- production-signing verification script;
- repeatable Android API 33/34/35/36 emulator regression matrix;
- correction of stale backup/UI instrumentation expectations;
- physical-device acceptance protocol.

## Gates still open before public release

1. Choose/provide the real production/upload signing key through the external signing variables.
2. Read the real certificate SHA-1 and SHA-256.
3. Register production SHA-1 with the Android OAuth/Firebase configuration.
4. Refresh `app/google-services.json` and rerun `production_identity_readiness.py --require-android-oauth-client`.
5. Register production SHA-256 for Firebase App Check / Play Integrity.
6. Run `scripts/production-release-gate.ps1` and preserve the exact signed APK/AAB hashes and verification manifest.
7. Install that exact APK on the physical target phone and execute `v124-production-device-acceptance.md`.
8. Only after those gates pass should the candidate be promoted/frozen as the public v2.0 release baseline.

## Release decision

**Automated RC2 release-readiness: PASSED.**  
**Production signing/remote certificate registration: PENDING.**  
**Physical-device acceptance: PENDING.**  
**Public v2.0 release readiness: NOT YET CLAIMED.**

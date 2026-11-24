# Backlot — First Launch: Backup, Restore, Delete and Play Readiness
Date: 2026-11-24
Owner decision: FREEZE AI/new Creator features/redesign; only two streams: (A) safe backup/restore/delete and (B) first Android public launch readiness.
Status: PLAN and source/backend READ-ONLY AUDIT. No app code, live data, hosted schema, paid service, APK or release modified by this roadmap.
Source of truth: owner-accepted Lab R2 application tree 28a7000159554f254ab1ecc1aecc7792a1385ac2; stable/backlot-lab-r2-v337-owner-accepted-20261124; read-only live Lab Supabase vasleafrmsovdbdlrcsw.

## Evidence from accepted source
- Stable Lab: signed personalStable, package com.framebynavin.app.lab, versionCode 337; owner accepted first-run and normal navigation, but deep data/lockscreen/cross-device checks NOT fully certified.
- Release Gradle: compileSdk=36, targetSdk=36, minSdk=26. Production applicationId defaults to com.framebynavin.app; prototypeStable uses repository-available development signer, NOT suitable for Google Play; release gradle task requires PRIVATE external keystore.
- CloudConfig: Lab package uses vasleafrmsovdbdlrcsw while standard com.framebynavin.app builds use OLD kukkqgpzxnfanynbddiw. Before Play release founder must approve a production backend selection/data migration decision, without silently deploying to either.
- Source Settings V144SettingsHub: Settings > Privacy > Data & Privacy exists and describes current limits; Settings > Advanced has device-only Start fresh. Earlier backup menu entry removed by owner design decision.
- The BackupActivity and PersonalBackupActivity legacy UIs persist as nonexported Android activities, but not evidence that they are exposed from current Settings. Legacy Creator portable and Personal JSON exports are explicitly NOT encrypted; external creator attachment bytes aren't bundled, and each domain requires a separate file.
- V341UnifiedBackupManager implements a password-protected encrypted combined Creator+Personal export and read-only inspection, but V341UnifiedRestoreInspection.canCommitUnifiedRestore is hard-coded false.
- V342UnifiedRestorePreparationStore and V344UnifiedRestorePrecommit provide encrypted source retention/authenticated preflight with deliberately NO live restore authority. V379UnifiedReleaseCoordinator documents combined restore still DISABLED pending Package J emulator QA. Do not describe unified restore as shipped.
- PersonalCloudSyncManager has explicit PersonalCloudIncompleteChallengeCoverage guard: single-activity 21-day challenges block Personal cloud upload/restore to avoid silent data loss.
- CreatorCloudSyncManager uses hash/revision-aware snapshots, a conflict path, and separate Personal sync handling, but full multi-device conflict/restore/owner-owned media coverage NOT verified.
- V269DataPrivacyActivity.DELETION_STATUS: full Backlot account deletion unavailable and no request submission possible. Existing supabase/account-deletion/deletion_core.ts and deletion_queue.ts are core algorithms/adapter contracts not deployed production service.
- Supabase read-only live Lab check 2026-11-24: healthy; public creator_profiles, creator_sync_snapshots, creator_sync_heads, personal_sync_heads all have RLS enabled; migration history includes initial bootstrapping, private voice policy and UX09 record migration; no account-deletion queue table in public schema. Security advisor shows auth_leaked_password_protection disabled WARN; inspect relevance if password login enabled. Do NOT touch live records.
- User-reported automatic 6-lesson first-user app tour absent; manual Settings > About > Show app tour exists.

## Workstream A — Data safety (no destructive operations on real accounts)
A0. Scope & coverage matrix (CURRENTLY UNDER SOURCE AUDIT):
    Inventory Creator Projects/Ideas/Scripts/Stages/Assets/Voice Idea files/Best Frames; Personal Reminders/Completion history/Spaces/photos/Challenges; schedules, widgets, custom music, preferences, identity, keys/tokens; distinguish copied bytes vs references vs unsupported. Decide honest user disclosures.
A1. Product contract and UX:
    Present one Data & Privacy destination for "Back up", "Restore", "Delete account/data" without resurrecting old clutter; keep Advanced > Start fresh as strictly local. Logged-out users get local export; signed-in users see cloud status and actual last confirmed copy; unsupported/unavailable flows disabled with truthful explanations. No fake "everything backed up".
A2. Verified encrypted unified BACKUP:
    Use canonical V341 owner archive + explicit passphrase; coverage and size matrix, full readback/hash/authentication, no logs containing user content. Verify interruptions, low disk, exports to document picker. External assets as references unless independently approved secure packaging.
A3. Transactional unified RESTORE:
    Finish H4 recovery/commit/release safety only on separately approved isolated branch; replay on disposable fixtures and cross-process kill; validate source + original, preserve rollback escrow, no partial two-domain commits, correct revision/generation and alarm reschedule. Unsupported challenge domains must BLOCK, not silently drop data.
A4. Cloud Backup/Restore/CAS:
    Two-device and two-real-account synthetic tests against isolated Lab tenant; owner/other user negative tests with verified tokens; long-unavailable/offline handling, signed-out/changed-account guard, conflict previews and explicit choices, voice media and completion receipts. Restore should never erase newer data without owner confirmation. Verify actual Lab RLS/RPC/Storage, not just static SQL.
A5. Full user DATA AND ACCOUNT DELETION:
    Distinguish (1) erase this phone, (2) delete a saved backup, (3) delete Backlot account + associated cloud records + stored media, (4) locally retained/exported data. Add approved server-authorized fresh reauth and request URL/page, deletion queue and idempotent worker with write fence, storage/object pagination and hard bounds, row cleanup, Auth deletion LAST; verify completion, failures/retry and cross-account isolation. Publish transparent retention exceptions and support channel. Never delete Google account.
A6. Recovery security validation:
    RLS, least privilege, legacy/backup retention, cross-account and revoked credentials, test fixture destructive operations ONLY. Strong account deletion must not rely only on client-supplied account ID or user_metadata.
A exit: signed candidate proves encrypted export/readback, correct coverage, two-domain restore/recovery across kill/restart, real cross-device Lab restore, genuine app+web account deletion and full data cleanup on synthetic test accounts; no data leaks/loss.

## Workstream B — First Android Play launch
B0. Protect stable source and account/backend identity:
    Freeze accepted Lab branch and APK, use feature branch and release-candidate branch; choose production package, backend and migration explicitly. Avoid switching current Lab APK data/backend without scoped plan. No forced git updates.
B1. True FIRST USER flow:
    Creator-first Home; account optional; five-page Creator setup and independently six-lesson tour; make first-run tour discoverable/optional and reliable without returning-user loop. Guest/new Google/signed-out/returning user, permission declines, offline, 320dp/tablet/TalkBack.
B2. Production security and store compliance:
    Private out-of-repo upload signing key, Play App Signing, release AAB, real Play-installed build smoke (including Google OAuth client/SHA-1/SHA-256 for Play App Signing); backend and data deletion webpage; in-app privacy policy and live public link; Data safety, privacy disclosures, account deletion form, permissions rationale (microphone, notifications, exact alarms, fullscreen, foreground service, calendar), ratings and app access.
B3. QA reliability:
    No debug-only gate; compile and full lint, tests for backup/restore/delete, exact SIGNED RELEASE installed from AAB/internal Play track on Android 12-16 and phones/tablets; real offline, reboot, missed alarms, lockscreen fallback, battery restriction, auth failures, malformed backups, network interruption, long data, upgrade, crash-free metrics and background behavior. Use APK checkpoint before downstream verification to avoid 40min rerun.
B4. Listing and release ops:
    Approved icon, final brand, screenshots of real UI, feature graphic, short/full descriptions, support contacts, verified privacy/deletion URLs, Data safety/App content forms, Play Console account verification, testing track eligibility. Closed testing 12 opted-in testers continuously 14 days if account is a qualifying newer personal Play developer account. Add rollback, incident response, release monitoring and staged rollout.
B exit: privacy/delete+backup certified, production AAB signed and Play-installed with no crash, owner physical multi-device pass, all console forms accepted and Play testing eligibility verified; Founder decides GO / NO-GO.

## Important external policy evidence, checked 2026-11-24
- Google Play new mobile apps target Android 16 API 36: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play app account deletion: in-app + web removal pathways, user data cleanup and retention disclosure: https://support.google.com/googleplay/android-developer/answer/13327111
- New apps require Android App Bundle and Play App Signing: https://developer.android.com/guide/app-bundle
- New personal Play developer accounts created after Nov 13, 2023: minimum 12 opted-in testers continuously 14 days in closed track before production access: https://support.google.com/googleplay/android-developer/answer/14151465
- Foreground service and USE_FULL_SCREEN_INTENT declarations: https://support.google.com/googleplay/android-developer/answer/13392821
- SUPABASE audit pending release: inspect Supabase release notes, RLS and Auth/Storage docs before actual backend implementation.

## Release-blocking issues, verified
P0-A: No full real account deletion (UI explicitly unavailable, server queue not live).
P0-B: Unified restore deliberately disabled pending runtime certification.
P0-C: Personal cloud 21-day challenge coverage guard (cannot promise complete backup).
P0-D: No authenticated end-to-end two-device Creator+Personal+voice restore/test against real Lab users proven by this audit.
P0-E: Production backend selection unapproved (standard build points to old backend while Lab points to new).
P0-F: No verified private Play upload signing/AAB and real Play-installed OAuth path.
P1-G: Creator new-user auto tour absent.
P1-H: Public privacy policy/deletion request URLs, Play Console disclosures/listing/test eligibility not proven by repository audit.
P1-I: Legacy Creator and Personal exports unencrypted: if exposed, label correctly and keep out of "secure one-file backup" promise.
P1-J: Full physical reminder/lockscreen/accessibility/permission matrix pending.

## Stop rules
No deletion of real user data in development or tests. No migration/deletion/paid hosting/private signer creation without separate founder approval. No claim of "complete backup" when media bytes or challenge history are uncovered. No stable promotion from isolated debug-only test. Preserve the signed Stage7 R2 owner APK and exact app tree until a new release candidate passes source, runtime and founder acceptance.

## Suggested immediate next stage
A0 + B0 READ-ONLY acceptance inventory, draft a per-domain matrix and Play account/backend decision sheet; then obtain founder approval before coding A2, A3, A5 or production release changes.

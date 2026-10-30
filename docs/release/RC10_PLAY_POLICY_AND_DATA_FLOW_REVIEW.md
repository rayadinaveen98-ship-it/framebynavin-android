# Backlot RC10 — Play Console permissions and data-flow readiness

**Date:** 2026-10-30 (India). **Scope:** Review/QA only. Frozen **Build267** app source `5b0682140ae19ec698c0c84aa2be8a9939844500`; RC10's predecessor RC9 `91b650d7f1f56caa7c703416c01b8566d819b17a`. No production APK, signing keys, package ID, app code, permission changes, user data, Play Console submission or `main` modification.

## Evidence already complete

- [RC6](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40922583197) five green Android11/15 jobs: Build267, focused UI and 266→267 data upgrade.
- [RC7](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40926157504) five green Android16 jobs: UI, reliability, 266→267 upgrade, unsigned AAB structural dry run.
- [RC8](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40927510200) two green jobs: different signer rejected on disposable Android16 without losing synthetic data.
- [RC9](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40929167339) two green jobs: 34-PR provenance, 650 changed paths versus unprotected `main`.
- Owner preview identity: `com.framebynavin.app` / `versionCode 267` / `targetSdk 36`, full owner APK SHA256 `02827066650f7f725ef712c7a2119f20746fec5198fd1b7cfdc0be709e8d0d7d`. The preview uses a **public prototype test certificate**, not a private Play signing identity.

## Official Google Play / Android policy references reviewed 30 Oct 2026

1. [Target API 2026](https://developer.android.com/google/play/requirements/target-sdk): from 31 Aug 2026, normal phone new apps/updates target API36+. **Backlot's targetSdk36 meets this numeric target.** This is not Play acceptance.
2. [Exact alarm developer guidance](https://developer.android.com/develop/background-work/services/alarms): `SCHEDULE_EXACT_ALARM` is user-controlled special access; fresh installs may lack it. Check `canScheduleExactAlarms()`, explain to user, provide denied/revoked fallback; use only for genuinely time-critical alarms.
3. [Play full-screen intent restrictions](https://support.google.com/googleplay/android-developer/answer/13392821): `USE_FULL_SCREEN_INTENT` automatic grant for core alarm/call apps, Play Console declaration and user permission/fallback for non-qualifying apps. **Backlot is a multipurpose creator organizer with alarms, not automatically an alarm-clock app**; reviewer may not grant auto-access.
4. [Play foreground service + full-screen declaration](https://support.google.com/googleplay/android-developer/answer/13392821): target API34+ FGS use requires description, type, user impact if interrupted, and demonstration video in Play Console.
5. [Android foreground service type definitions](https://developer.android.com/develop/background-work/services/fgs/service-types): `mediaPlayback` is for continued background audio/video playback; **Android15+ forbids launching mediaPlayback FGS directly from a `BOOT_COMPLETED` broadcast**. Verify actual reminder audio lifecycle; avoid assuming an alarm sound automatically qualifies.
6. [Data Safety collection definitions](https://support.google.com/googleplay/android-developer/answer/10787469): transmitting data off device **through SDKs** counts for collection analysis. Declare types, purposes, optionality, security and deletion based on verified runtime/server and SDK data practices, not manifest only.
7. [Privacy policy requirement](https://support.google.com/googleplay/android-developer/answer/9859455): public globally reachable ordinary HTTPS/HTTP page, linked both in app and Play store listing; comprehensive and app-specific disclosures.
8. [App account deletion](https://support.google.com/googleplay/android-developer/answer/13327111): if users create accounts in or from the app, provide a discoverable in-app deletion route and external request webpage; deleting an app account entails associated user data deletion, subject to disclosed lawful retention.

## Current manifest permission-to-feature review

Source: `app/src/main/AndroidManifest.xml` (not an inferred list). `targetSdk = 36`.

| Permission / service | Observed application evidence | Play review / proof needed | State |
|---|---|---|---|
| `INTERNET` | Supabase REST auth/creator snapshot, Google/YouTube API, Firebase AI optional | Verify endpoints, encryption, retention, user-facing disclosure | **Unverified** |
| `POST_NOTIFICATIONS` | Creator/Personal alerts | Runtime grant/denied flow, visible behavior and quiet-hours proof | **Needs phone evidence** |
| `SCHEDULE_EXACT_ALARM` | User-scheduled alarm reminders, recovery broadcast | Only precise user alerts; access-grant explanation, denial/revocation/Doze tests | **Needs phone+policy evidence** |
| `USE_FULL_SCREEN_INTENT` | `AlarmRingingService` AND `VoiceReminderService` call `setFullScreenIntent` | Prove alarm-centric necessity and declaration; no pregrant assumptions for creator organizer; permission denied fallback | **HIGH policy risk** |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Creator alarm/voice audio services and Personal audio service have `foregroundServiceType="mediaPlayback"` | Demonstrate actual audio playback; 3 service-type declarations, user interruption impact, supported paths on Android15+ | **HIGH review risk** |
| `RECORD_AUDIO` | Voice Idea recorder and opt-in push-to-talk Spark `createOnDeviceSpeechRecognizer` | Runtime consent and microphone use explanation; no silent recording; remote AI not confused with local voice recognition | **Needs manual review** |
| `READ_CALENDAR`, `WRITE_CALENDAR` | `CreatorCalendarSync` reads selected Google calendar and adds/updates chosen project deadlines after explicit S1 consent | Access must be contextual, purpose-limited; project descriptions sent to device Calendar provider may sync with user's Google account | **Needs documentation** |
| `RECEIVE_BOOT_COMPLETED` | Reminder recovery receiver | Confirm only scheduling on boot; **no mediaPlayback FGS launched directly from BOOT_COMPLETED** on API35+ | **Needs code/runtime proof** |
| `WAKE_LOCK`, `VIBRATE` | Reminder audible/vibration screen | Short bounded wake usage and meaningful foreground alerts | **Needs proof** |

**Do not remove any of these during the RC10 review**; such changes would require a fresh Build268 and regression matrix to protect alarm delivery.

## Data Safety: evidence inventory (DO NOT submit yes/no declarations yet)

| Feature/data | Repo evidence | External transfer / privacy question |
|---|---|---|
| Creator Google account / username / profile | `cloud/CloudSyncManager.kt`, `CloudApiClient.kt` and `CloudLocalStore.kt` | Google ID token to Supabase auth, account metadata/storage; identify actual servers, retention and deletion requests |
| Creator projects, ideas, media | `cloud/CreatorCloudSyncManager.kt`, `CreatorCloudVoiceMedia.kt` | Optional cloud snapshots/media potentially carry creator work to Supabase; verify toggles and defaults, categories, deletion and at-rest encryption |
| YouTube identity and analytics | `youtube/YouTubeAuthorization.kt`, `YouTubeApiClient.kt` | OAuth `youtube.readonly`, `yt-analytics.readonly`, separate optional monetary scope; determine local storage vs transit and third-party SDK processing |
| Google Calendar titles/deadlines | `ui/V129GoogleCalendarSettings.kt` | User opt-in provider operations, calendar provider may sync account remotely; explain destination |
| Firebase AI video autopsy | `ai/CreatorGeminiVideoAutopsyProvider.kt` | Optional evidence prompt + public video reference sent to Firebase AI / Google AI backend; verify consent, output handling and provider retention |
| Firebase App Check and analytics/SDK defaults | `app/build.gradle.kts` Firebase AI, App Check Play Integrity, debug SDK declarations | Validate actual runtime initialized SDKs and data to Google; **do not assume 'no collection' from local-first architecture** |
| Personal Reminders | `personal/PersonalBackupTransfer.kt`, `ui/PersonalBackupActivity.kt` | Separate local DataStore; exported Personal JSON includes private note text and is **not encrypted**. Users may choose to share/transfer file; keep separate from Creator cloud |
| On-device Spark voice | `ui/V2293SparkVoiceSession.kt` | Uses Android on-device recognizer; never infer this protects separate optional Firebase AI requests |

### Privacy policy and deletion readiness

- No project-authored `privacy*` / `data-safety*` policy **path** was detected in tracked non-`app/build` source inventory in this review. This **does not prove** a separate hosted website or Play listing is absent. A live public policy URL and in-app link have **not** been verified.
- Supabase account creation/login is present. `CloudApiClient.deleteCreatorSyncData` deletes one creator-sync domain, **not automatically a complete app account**. Verify full account deletion UX including Google/creator profile, cloud data, retention and public web request page before a Play release.
- Do not submit a Data Safety checkbox or claim encrypted/private/offline-only based on the repository alone. The app contains external auth/sync/AI and SDKs requiring a service- and network-level review.

## Repository hygiene (release and secrets audit, not app change)

- Git tree for frozen RC9 snapshot contains **6,574 tracked entries under `app/build/`**, including generated intermediate files. These should not normally live in source history; review safe cleanup in a **future separate source-controlled change** with a fresh build/provenance check. Never delete any current user's `filesDir` or app data.
- Tracked `dev-signing/framebynavin-public-debug.jks` is an **intentionally public prototype** signer and cannot be trusted for production. Tracked `app/google-services.json` is Firebase client configuration; inventory it without pasting values into public CI logs, and check configuration/Google Cloud security restrictions. Do not assume this client file is an admin key.
- `main` remains unprotected as last verified, tracked in [issue #185](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/185). The package/signing migration is open [#180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180). The release tracker is [#174](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174).

## RC10 CI contract

The separate read-only CI workflow must assert **exactly two RC10 changed files**, confirm Build267 frozen version+manifest permissions and the specific source evidence above; emit a machine-readable `rc10-play-manifest-policy-inventory.json` and `rc10-data-flow-review-open.json` containing **no API keys, no user records and no remote credentials**. It must explicitly mark `play_policy_approved=false`, `privacy_policy_verified=false`, `data_safety_filed=false`, `account_deletion_verified=false`, `production_signer_verified=false`, `main_merge_authorized=false`. A green workflow means **the audit completed, not that Backlot satisfies Play policy**.

## Owner/reviewer follow-ups before Play internal track

- [ ] Confirm actual Play app identity and resolve signing transition with verified Creator, Personal and source-media exports; no uninstall/data loss.
- [ ] Test exact alarm special access denied/revoked, full-screen alert denial, foreground audio timing/Doze and phone lock behavior; video-record each feature's valid user journey for potential Play reviewer request.
- [ ] Decide whether full-screen intents are essential in a creator app; document and test fallback instead of assuming auto-grant.
- [ ] Complete detailed SDK+network/retention audit and draft specific public privacy policy, show inside app + store listing.
- [ ] Provide verified in-app AND external app-account deletion requests if account creation is offered; verify deletion of all relevant associated cloud data.
- [ ] Complete accurate Data Safety and app-access/permission declarations in Play Console, including FGS.
- [ ] Fix repository protection [#185](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/185), review stacked PR integration, obtain owner physical-device signoff, production private key and Play internal testing approval.

**No automatic Play submission, policy declaration, new signing key, source-code alteration or merge occurs in RC10.**

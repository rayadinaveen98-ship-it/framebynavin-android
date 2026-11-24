# Backlot Lab R2 — Reminder Stabilization / Controlled Change Record
Date: 2026-11-24
Owner: Founder. Status: APPROVED DESIGN, IMPLEMENTATION STAGED IN ISOLATED BRANCH (not yet integrated/tested/released).

## Source of truth and rollback
- Founder's working Backlot Lab and previous accepted stable branches stay protected.
- Frozen Android 15 R2 QA-tested source commit: `64ced7c21f0de30c82926505985d81ddcaf5a5f3`.
- Original source reconciliation: `1577669bc328153a365661a7f3d36fb1558e891c`.
- R2 two-file Kotlin repair already present in tested source: `V144SettingsHub.kt` and `V407ReminderAlertReadiness.kt`.
- QA evidence: one-shot Lab Kotlin compile Actions `45309746036`; Android 15 Lab debug/test APK and 2/2 first-run navigation Actions `45312742080`.
- The *separate* non-debuggable owner-preview `personalStable` build `45315344548` FAILED during `:app:lintVitalAnalyzePersonalStable` (internal Android Lint exception analyzing `V101BReminderUi.kt`); no certified signed owner preview from that run.
- This branch starts at the SHA above. No merges, releases or changes to original Backlot package, cloud host, Supabase schema/data, signing, existing owner installations, or populated local data.

## Founder-approved scope (one stabilization package)
1. Three DIFFERENT premium Voice/Alarm emblems: Luminous Glass, Cinematic Emblem, Living Energy. Default = **Living Energy**, with one global local-only style selection under Settings → Personalization → Reminder Background. Selection independent of existing gallery and family preferences. Retain voice microphone vs alarm bell semantics.
2. Uploaded image/video/animated-image must not visibly flash an unrelated fallback for 1–2 seconds on a permitted unlocked screen; use cached/decode/poster-first presentation, maintain one video player from entrance through main, and never delay audio to wait for media.
3. Creator-only, explicit default-OFF setting for showing uploaded reminder media over keyguard, subject to Android permissions. Preserve Personal and 21-day private keyguard defaults.
4. Shared local music ducking for Creator/Personal/21-day Voice: initial player gain around 0.68, speaking around 0.18, restore to normal between utterances, short tail and smooth natural end. Do not change device-wide music volume; do not truncate TTS.
5. Controlled natural audio tail/fade for Voice and Alarm, immediate response commit and short safe termination for Done/Snooze/Dismiss, stale/error paths never wait for animations; audible alarm effectiveness preserved.
6. Creator Alarm optional TTS gets intelligible mix with alarm tone; 21-day Alarm currently uses notification-channel alarm audio and is a separately gated architecture/parity decision (no silent change).
7. Five-page Creator setup stays available; add deliberate owner re-entry/edit without looping completed returning-user onboarding. Creator OS remains first, Personal navigation consistent.
8. Home companion avatar anchored to top-right safe area with face-focused crop, responsive layout, no other Home redesign.
9. Consolidated physical QA of 3 domains × 2 alert modes × 3 device states and media/audio, permissions, action receipts, and privacy.

## Implementation stages and gates
- **S0 BASELINE**: create branch and this record; validate branch ancestry/source, no release.
- **S1 ICONS**: local style model/store, Compose-rendered Glass/Emblem/Energy microphone/bell variants, global Settings selection and previews, reduced motion and low RAM guards; no source edits to notification/service/DB.
- **S2 MEDIA**: poster/first-frame, still decode, GIF, one player, keyguard Creator opt-in; locked Personal/challenge privacy unchanged.
- **S3 AUDIO**: stronger shared ducking, restoration, controlled fades; alarm+speech mixing; playback lifecycle and action-safe cancellation.
- **S4 ONBOARDING/HOME**: safe creator-setup re-entry, top-right companion crop.
- **S5 CONSOLIDATED QA**: run source-contract and targeted Kotlin compile ONLY AFTER owner says staged work is complete; then narrow tests and Android15 scenarios from frozen candidate; fix failing stage without broad reruns.
- **S6 PREVIEW**: isolate/fix Android Lint internal analyzer failure, run single nondebuggable `personalStable -PbacklotLab=true` build, verify package `com.framebynavin.app.lab`, signer, label, hash, non-debuggable manifest. No unverified owner APK claims.
- **S7 TABLET**: founder physically tests all six modes, 18 core conditions and visual/audio, retains no-data-loss state.
- **S8 STABLE**: only with explicit Founder acceptance and gates green; preserve old stable rollback.

## Status accounting
APPROVED = scope/direction only. STAGED = branch code present, not proven. COMPILED / AUTOMATED QA = exact SHA evidence only. PHYSICAL QA = founder observations. PROMOTED = explicit protected stable action with hash, owner approval. Do not claim a stage complete without evidence.

## Invariants
- Alarm/voice scheduling, follow-up, Smart Snooze, account auth, 21-day challenge receipts, projects, notes, Personal spaces and all cloud data MUST remain untouched by presentation-only changes.
- Accessibility: TalkBack semantic labels; touch targets in Settings; animations-off = calm static icons; low-RAM/battery-saver bounded motion.
- Visual style choice is separate from three existing P10 background families and local P1 gallery media; no retired Classic/Premiere reintroduction.
- Rollback: revert only this branch's scoped commits to the exact frozen tested SHA, never delete user records or erase Android app data.

## Stage 1 — premium icons (SOURCE-STAGED; NOT BUILT/TESTED)
- Saved global icon preference defaults to Living Energy; Luminous Glass and Cinematic Emblem remain independently selectable.
- All three Voice/Alarm Compose vector families use the existing reminder presentation entry, without changing background families, media choices, alarm delivery or cloud state.
- Settings → Personalization → Reminder backgrounds offers all icon choices and both symbolic previews. Search entry updated.
- Contract tests added but deliberately NOT run before consolidated founder-approved QA.

## Stage 2 — media/lock-screen presentation (SOURCE-STAGED; NOT BUILT/TESTED)
- Creator-only lock-screen gallery option persisted in a NEW local key; default OFF and enabling requires explicit privacy-warning confirmation.
- Shared policy enforces CREATOR AND owner opt-in when keyguard is locked. Personal and 21-day gallery media remain hidden, regardless of the Creator preference.
- Removed extra initial lock-state frame: Creator and gallery renderer synchronously read Android keyguard status at first Compose presentation rather than assuming locked until the first lifecycle callback.
- Gallery import builds a bounded <=720px JPEG first-frame poster for still and animated images; existing video imports already produce the first-frame poster. Existing previously imported images obtain missing posters asynchronously on first use without reimport or deleting user media.
- Still and animated renderer displays bounded poster immediately when available while high-resolution decoding runs on IO; video player keeps its texture alive beneath poster and reveals it on Media3's `onRenderedFirstFrame` callback; soundtrack/voice remains separate and video remains muted.
- No new notification/receiver/service/scheduling/reminder-action changes; no new permissions, Supabase calls, migration or file deletion.
- Policy JUnit regression tests added for opt-in Creator visibility, explicit family/gallery mode, safe default and Personal/21-day privacy. Not run yet.
- QA **must measure** first-frame time with new image, existing legacy image, GIF/WebP, valid video, invalid/corrupt media, low-RAM, battery saver, and Android animations disabled; test keyguard opt-in off/on in Creator and privacy in Personal/21-day. Do not promise zero visual delay until tablet evidence. The first presentation of a LEGACY still that has no saved poster may briefly show the safe fallback while its poster is generated off-thread. Live Photos imported as stills remain still; user must supply a supported short video for motion.
- QA **must confirm** that keyguard presentation on the owner's tablet permits an over-lock-screen Activity; OS heads-up notification fallback is not app media playback failure. When Creator opt-in is OFF, private selected images/videos never render over keyguard.
- Do not let Android 15 navigation QA count as video/audio/lock-screen certification.

## Stage 3 — intelligent reminder audio (SOURCE-STAGED; NOT BUILT/TESTED)
- Shared `ReminderBackingMusic` is used by Creator Voice, Personal Voice and 21-day Voice. Music now ducks synchronously to 0.18 app-player gain BEFORE TTS speak, restores to the original 0.68 gain over 420 ms after each sentence, then fades to zero across the last 650 ms of the EXISTING service-owned natural tail. Custom SAF music and offline piano share the exact envelope. No track restarts or system-wide stream volume changes.
- The Creator natural speech tail remains the original 1,500 ms; Personal and 21-day remain 1,900 ms. No playback service extension, lease extension, automatic Done, auto-Snooze or changed retry/notification handoff. TTS errors and explicit Done/Snooze/Dismiss/replacement still stop immediately.
- The Creator Alarm has a scoped MediaPlayer gain envelope: optional TTS gets a 0.42-gain alarm tone while spoken and a 300 ms recovery; alarm tone fades to zero over the last 350 ms before the *existing* watchdog timeout. Vibrations and the audible signal remain otherwise unchanged.
- The Personal Alarm likewise fades within the final 350 ms of its existing maximum ring duration. Neither alarm service changes occurrence validation, completion receipts, foreground notifications, wake locks, actual selected duration or alarm-scheduler behavior.
- The existing 21-day Alarm is Android notification-channel controlled, NOT a dedicated app-owned looping MediaPlayer. It therefore cannot use app-level fading without a separately approved service and Play/Android permission review. This architecture is intentionally unchanged.
- Contract tests for initial gain, strong duck, exact recovery, fade-inside-tail, bounded alarm mix and no device-volume assumptions are SOURCE-STAGED but NOT run.
- Required future QA: voice intelligibility on Creator/Personal/21-day across piano and uploaded music, first syllable, multiple repeats, TTS error, custom audio revocation, concurrent/superseded occurrences, user action during fade, screen locked/other app, alarm optional spoken text, natural duration, exact Snooze, notification response/automatic retry, no truncated speech. Check that 21-day notification alarm remains audible; do not present its channel sound as an app-fade success.
- Policy values are proposed calibration targets. Physical tablet listening and Android runtime tests must confirm that music is neither dominant nor inaudible; no claim of compilation or tested sound yet.

## Next authorized scope
- S4: restore deliberate original Creator setup re-entry and anchor/crop the Home companion avatar. Do not revive a returning-account onboarding loop.
- S5 after all source stages and Founder authorization: frozen SHA source-contract + focused Kotlin compile, then related unit and Android15/owner-tablet tests; repair *only* the failing gate and avoid repeating the long JVM campaign unnecessarily.
- Separate lintVitalAnalyzePersonalStable internal failure must be diagnosed and gated before signed owner-preview/stable promotion.
- No Gradle build, automated tests, APK release, stable promotion or other expensive campaign until the owner explicitly says all source stages are done and authorizes consolidated checks.

## Stage 4 — original Creator setup review and Home avatar (SOURCE-STAGED; NOT BUILT/TESTED)
- Original `V18CreatorOnboarding` five-page screens remain intact for genuine first-run accounts. Added an OPTIONAL `onClose` callback used only by voluntary returning-user review. Existing first-run call provides no onClose and cannot be cancelled. Back moves to the previous step; at first page of manual review it returns to Profile without saving.
- Profile → `REVISIT 5-PAGE CREATOR SETUP` deliberately opens new isolated `POverlay.CREATOR_SETUP`. The old `EDIT CREATOR DETAILS` profile editor stays unchanged. A review starts with saved Creator profile values, and only FINISH calls `CreatorOsSettingsStore.saveCreatorProfileEdits(revised)`. Unlike `completeCreatorSetup`, this existing verified writer preserves account choice, onboarding completion flag, guided-tour state, projects and reminders; failure keeps the review open and displays its existing save error.
- Close/Android Back from voluntary review returns to Profile without changing stored settings. Successful Finish snapshots saved profile and returns to Profile, not to Welcome or a first-run Creator-entry animation. Original P6a first-run routing policy remains UNCHANGED.
- `V18HeroGreetingBar` now uses one top-aligned responsive Row for greeting + companion at every device width and font scale. No more second-row avatar on narrow screens. For standalone legacy Hero callers, only the optional Gallery button may wrap beneath the greeting.
- `V339HomeOverflowMenu` retains the original 48dp menu target, selectable companion identity, activity indicator and every existing action. It now clips a UNIFORMLY scaled top-of-illustration/face-focused crop into the circle, rather than fitting the whole character in the 40dp slot. Uses existing Spark/Mira pose artwork; no new identities or image substitutions. Final crop focal point MUST be checked on real tablet and phone; source implementation alone cannot certify art alignment.
- Changed only two UI entry presentations, original onboarding view's optional manual-close navigation, and the root's Profile overlay route. No backend, auth, reminders, cloud, data reset, account flags or unrelated Home features were changed.
- Targeted QA MUST cover: (a) clean first-run account → companion → five original pages → Home, (b) returning user opening/closing several times without setup relaunch, (c) voluntary review with Back from page 4→3 and Close from page 1, no data mutation on cancel, (d) successful edited weekly target/platform/goal persisted, still no repeat gate after app restart, (e) failed profile save keeps editor open, (f) narrow phone/large font/tablet portrait and landscape top-right avatar geometry, (g) Spark and Mira face crop, menu opening and existing notification/Spark/gallery/profile routes, (h) compact layouts, TalkBack semantics and 48dp touch target.
- All Stage 0–4 changes are SOURCE-STAGED only and explicitly NOT COMPILED, NOT RUNTIME TESTED, NOT APK CERTIFIED. No claim of successful graphic crop before device verification.

## Consolidated QA readiness gate
- With Stages 1–4 source-staged, Stage 5 remains AWAITING explicit founder instruction to begin the agreed consolidated tests. Freeze an exact SHA first; run targeted source/Kotlin checks, then narrow Android15 instrumentation, not full JVM reruns on each failure.
- Inspect overlapping reminders, late/revoked gallery media, Creator opt-in over keyguard, Personal/21-day privacy, all six Voice/Alarm paths in three device states, TTS/music envelopes, natural fade and immediate Snooze/Done action safety.
- Repair the `lintVitalAnalyzePersonalStable` internal analysis blocker narrowly in the separate owner-preview build gate; never claim an unsigned/debug APK is certified stable.
- Preserve verified R2 base `64ced7c21f0de30c82926505985d81ddcaf5a5f3`, rollback references, original Backlot package and all Lab user data throughout.

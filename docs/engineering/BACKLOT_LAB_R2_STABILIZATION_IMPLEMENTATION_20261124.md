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

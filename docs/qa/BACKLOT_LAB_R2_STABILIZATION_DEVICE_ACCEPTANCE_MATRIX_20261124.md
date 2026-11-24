# Backlot Lab R2 — Stage 5/7 Device Acceptance Matrix

Date: 2026-11-24. Status: PLANNED, NOT YET EXECUTED.

## Verified precursors and limits
- Android15 R2 baseline `64ced7c21f0de30c82926505985d81ddcaf5a5f3` passed prior two first-run navigation scenarios.
- Stabilization R2 repaired Kotlin application commit `379a3412ca84bf4fe70671fe14cec0bb56b0d316` PASSED one targeted `compileDebugKotlin -PbacklotLab=true` gate (run `45337219034`).
- Three class-scoped policy unit tests PASSED (run `45340227725`).
- Android15 disposable build/navigation run `45343275501` was started but had not finished when this document was authored.
- Neither a successful compile nor successful first-run emulator navigation proves lock-screen media delivery, audio mixing, video first frame, performance or saved-setting persistence on real devices.
- The first-run Android15 runner intentionally resets only a DISPOSABLE emulator's Lab package; NEVER run `pm clear` on the owner's phone/tablet or install a test APK over valuable owner data without approved upgrade/signature verification.

## Visual/Settings acceptance
1. On fresh Lab user, default icon is Living Energy across Creator/Personal/21-day entrance and main screens.
2. Settings > Personalization > Reminder Background has three differentiated styles: Luminous Glass, Cinematic Emblem, Living Energy. Each shows VOICE mic and ALARM bell. Switching style changes only icon visuals, not selected gallery/family/crop.
3. Style remains saved after exit/relaunch; unknown old saved value falls back to Living Energy.
4. Motion responds to Android reduce-motion/animator-off and battery saver; no unbounded compositor/decoder use.
5. Every built-in family, uploaded still, animated GIF/WebP, video, and supported exported Live Photo should be checked. Videos should immediately display their stored poster and then moving picture, with no 1–2s unrelated family flash on permitted unlocked screens. Existing legacy images without a poster may need one asynchronous first-use generation; record separately rather than passing as zero-flash.
6. Image/video should not reinitialize player between entrance and main, playback remains muted, selected framing preserved; missing/corrupt/revoked files should show a safe fallback and allow response.
7. Creator locked media opt-in is OFF by default; affirmative consent dialog required before enabling. When OFF, private custom Creator gallery not shown on lockscreen. When ON, show it only if OS actually displays Backlot full-screen Activity. Personal and 21-day gallery NEVER shown while locked regardless of Creator choice. Do not expose titles/notes on keyguard through this visual setting.

## Audio acceptance
8. Creator, Personal, 21-day VOICE with default piano and supported custom music: speech clearly dominates music. Mix target linear player gain normal 0.68 / speech 0.18; verify FIRST syllable, repeat transitions, recover between utterances, natural tail and fade. Music does not restart per announcement or alter OS STREAM_MUSIC volume.
9. Speech ends naturally with a complete phrase, post-speech music recovers to normal, then fades before existing service teardown. Test utterance error, slow TTS initialization and missing external music.
10. Creator ALARM optional speech over continuous alarm remains intelligible and audible, vibration remains urgent. Natural max-duration alarm fade occurs within original timeout. Personal ALARM follows same natural-ending principle.
11. Done, Snooze, Dismiss, stale/overlap/duplicate service events and timeout must silence and commit immediately, avoid extra reminder/timer or double notification.
12. **Current architecture exception:** 21-day ALARM sound is managed by Android notification channel, not app MediaPlayer. App-level fade/loop parity is NOT IMPLEMENTED or certified by this stabilization. Keep 21-day alarm visibility, vibration, sound and snooze functioning. A future change requires separate owner approval.

## Six delivery modes × three device states = 18 core conditions
For each VOICE/ALARM of (Creator, Personal, 21-day), validate on owner device with app:
- Foreground, unlocked
- Background while another app is visible
- Keyguard locked (check permissions/full-screen limits and notification fallback)

Log: scheduled vs actual trigger clock time; OS Android/API, exact permissions (notification/FSI/exact alarm/battery optimization); reminder ID/type but NOT personal title/private photo in public logs; audible speech/music/alarm; full screen vs notification; media displayed vs privacy fallback; Done/Snooze/Dismiss; no duplicate alerts; foreground service status; any error/logcat or crash.

## Original setup and Home
13. Fresh install: account choice → companion selection → original FIVE Creator setup pages → Creator Home → Reminder Hub PERSONAL by default. No repeated onboarding after navigation/restart.
14. Returning user: Profile > Revisit 5-Page Creator Setup. Back page 4→3, Exit without saving, Finish updates allowed Creator profile fields ONLY; no reset of account/onboarding flags, no clearing projects/reminders/Spaces.
15. Home header: portrait remains top-right with clear greeting on narrow phone, normal tablet and large accessibility font; Spark and Mira face crops framed correctly without stretch, clipped safely and no navigation collision. Menu/notification/gallery actions still work.

## QA stages and stop conditions
- Gate 1: frozen source integrity and Lab identity.
- Gate 2: targeted Kotlin compile.
- Gate 3: only new three-class policy unit tests.
- Gate 4: one disposable Lab debug + AndroidTest build, two Android15 real first-run navigation attempts; isolate failing job, DO NOT rerun successful earlier stages.
- Gate 5: targeted reminder presentation/device checks (not covered by the first-run navigation test); owner physical tablet evidence.
- Gate 6: personalStable owner-preview Gradle Lint resource failure must be repaired by memory/configuration diagnostics rather than bypassing lint; signer/package/nondebuggable and APK hash verified.
- Gate 7: stable promotion is forbidden until owner accepts EXACT APK and the required no-loss/signing/privacy evidence.

Release STOP if any critical reminder missed or doubled, crash, Snooze/Done incorrect, personal/challenge gallery leaked on keyguard, onboarding loop, data loss, package/signature mismatch, build/lint error, or Live media blocks speech delivery.

This is an acceptance protocol, not a claim that these tests have been executed.

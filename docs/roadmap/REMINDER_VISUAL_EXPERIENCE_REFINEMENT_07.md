# Backlot — Refinement #07: Cinematic Reminder Intro + Main Screen + Notification Identity

**Date:** 2026-11-16  
**Decision:** PROPOSED, awaiting founder review. **No production changes authorized.**  
**Sources:** V337 stable-preserved `678502f4626150ea1945b81eed1593a362377454` plus Android Developers notification/full-screen guidance.  
**Dependency:** #05 retires Premiere visual experience, refines the seven main reminder backdrops; #02/#03 persist selected Spark/Mira; #04 refines spoken personas and five repeat options.  
**Product objective:** One coherent, distinctively premium pattern across **Creator**, **Personal**, and **21-Day Challenge** Voice and Alarm reminders: one of four user-selected intro visual styles leading, on deliberate double-tap, to the main actionable reminder with its independently selected animated background and the actual chosen companion. Also make platform notifications authentic, distinct and readable without breaching Android UI policy or privacy.

## Verified source baseline and gaps

| Route | Actual source | V337 behavior | Gap to proposal |
|---|---|---|---|
| Creator Voice/Alarm | `ReminderCheckInScreen.kt`, `V195ReminderParticleScene.kt` | Fullscreen particle field forms Mic or Clock glyph from particles, **single-tap** to show controls; animated `V391ReminderBackdrop` after reveal; Creator callbacks and audio service independent of visual effect | Add style selector; deliberate double-tap with clear accessible alternative; selected companion in main UI |
| Personal Voice/Alarm | `PersonalAlertActivity.kt`, `V2233PersonalAlertScreen` | Direct actionable reminder with `V391ReminderBackdrop` and `V336DirectorSignalArt`; locked keyguard must never expose private title/notes/actions; optional V394 Premiere variant still exists | Add independent intro only after valid/unlocked personal reminder; preserve privacy and controls |
| 21-Day Challenge | `Phase1HabitLiveDelivery.kt` | Dedicated quiet/alarm/voice notification channels; Android notification opens MainActivity; Voice runs foreground TTS; challenge Done is not inferred from delivery; no shared full-screen double-tap UI | Fullscreen visual entry and safe genuine challenge-specific actions require separate implementation, OS access/policy checks; quiet notifications remain notification-only |
| Main Reminder Scenes | `V391ReminderSceneEngine.kt` | Seven backgrounds shared across Voice/Alarm with independent selections | Keep independent selector, improve authenticity in #05; NOT mixed with intro selector |
| Selected Companion | `V144GuidePrefs`, `V127GuideCompanion` | Spark/Mira selection is persisted, but `V199ReminderSpark` uses original Spark renderer and main Creator controls use mic/clock canvas; not universal chosen-companion presentation | Render persisted actual Spark/Mira art/pose in common main UI, not an invented alternate character |
| Notifications | `ReminderNotifications.kt`, `PersonalReminderNotifications.kt`, `Phase1HabitLiveDelivery.kt`, `V390CompanionNotificationArt.kt` | Creator/Personal small icon uses Backlot monochrome drawable; expanded private notifications can use selected companion as large image; Challenge uses Android generic alarm small icon and selected large icon | Normalize a polished Backlot small-icon family, distinguish types with legible text/category/channel and contextual expanded artwork; preserve OS template and privacy |

**Correction:** User's remembered double-tap is a desired target behavior; Creator V337 has `.clickable(onClick = onReveal)`, hence **single tap**, not double-tap. Personal and challenge parity is not implemented. Do not claim otherwise.

## 1. Unified experience / state model

1. Scheduled occurrence fire is owned by existing reminder engine, not visual animation.
2. Check valid occurrence/lease + full-screen eligibility + locked-screen privacy *before rendering private content*. If challenge/Personal route is merely a notification or is locked, preserve existing safe generic notification/locked experience.
3. Intro visual = user-selected `ReminderIntroStyle`, icon glyph driven by VOICE/ALARM. Full-viewport art; always shows subtle **Double-tap to open** and a separate accessible **Open reminder** control (so TalkBack, motor impaired, one-handed or urgent cases never get stuck). A direct Done/Snooze action in OS notification must remain possible without first seeing intro.
4. On intentional reveal, a bounded 350–500ms crossfade makes main action screen available, with focus transferred to current title/primary action and no invisible hit targets behind intro.
5. Main background = separately selected `V391ReminderScenePrefs.selected(kind)`, conceptually seven visual scenes as refined in #05. Same visual family for all domains, but different domains retain their own action set.
6. Chosen companion = selected `V144GuidePrefs.currentGuide` rendered from approved Spark/Mira art in a quiet bottom/header anchor, with semantic poses: Voice PRESENT/LISTEN, Alarm POINT/ALERT, completed SUCCESS; small enough to never cover title/Done/Snooze/status/replay, reduced motion when needed.
7. Artwork never drives actions. The underlying occurrence can time out, be invalidated, snoozed, completed or cancelled independently. Upon invalidation close/replace the visual surface, never keep an infinite overlay.
8. Audio must start/stop according to existing service policy, not wait for double-tap. If ringing/voice ends or watchdog expires before reveal, preserve pending actionable notification/receipt truth, never invent Done.
9. Global reduced motion / battery saver: static icon, no continuous GPU work; intro can be bypassed immediately via an explicit visible button. Lock-screen privacy and OS restrictions take precedence over animation preferences.

## 2. Four selectable intro animation identities

### A. Original Particle Formation — default / preserved V195

- Visual language: deep burgundy/charcoal with multi-pigment warm ivory, coral, flame, crimson and subdued gold. Thousands of bounded particle points stream and smoothly gather into a truly sampled Mic/Clock silhouette, without opaque icon pasted on.
- Voice: particles settle into microphone grille/pill; Alarm: dots form recognizable clock body/hands.
- Motions: living ambient field, purposeful convergence, relaxed orbital tail; hold until action.
- Status: Current V337 *single-tap* intro; proposed gesture system change only after approval. Default for existing users, safe fallback.

### B. Prism Forge — new option 02

- Art: saturated sapphire, turquoise/cyan, violet and magenta light ribbons collide/refine like faceted glass. The curves resolve into a **physically crisp luminous Mic or Alarm clock outline**, retaining translucent depth and restraint.
- Mic build: vertical multi-layer ribbon wraps cylinder + grille; clock build: ribbons join ring then assemble hands / subtle two bells. Same style, different actual glyph.
- Motion: controlled vector travel, light refraction, precise final lock, delayed floating spectral halo. No generic glitter storm, no copied Backlot welcome frame or excessive bloom over text.
- Stage sequence (conceptual, loop after 2.5s): 0–0.4 deep-color ignition; .4–1.25 ribbons sketch geometry; 1.25–2.0 faceted icon lock; 2.0+ quiet, reversible breathing until reveal.
- Voice palette: cool cyan-violet; Alarm accents: coral/gold brighter but same brand character.
- Risk: GPU and gradient complexity; deterministic cache + reduced-motion static silhouette.

### C. Signal Observatory — new option 03

- Art: deep midnight blue, champagne-gold technical ring calibrations, emerald/cyan pulse-lines and restrained white stars. Premium space-command/control-room identity, not sci-fi clutter.
- Mic build: waveform vectors assemble into finely drawn mic capsule inside rotating orbital dials; Alarm: time-rings lock in a clear clock shape with graceful rotating minute marker.
- Motion: one calibrated sweep, magnetic snap-to-center, slow controlled orbital cues, measured light responses. Emphasize precision, dependability and clarity.
- Stage sequence: 0–0.55 expanding sonar ring; .55–1.5 light lines triangulate; 1.5–2.2 mic/clock precision lock; 2.2+ orbital idle.
- Distinction: geometry and scan/radar craft, NOT glowing neon ribbons or particle confetti.
- Risk: false 'recording' state; visuals are decorative and must never imply microphone audio monitoring or live recording.

### D. Ember Pulse — new option 04

- Art: theatrical black/ink, scarlet, deep tangerine, rose, rich amber and warm gold; cinematic volumetric dust/embers and believable heated-glass light surface.
- Mic build: one molten glimmer rises, melts into outline/grille of a grounded microphone; Alarm: circular amber ring emerges from ember haze and builds clock/bells.
- Motion: warm embers rise and swirl in curved currents, settle as a luminous sculptural glyph, soft heat shimmer/slow flame like a projector, gentle breathing idle.
- Stage sequence: 0–.6 atmospheric embers; .6–1.6 strands coalesce; 1.6–2.3 warm formed icon; 2.3+ slow amber current.
- Distinction: organic atmospheric heat, NOT same particles in red, and no destructive 'fire' connotation for normal personal tasks.
- Risk: contrast, motion/photosensitivity and high particle count; high contrast matte silhouette and saver fallback.

**Important:** Option names are working art-direction labels, not manufactured production images. Refine each with actual phone motion frames, brand/color QA, low-end performance and real user evaluation before locking. Richness must come from light structure, silhouette accuracy, controlled motion and depth—not visual noise.

## 3. Independent Settings selectors and persistence

**Settings → Appearance & Sound → Reminder Scenes**
- **Opening animation** (new) with four cards, actual short video/native preview and selected state; share style for Personal/Creator/Challenge to keep one consistent identity, with separately stored **Voice opening** and **Alarm opening** choices. Factory default for both = Original Particle Formation; older installs do not change visuals on upgrade.
- **Voice reminder background** (existing V391, refined #05).
- **Alarm reminder background** (existing V391, refined #05).
- No revived Premiere / Full-Screen Experience menu or separate visual pipeline.
- Preferences local, offline, stable enum IDs; safe fallback on missing style/asset, no cloud dependency and no destructive migration. User changes take effect on the next actual presentation (not mid-action).
- Screen preview must never dispatch a real OS notification, start a reminder service, acknowledge a reminder, ring an alarm, schedule a task or expose private titles.

## 4. Main reminder screen, domains and companion

**Unifying shell:** category overline (PERSONAL / CREATOR / 21-DAY CHALLENGE), time/occurrence context, selected background, chosen small companion pose in reserved space, title/activity, essential action prompt, 1 primary action, Snooze/appropriate secondary actions, accessible More/Repeat/Reschedule where appropriate. No duplicated payload or burying actions below illustration.

**Creator**
- Keep `ReminderCheckInScreen` primary Stage Done / I'm Working, Snooze, Pause, Reschedule, Replay and Dismiss. Prompt current project next move accurately. Do not auto-approve stage from visual reveal.
**Personal**
- Keep `PersonalAlertActivity` and its token-matched Done/Snooze/Dismiss, title/notes privacy while locked. Do not expose private details or buttons under Android keyguard. No compulsory full-screen for ordinary personal Voice notification.
**21-Day**
- Preserve 21-day challenge's separate `Phase1HabitChallenge` record, original id/day/occurrence and posted notification/voice service. Proposed main screen must offer **Open Challenge / View Progress / Snooze** bound to the real challenge, and **Mark Day Complete** only if explicitly authorized and implemented using the originating challenge's verified mutation path; never mark it Done due to intro tap or notification click. No silent new independent reminder or duplicate schedule.
- Distinguish quiet/voice/alarm eligibility; genuine full-screen only when Android/Play rules permit user-enabled high-priority alarm use.

**Selected companion identity:** Spark and Mira from approved V144 character assets; do not show Spark by default if Mira was chosen. Gesture/action/pose cues should represent Voice/Alarm type without faking actual audio-reactive bars. Avoid full-size companion behind text/buttons or visually replacing the actual central mic/alarm icon in the intro.

## 5. Notification/icon specification

- Notification **small icon**: must be an Android-compatible monochrome high-contrast transparent silhouette; test 18–24dp density and manufacturer rendering. Default current Backlot icon for Creator/Personal is working; propose one cohesive category family (Creator: small clapper/check-in motif; Personal: heart/bell; 21-Day: calendar ring/tick), with Voice/Alarm differentiated primarily in text/channel and optional private large content. **No colored/animated status-bar icons**.
- Challenge currently uses `android.R.drawable.ic_lock_idle_alarm` while Creator and Personal use `backlot_v198_notification_monochrome`; replace generic Challenge icon only with properly tested Backlot family, not arbitrarily with full-color logo.
- Preserve **expanded private large icon** already showing selected companion from `V390CompanionNotificationArt` on all domains. Ensure appropriate icon shape and quality; test missing resource fallback. Android generally controls expanded presentation and may vary by OEM.
- Show accurate domain type, title/summary and actionable buttons where truthful. Preserve personal/challenge generic redaction on the lock screen. Update Creator privacy only after policy review.
- Platform's full-screen intent access restricted from Android 14; only user-authorized high-priority alarm use should attempt it. Full-screen notifications are not promised for all reminder types.
- Official Android references: https://developer.android.com/about/versions/14/behavior-changes-14 ; https://developer.android.com/develop/ui/compose/notifications ; https://developer.android.com/design/ui/mobile/guides/home-screen/notifications .

## 6. Critical trade-off: double-tap versus alarm safety

- Founder wants until **double-tap**. In current V195 the event is `clickable(onClick=onReveal)` and requires only a single tap. Implementing true double-tap must be a **separate, tested UI policy**, not a cosmetic copy change.
- A required double-tap to access Snooze/Done has user-error, accessibility and emergency-alert costs. Recommended: allow **double-tap on open artwork** plus one distinct visible **Open reminder** button (single-tap). On TalkBack, normal focused click activation should work without a custom timed gesture conflict. Keep OS notification action shortcuts usable directly.
- If the user explicitly wants no visible single-tap alternative, record a product/accessibility exception for founder review before release, not an assumed harmless choice.
- During intro, audio is already driven by reminder service, not gated by user action. Do not loop CPU/GPU indefinitely if unattended; use controlled low-frequency idle and Android's existing notification/watchdog. External invalidation tears down intro; OS safety and privacy win over art.
- The chosen animation is not a second 'Premiere' mode. It becomes the one native visual intro integrated into the existing reminder surfaces.

## 7. Delivery plan

1. **Baseline matrix:** test current Creator/Personal/21-Day voice/alarm/quiet, locked/unlocked paths, original sound, gesture, large/small notification icon and companion preference on disposable devices. Source audit alone is not real-device certification.
2. **Art lock:** illustrated seven-state motion frames, mic+alarm variants for each of three new concepts, portrait/tablet crops, static fallback, A/B contrast and motion-performance budget. Founder approval for final visuals.
3. **Native intro controller:** one reusable style renderer/transition and explicit accessible gesture policy, but separate domain action controllers.
4. **Personal+Challenge integration:** preserve token/occurrence, privacy, challenge voice, alarm entitlement, correct actions and errors. No auto-alarm for quiet/voice types.
5. **Companion integration:** use V144 persisted Spark/Mira, approved art/poses, no obscured primary action.
6. **Notification icon pass:** small monochrome resources with correct OS template/large chosen art, test active notification channels and locked-screen redaction.
7. **Settings:** four previewable intro styles + independent Voice/Alarm choice; separately retain main backgrounds; remove duplicate Premiere path as #05 already plans.
8. **QA and release:** deterministic intro/reveal, Voice/Alarm playback continuing, snooze/Done/invalid token/stale occurrence, long titles, 320dp phones, tablets, landscape, 200% font, TalkBack, reduced motion, battery saver, multiple simultaneous reminders, Android 11–16 targeted, locked/unlocked/privacy, old preference migration, app process kill, low-memory frames, and full reminder regression. Test with physical signed candidate only after separate implementation approval.

## 8. Acceptance criteria

- Existing original remains default on upgrade; three genuinely different native intro choices, each with clear Mic vs Alarm silhouette.
- Creator/Personal/21-Day eligible full-screen experiences are consistent when actually presented; no claim of automatic full screen for quiet/voice when OS disallows.
- Selected Intro and Main Background preferences independent and persistent; Voice vs Alarm choice independent where requested.
- Main reminder uses chosen companion (Spark or Mira), not hard-coded Spark; image/pose consistency validated.
- Double-tap works as designed with accessible single-tap button and system TalkBack activation, no accidental dismiss.
- Existing callbacks and occurrence identities untouched; Snooze, Done, Stage Done, Replay, service stop, timeout, missed/follow-up all verified.
- Notifications use OS-correct monochrome small icons, accurate channel/content, meaningful private large character art, no personal/challenge content leaks on lock screen.
- Low motion, battery, GPU and app longevity safe; fallback to static for failed animation; intro never causes missed reminder.
- Stable V337 and authorized existing design decisions remain intact; no merge/release without founder final acceptance.

**Status:** Proposed, source-audited, not rendered as production-quality animations, not implemented/tested. Preserve stable baseline.

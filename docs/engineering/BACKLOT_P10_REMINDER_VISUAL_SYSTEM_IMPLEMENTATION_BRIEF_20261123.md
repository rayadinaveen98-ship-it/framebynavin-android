# Backlot P10 — Unified Living Reminder Experience
## Owner-approved product/design/engineering brief · 2026-11-23

**Authorization:** The founder rejected every older/default reminder main-screen look and obsolete reminder-scene/voice/alarm/Premiere full-screen collection; approved preserving Living Bloom, Pulse Studio and Rise Core as the only three built-in entrance families, making them visibly more alive, redesigning mic/alarm iconography, redesigning the post-double-tap main screen, and enhancing gallery image/animated photo/video customization. Founder said “yeah ok bro continue” to the implementation-brief and isolated source-staging plan.

**Status:** P10A/P10B foundation **SOURCE-STAGED**; P10C Creator, Personal and valid notification-opened 21-Day entrance/main visual cutover **SOURCE-STAGED BUT UNCOMPILED AND UNTESTED** on a new descendant branch. P10D gallery and built-in picker polish plus media/main continuity, and P10E old code/resource removal **NOT STARTED**. No compile, Gradle, JVM, instrumented run, CI, emulator, device test, merge or APK is permitted until the founder later explicitly opens the consolidated testing gate.

### Source of truth and non-negotiable boundaries

- Repository: `rayadinaveen98-ship-it/framebynavin-android`
- Parent: `feature/backlot-v337-point09b-settings-interiors-source-20261123` (P1–P9B cumulative staging; **not** default main).
- Current isolated P10 branch: `feature/backlot-v337-point10a-reminder-visual-foundation-after-p9b-20261123`.
- Owner accepted V337 baseline: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Keep main, V337, signing, data, backend and hosted services untouched.
- Creator OS first. Personal OS conveys warmth and support; 21-Day remains its existing habit/notification engine. No separate navigation, account, cloud, subscription, mic or alarm owner.
- Do NOT remove the **app-wide** Director's Cut dark theme or Backlot Light theme; the ban applies to retired *reminder presentation* visuals, not app theme.
- Do NOT remove Creator/Personal/21-Day scopes, VOICE/ALARM controls, reminder creation/editing, scheduling, playback, notifications, exact occurrence validation, Done/Working/Snooze/Dismiss/Replay/Reschedule/Pause, advanced accessibility, media crop/reset or privacy protections.

## Honest audit of the current P9B candidate

1. Creator: `ReminderCheckInScreen.kt` intro still uses `V195ReminderParticleScene` with old dotted mic/clock that is **single tap** to reveal, despite the desired double-tap journey. On reveal, `V336DirectorBackdrop` and `V336DirectorSignalArt` provide the main screen.
2. Personal: `personal/PersonalAlertActivity.kt` uses `P1PersonalGalleryEntrance` only when a valid gallery item is selected and the device is unlocked; that entrance is double tap. Without gallery item it shows `V2233PersonalAlertScreen` immediately, with `V336DirectorBackdrop` and `V2233PersonalParticleWave`. Personal ALARM is permitted to open full-screen under existing Android policy; VOICE and NOTIFICATION must not be granted that capability.
3. 21-Day: `P1ChallengeNotificationEntrance` exists only after a notification route, current/unlocked validation, and selected gallery media. It does not create a universal real full-screen alarm route; do not fabricate one. No replay or action ownership changes.
4. `P1EntranceBackdrop.kt` provides app-private photo/animated WebP/GIF/HEIF/still/video media with 15s/40 MiB limits and crop/framing. Video is muted and stops/releases on lifecycle changes. Locked device uses a neutral fallback.
5. `V391ReminderSceneEngine.kt` has old theme choices and independent voice/alarm selectors; `V394PremiereExperience.kt` carries deprecated opt-in modes and full-screen renderers. P9 deactivated active settings call sites, but these Kotlin classes and stored keys remain.
6. `P1GalleryBackgroundSettings.kt` P9B currently presents "Backlot Original" vs gallery; the approved three built-in family choices are **not yet surfaced in this source path**. Do not tell the founder they're already integrated.
7. Public-facing legacy labels/text, static fallback, main-stage hierarchy and icon treatment are still mixed era. Repair by one replacement system, not another overlaid renderer.

## Product contract: one experience, two acts

### Act I — entrance
- Available built-ins (exact public names): **Living Bloom**, **Pulse Studio**, **Rise Core**. All available to Creator, Personal and 21-Day, with sensible initial suggestions (Creator: Pulse Studio; Personal: Living Bloom; 21-Day: Rise Core). No prescribed lock-in.
- Optional local media background remains an alternative: photo/HEIF/GIF/animated WebP/silent video; selection and crop are per scope.
- Entrance has a domain label, a meaningful high-quality Voice/Alarm identity glyph, one primary title (privacy-gated), progressive living background, subtle affordance reading "Double tap to open", and a single well-defined full-screen touch target.
- A double tap opens Act II with the same family and mood. On screen readers or hardware accessibility actions, the semantic **Open reminder** action works on a single explicit activation; no forced timed reveal.
- Creator will require replacement of its current **single-tap** gesture. Do not quietly claim it already matches this design.
- No interactive controls should be concealed behind a permanent animation layer.
- Real OS animation-off / reduced-motion modes show the same new composition statically and allow immediate access to actions; do NOT resurrect V336 as a fallback.

### Act II — main action screen after entrance
- Continue the *same* family, palette, and visual rhythm; transition focus from expressive centerpiece to readable task and actions. Never switch to old Director/Premiere/backdrop after the tap.
- Maintain typography hierarchy: identity strip/time, reminder heading, short context/next action, ONE dominant completion/working action, prominent Snooze, lower-priority More Options (retaining current conditional actions).
- Replace old V336 icon render and stacked panel aesthetics with low-contrast visual depth, composited translucent stage, stronger but non-distracting kinetic atmosphere. Main-stage animation remains active in subtle range, not fully static.
- No new business semantics: Stage Done means existing stage check-in, Working means existing response, Done means existing Personal response. No auto-completion and no new automatic alarm scheduling.
- On short screens or large font: vertical scrolling without controls offscreen or text truncation obscuring a required action. Main-stage typography >= system-scalable, minimum 48dp actionable touch targets.

## Visual language and motion specification

### Living Bloom — organic kinetic light
A layered living core (not a static expanding circle): three offset petal/ribbon fields flowing in a slow nonrepeating orbit, one breathing light nucleus, 8–18 restrained depth motes. Visual evolution should be visible in a 3-second observation, yet not flicker or strobe. Warm highlights and rich shadow separation; avoid copy-pasting pulsing rings as the only motion.

### Pulse Studio — creative focus, alive
Kinetic frequency ribbons / concentric asymmetrical arcs, subtle parallax and traveling studio lights. Movement is present *while idle*; real microphone amplitude may influence energy only when explicitly available and consented. **Do not fake audio-reactive signals** from a reminder that is only being announced. The voice identity is visual, not a hidden recording session.

### Rise Core — upward momentum
A rising luminous spine with layered atmospheric wisps and independent micro-ribbons, a slow opposing drift and controlled brighter highlight on reveal. Not the retired Personal Sunrise scene. No historical projector/light/aperture templates are reused.

### Motion quality
- Active motion must feel visibly living, not a barely moving wallpaper. Design-review targets: center silhouette changes, depth motion and secondary light travel are discernible over 3 seconds on a normal phone.
- Target bounded, frame-clock-driven rendering; cap normal update rates around 30fps and power saver ~20fps. Use deterministic geometry and precomputed objects rather than per-frame random lists, blur passes or media decoders.
- User's animator-off / Android accessibility preference: no continuous animation, meaningful static composition, controls available. Low power/low RAM: fewer layers/particles, lower frame cadence while preserving identity and gesture.
- Avoid high-frequency brightness flashing, busy behind-text movement, prolonged entrances, loud surprise sound, sudden camera jumps or motion that could disorient. No motion tied to reminder scheduling engine.

## Premium icon system
- Retire old dotted microphone and clock silhouettes plus old V336 signal art from live routes.
- Voice: custom high-contrast capsule/signal emblem built as coherent native vector geometry, not a stock outline icon pasted on top; show semantically as "Voice reminder".
- Alarm: similarly authored radial time/alert emblem with deliberate optical weight, not generic Clock/Mic badges.
- Both adapt to family palette but retain same geometry in all scopes; avoid fake recording/listening indicator or mic permissions.
- Icons must read at small screen sizes, across custom media and without animations, with screen-reader descriptions.

## Gallery / local media enhancement
- Reuse **only** `P1EntranceMediaStore` and `P1EntranceBackdrop` for storage and decoding. No new media ownership, network upload or gallery scanning.
- The media fills the full scene; cinematic edge vignette, content-aware contrast protection, animated family-colored accent, and safe crop/position all stay presentation only.
- Silent video remains short/looping and lifecycle-bound. Failure, power saver or unreadable file => a static/new built-in P10 fallback, never an old V336 scene, never blank alert.
- Strong lock-screen privacy: Personal media/title only after unlock; challenge media never auto-displayed locked. Creator unlocked custom media only when allowed by the existing gate.
- Settings: built-in family picker + local gallery in one page, per-scope indicator, immediate full-screen preview, Voice/Alarm preview state, clear restore/reset; no old "Original" collection and no duplicate editor.
- Motion photo formats depend on Android support; do not promise iOS-style Live Photo movement without verified conversion/import.

## Removal and cleanup inventory

**Required cutover from active routes:** `V195ReminderParticleScene` (Creator old dotted formation/single-tap), `V336DirectorBackdrop`, `V336DirectorSignalArt` and `V2233PersonalParticleWave` (post-entrance look); replace with P10 family renderer, new icons and unified phase handling. P1 media renderer remains but its *old neutral fallback* must be replaced with new static P10 atmosphere.

**Retire old UI options:** `V391ReminderSceneSettings`, old voice/alarm background choices (including aperture, projector, sunrise, etc.); `V394PremiereSettingsSection` and legacy experimental full-screen opt-in. No preview or deep link should expose those designs.

**Physical source deletion gate:** Once all producers/consumers, backups, tests, resources and references are inventoried, remove unreferenced V391/V394 renderers, V336 visual code and old preview assets in a separate documented cleanup commit. Reconcile any nonvisual references before deletion. Leave persisted V391/V394 keys intact (ignored, not displayed); deleting their storage without migration could corrupt recovery/backup and is not implied by visual removal. Verify no old visual strings/resources ship in signed APK at final QA gate.

## Implementation packages / acceptance

- **P10A — source foundation:** Create typed family selection and motion policy with safe storage; record precise source map. No production rendering route altered; source stage only.
- **P10B — new entrance visual:** Native living family renderer + authored Voice/Alarm vector marks + meaningful movement, replace Creator old entrance and unify double-tap accessibility semantics. Update unlocked Personal / valid challenge hosts with the same renderer, respecting separate launch rules.
- **P10C — new post-open main:** Replace Creator and Personal V336 backgrounds and center art; maintain complete callbacks, privacy and responsive interaction. For 21-Day, retain real notification and normal challenge destination unless a distinct owner-approved challenge action screen already exists.
- **P10D — gallery polish:** Enhance media compositor without replacing P1 storage, scoped preview and full-screen transitions; no old scenes in reset/fallback.
- **P10E — retire old assets/code:** Delete old renderers and settings only after verified references/compilation source inspection; preserve historical keys as inert, and document removals.
- **P10F — consolidation QA gate:** On explicit founder "done, start next steps and give APK" command only: compile incremental and focused, resolve exact errors (do not restart full JVM if avoidable), then integration tests, real device, security/privacy, performance, accessibility, signed owner preview. This is deferred.

### Nonnegotiable QA observations
- No Creator one-tap bypass on fresh entrance. Proper TalkBack semantic action.
- Returning saved settings unaffected; no accidental reset/overwrite.
- Both Creator ALARM and VOICE; locked/unlocked Personal ALARM with no private data; Personal VOICE notification behavior not escalated; invalid/stale occurrence actions disabled; 21-day time validation and no unwanted full-screen grant.
- Act I and Act II use the same family (and selection persists), gallery crops persist, no click-through, no double receipt, no blank screen.
- All Done/Working/Snooze/Replay/Reschedule/Pause callbacks unchanged and reachable.
- Video decoders release on stop; notification voices and music unaffected; use photo fallback on video decode errors.
- 320dp/360dp/420dp/large screen/tablet, font scale 1/1.5/2x, TalkBack, reduced motion, battery saver, lifecycle/rotation and process death; headless code review is not proof of any of these.

**Important:** P10 cannot honestly be called "complete" until P10E deletion AND founder-accepted physical APK tests. A committed source stage is not an implemented or verified product.


## P10A / early P10B implementation ledger (source only)

- `P10ReminderVisualContract.kt`: three named families, independent per-scope local selection store, first-run suggested family and Act I/II tuning; retired identifiers never become selectable family IDs. Motion-off is static in the *new* system rather than an old-scene fallback.
- `P10LivingReminderVisuals.kt`: three distinct native Canvas render paths (organic layered Bloom ribbons, asymmetric Studio arcs/frequency lanes, and vertically rising Core ribbons), separately bounded depth motes, main-act reading scrim, and authored capsule-voice/radial-alarm geometry. There is no actual microphone recording or fabricated VU-level response.
- `P10ReminderPresentationHosts.kt`: staged domain-aware entrance with image/video underlay reuse, no media when locked, touch double tap and accessibility explicit open. All private title display is lock-gated. A phase-two atmosphere helper is available for *later* alert action-owner migration.
- `P10ReminderVisualContractTest.kt`: staged unexecuted test source for family exclusivity, independent defaults, legacy suppression, animation-off, power reduction, lock privacy and deliberate open.
- P10 source is **not yet mounted** in `ReminderCheckInScreen.kt`, `PersonalAlertActivity.kt` or `P1ChallengeNotificationEntrance`. The old active Creator/Personal UIs therefore still render on this branch. This is intended temporary source staging, not a completed release.
- P10 gallery live-media decode fallback still needs P10D replacement. The old `V391/V394/V336/V195` source is not deleted yet. Never report it removed.
- During source review a locked-entrance gesture issue was corrected: touch double-tap and TalkBack activation both refuse reveal while locked; `pointerInput` is keyed to current lock state and captures the newest open callback.
- Source review is **not** Kotlin compilation, automated tests, a device screenshot, performance profiling or owner aesthetic acceptance. All remain deferred.


## P10C source-stage continuation (same founder-approved scope)
- P10C descendant branch: `feature/backlot-v337-point10c-live-screen-source-after-p10b-20261123`.
- Added `P10ReminderActionPalette.kt` and `P10PersonalReminderExperience.kt`.
- `ReminderCheckInScreen.kt` Creator's active entrance/main switched from old V195/V336 renderers to P10 family, with touch double tap and accessible open; stage and response callbacks unchanged. A single editorial context stage replaces two old stacked panels.
- `PersonalAlertActivity.kt` now uses P10 double-tap entry for validated Personal reminders and the new main backdrop/icon/palette plus one information stage; its action owner and lock privacy policy are unchanged.
- `P1ChallengeNotificationEntrance` now shows the P10 entrance after an existing **user-opened valid challenge notification**, with or without gallery media; it does not schedule automatic full-screen challenge alerts. Opening returns to the existing challenge destination.
- Creator lock-screen response availability is explicitly preserved from V337; only private gallery imagery is hidden until unlocked. Personal remains stricter.
- Stage record and exact unfinished items: `docs/engineering/BACKLOT_P10C_LIVE_REMINDER_VISUAL_CUTOVER_SOURCE_STAGE_20261123.md`.
- P10D/E remain pending, especially P1 old gallery fallback, family selector UI and physically deleting retired styles. The historical statement above that P10 source was 'not mounted' describes the earlier P10A checkpoint and is superseded by this P10C section.

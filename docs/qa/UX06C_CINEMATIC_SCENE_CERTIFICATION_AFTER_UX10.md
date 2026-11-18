# UX06C — Cinematic Scene Certification Matrix (NOT EXECUTED)

**Status:** deferred to the owner-approved consolidated test campaign after UX10.
**Parent:** UX06B draft #405, exact SHA a81baf0b6cd01758de599e868ce958d918252064.
**Protected stable:** V337, unchanged.
**Do not treat source inspection, file names, visual intent or manually authored tests as proof of a passing runtime test.**

## Static integration contract

- V391ReminderScene classic IDs kept exactly: DIRECTOR, AURORA_SILK, LIQUID_CHROME, CELESTIAL_DRIFT, CRIMSON_PULSE, GLASSWAVE, EMBER_FIELD.
- New IDs appended: CINEMATIC_APERTURE, PERSONAL_SUNRISE, ATMOSPHERIC_PARTICLES, PROJECTOR_LIGHT, GLOWING_RINGS.
- Only new curated entries route to V406CinematicReminderBackdrop; all original scene drawing code remains as before.
- Existing two independent preference keys `voice` and `alarm` stay in `backlot_reminder_scenes_v391`; absent/unknown ID falls back to DIRECTOR, no migration or renaming.
- Creator + Personal classic live surfaces use same existing V391ReminderBackdrop. Premiere enabled means existing V394 alternative, no new double Canvas/composition.
- Current locked Personal screen, Challenge preview-only and Idea notification behavior remain unchanged.
- Native scenes are self-contained draw-only Compose Canvas and do not call any Android or domain reminder services.
- All animated visual components must freeze when animations disabled or battery saver, and cap draw workload.
- Existing V391 kind/scene selection test tags preserved; new collection groups and selection states are additional visual metadata only.

## Post-UX10 test scenarios

| ID | Scenario | Acceptance result |
|---|---|---|
| C01 | Upgrade from V337 with saved Director voice & Glasswave alarm | Exact respective names and choices survive, no preference overwrite |
| C02 | Upgrade with every one of seven existing saved Classic names | All parse and render unchanged |
| C03 | Unknown / corrupt saved scene value | Safely falls back to Director; no crash or destructive rewrite |
| C04 | Fresh install without scene preferences | Voice and Alarm both default Director; no unexpected Premiere enable |
| C05 | Choose each of five Cinematic scenes for Voice | Saved enum name exactly matches selected; return/restart persists |
| C06 | Choose each of five Cinematic scenes for Alarm | Independent Alarm selection never changes Voice |
| C07 | Voice scene change after Alarm choice | Alarm choice unchanged; no new scheduled alarm |
| C08 | Scene selection while Creator Voice alert is already open | Correct next-frame presentation without TTS interruption or duplicate response |
| C09 | Scene selection while Personal alert is already open | Correct presentation, no completion or snooze |
| C10 | Classic renderer after choosing curated and switching back | All seven original backgrounds still reachable and distinct |
| C11 | Opt-in Premiere on for Creator Voice/Alarm | Existing V394 presentation takes priority, selected background remains saved |
| C12 | Opt-in Premiere on for unlocked Personal Voice/Alarm | V394 eligible only; locked privacy preserved |
| C13 | Disable Premiere after choosing curated scene | Selected curated scene appears without re-selecting or overwriting |
| C14 | Locked Personal reminder with selected Cinematic scene | No private title, notes or actions exposed above keyguard |
| C15 | 21-day Challenge Voice/Alarm | Does NOT suddenly trigger a full-screen cinematic screen; existing notification remains |
| C16 | Idea reminder | No new full-screen or unwanted animation, notification actions unchanged |
| C17 | System animator scale disabled | Static image, immediate controls, no animation requirement for Snooze/Done |
| C18 | Android battery saver on | Static frame and reduced particles/beams/rings, no runaway frame callbacks |
| C19 | Long Creator/Personal Voice with music | Canvas does not alter audio focus, volume, loops, service lifetime or watchdog |
| C20 | Alarm and ringing while device locked and screen rotated | Original response and occurrence token, no new accessibility focus trap |
| C21 | Small screen 320dp, 200% font scale, landscape | Controls reachable, backgrounds low contrast where text appears |
| C22 | Tablet and large screen | No giant geometry, cropped visual subject or obscured action |
| C23 | TalkBack and touch exploration | Scene is decorative, selection buttons expose selected state; all alert actions reachable |
| C24 | Reduced-contrast/light appearance | Still adequate readable contrast; selected state perceivable without color |
| C25 | Rapid 12-scene selections and process recreation | No crash, wrong persisted enum, frame stall or leaked coroutine |
| C26 | Continuous animated reminder foreground | Record CPU/GPU/jank, memory, heat and battery; enforce bounded budget |
| C27 | Dual reminders / duplicate Broadcasts | No additional schedules, receipts or audio created by visual selection |
| C28 | Reset/restore older backup with Classic values | Settings preference continuity; canonical reminder/achievement records unchanged |
| C29 | Native preview of all five scenes | Correct unique artwork at 136dp preview, static in low-power preview |
| C30 | Full-screen actual render on Android 13–16 supported devices | Distinct blade/sun/motes/beam/arcs, no FPS regression, no black or clipped screens |

## Certification evidence

- Exact cumulative build SHA and the GitHub Actions/Gradle/test artifacts (not generated during this stage).
- Real Android captures per scene in Voice and Alarm, Creator and Personal, Classic and Premiere priority.
- Pixel/visual UX inspection of legibility, scale, animation, 320dp/large-font and tablet layouts.
- Android profiling FPS, frame time, memory, battery saver and static motion-disabled states.
- State/backup fixtures showing Voice/Alarm preference names survived upgrade, restore and process recreation.
- Regression records for R01–R35 from UX06A and B01–B25 from UX06B.
- Owner physical acceptance for presentation quality plus QA/release gating.

**Do not certify UX06C until C01–C30 have test evidence. The source code is proposed integration, not a verified APK.**

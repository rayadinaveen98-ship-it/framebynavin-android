# RX01-B — Premium Optical Icons and Entrance Hero

**Status:** SOURCE-STAGED only on a stacked DRAFT PR. No integration, unit/JVM tests, Kotlin compilation, Android lint, emulator, APK, or physical acceptance has been executed for RX01-A/B. Source-stage B must not change the founder-accepted \`main\` or stable head.

**Ancestry:** based on RX01-A exact commit \`2bf2b065308e953d723c5abccd9e251d652284e0\`, which itself descends without divergence from frozen source \`abbe88984bac16cfafbd990d9cb00ed54cfdd07e\`.

## Requested / approved design

Three domain-specific entrances over the separately staged RX01-A fluid light backgrounds, with a central original premium microphone or alarm icon. An honest combined symbol may appear **only if a domain source owner verifies both Voice and Alarm belong to the same occurrence**. The graphics must match the fluid palette and lead into a calmer, different main reminder background only after a later valid double-tap.

- **Creator / Pulse Studio:** illuminated metal and cobalt/cyan optical arcs; strong mic/clock silhouettes, purposeful lateral glints.
- **Personal / Living Bloom:** soft pearl lens with warm luminous edges, quiet curved arcs and silk-like light; same high-contrast glyph silhouettes.
- **21-Day Challenge / Rise Core:** ember-core glass with three symbolic energy arcs and an amber highlight. **These are NOT daily confirmations, streaks, or a completion ring.**

### Source artifacts

1. \`app/src/main/java/com/framebynavin/app/reminders/RX01ReminderOpticHero.kt\`: domain+mode policy, original Canvas mic and clock glyphs, a combined optic, adaptive halo and glints, safe static fallback, non-clickable entrance hero with domain label and future instruction.
2. This design/engineering acceptance brief.

Code reuses the **six approved RX01-A palettes**, via \`RX01FluidTheme.palette(domain, ENTRANCE)\`. No duplicate colour preference store is created. All icons and ornamentation use native Compose draw operations; no Rive, Lottie, video file, external asset or paid service is required.

## Mode integrity (must be maintained in subsequent stages)

- \`RX01ReminderIconPolicy.creator(ReminderCheckInKind)\` derives Creator Voice or Alarm directly from the existing canonical kind.
- \`RX01ReminderIconPolicy.verified(voice, alarm, combinedModeVerified)\` fails if neither signal is present or if both are claimed without an explicit same-occurrence verification flag.
- Personal **Voice** is currently notification-only; depicting a mic never grants a full-screen launch. Music is NOT a second Alarm signal.
- Single-activity Challenge modes must be derived from \`Phase1Habit\`'s actual verified saved alert type, and not from the presence of piano or custom music. No 21-day auto-Done or new notification actions.
- No persistence of icon selection is introduced. The combined mode is allowed in the reusable visual component but **not activated in any live reminder owner** by RX01-B.

## Layout and accessibility

- Hero uses \`WindowInsets.safeDrawing\`, 20dp horizontal safe gutter and compact 130/168dp optical sizes for short/narrow displays; 218dp on larger screens.
- Entrance title is a generic category label; it never displays private Personal titles, notes, photos, Google account data, or saved progress. The future owner-scoped main screen must preserve the existing Keyguard privacy barrier.
- The optic exposes only a genuine semantic name ("Voice reminder", "Alarm reminder" or verified "Voice and alarm reminder"). Light rings, glints and clock hands are decorative; no fabricated audio input, countdown, reminder delivery, user acknowledgement or streak signal.
- The hero includes the planned prompt **DOUBLE TAP TO OPEN** but deliberately contains **NO gesture handler** until RX01-C. TalkBack activation of the later combined entrance surface must remain a standard, documented accessible action, not a mandatory custom rapid-tap gesture.
- No companion face is drawn over the optic. Existing optional selected Spark/Mira appearance remains preserved for the later live/main integration. The hero's category label is generic even on locked Personal.

## Native motion and bounded cost

- Original Canvas optics: no shaders, bitmap blur, background images, external fonts, audio capture, sensors, hidden mic permission or postprocessing.
- A bounded, deterministic 6s/8s/7s icon breath cycle for Creator/Personal/Challenge. Only light radii and small glints move; glyph silhouette and clock hands remain stationary.
- \`motionEnabled=false\` or \`lowPower=true\` selects a deterministic still frame with no infinite transition. Future RX01-C/D/E must supply correct active lifecycle, Animator scale/battery saver/reduce-motion inputs.
- Entrance frame may be vibrant; the already-staged RX01-A **MAIN** mode is a separate dark/readable palette for buttons and notes. Do not put the entrance optic over essential main-screen actions.

## Strict separation of responsibilities

\`\`\`mermaid
flowchart TD
    A[Original reminder source verifies current occurrence] --> B[Owner resolves verified domain and signal kind]
    B --> C[RX01-A fluid entrance underlay]
    B --> D[RX01-B read-only optic and generic label]
    C --> E[RX01-C gesture / accessibility gate - NOT YET STAGED]
    D --> E
    E --> F[Original live MAIN controls over RX01-A calm main art - FUTURE]
    F --> G[Original service and source owner remains sole Done/Snooze/response writer]
\`\`\`

This PR deliberately has **no calls** from \`AlarmActivity\`, \`VoiceReminderActivity\`, \`PersonalAlertActivity\`, \`Phase1HabitLiveDelivery\`, \`ReminderCheckInScreen\`, Android receivers, WorkManager or Settings. No manifest, saved preferences, permission, cloud data, server, account, UX09 or protected stable branch changes.

## Deferred post-'all done' certification

1. Compile each Canvas and Compose call on the integrated Kotlin/Compose version; check Android API26 fallback and API35/36 behavior.
2. Verify all 3 domain × 3 signal symbol combinations on visible backgrounds, including safe label/cutout alignment at 320dp and 200% system font, landscape and tablet.
3. Measure actual icon motion, no flashes, no battery-saver animation and no leaked render loops when the Activity is stopped.
4. Ensure visual source never reads microphone, media level or audio recorder and never claims the user saw/heard/completed a notification.
5. Prove no Voice+Alarm icon appears without real same-occurrence ownership verification. Add negative tests for neither signal and unverified combined mode.
6. Ensure live Creator/Personal/21-day action controls, full-screen entitlement, lock-screen privacy, Snooze, voice, music, restore and completion ledgers remain owned by the original modules.
7. Confirm future double-tap gate is accessible, does not require an invisible main button, and the original notification remains actionable if Android refuses full-screen presentation.

## Next stage

**RX01-C:** a one-way occurrence-scoped two-tap transition container with TalkBack/switch-access equivalence, stale occurrence and lifecycle safeguards, and a calm separate MAIN background. Source-stage it separately, then proceed to D/E/F in founder-approved order. Do not integrate, run the final test campaign, deliver an APK or promote any branch until the founder explicitly says **ALL DONE**.

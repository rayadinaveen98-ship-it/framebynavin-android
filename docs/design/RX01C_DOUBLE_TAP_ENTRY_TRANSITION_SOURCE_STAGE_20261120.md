# RX01-C — Double-tap entrance gate and independent main reminder scene
**Date:** 2026-11-20  
**Status:** SOURCE-STAGED ONLY, draft/unmerged. No Kotlin/Gradle/lint/JVM/emulator/physical tests; no APK or main/stable changes. Deferred integration follows the founder's explicit **ALL DONE** instruction.

## Authority and ancestry

- Founder-approved new stable/main both at \`abbe88984bac16cfafbd990d9cb00ed54cfdd07e\`.
- RX01-A source: PR #448, \`2bf2b065308e953d723c5abccd9e251d652284e0\`: fluid entrance and six independent domain/phase palettes.
- RX01-B source: PR #449, \`5c407c1bf17b707a746f37d2ad744d150fb30b51\`: premium verified mic/alarm/combined optics and visual-only entrance hero.
- RX01-C stacks on B and adds a **single independent Kotlin UI/state-machine file**, \`app/src/main/java/com/framebynavin/app/reminders/RX01ReminderExperienceGate.kt\`, plus this acceptance contract.
- No pre-existing Creator, Personal, Challenge, notification, scheduler, receiver, audio, security, store, Settings, cloud, Manifest, UX09 or production source files are modified.

## Approved interaction

The alarm/voice is delivered using its original owner; once a **verified, permitted Activity** opens, the user sees a full-screen, vibrant domain entrance and central mode-aware optic with **DOUBLE TAP TO OPEN**. A **single ordinary touch tap must do nothing**. Two ordinary touch taps in the same Android-recognized double-tap gesture open the main content after a premium, subtle ~320ms expand/fade transition. The main screen uses the independently coloured, **quieter MAIN** gradient; it is not the entrance replayed behind text. No auto-advance on any animation cycle, after waiting, or after audio completes.

\`\`\`mermaid
flowchart TD
  A[Owner verified occurrence + eligible foreground Activity] --> B[ENTRANCE: RX01-A fluid + RX01-B optic]
  B --> C{User interaction}
  C -->|Single ordinary tap| B
  C -->|Double tap / accessible Open action| D{Fresh owner validation}
  D -->|Rejected or stale| X[Owner invalid handler; no main content]
  D -->|Valid| E[EXITING: ~320ms scale and fade to dark]
  E --> F{Fresh owner validation again}
  F -->|Invalid| X
  F -->|Valid| G[MAIN: different calm fluid palette + original live response UI]
  G --> H[Only original owner processes Done, Snooze, replay, dismiss and reschedule]
  A -->|No valid/full-screen entitlement| N[Existing notification/user-opened fallback]
\`\`\`

## Exact staged code contract

\`RX01ReminderExperienceGate(domain, mode, occurrenceKey, occurrenceIsActive, validateOnOpen, motionEnabled, lowPower, onInvalidOccurrence, modifier, mainContent)\` is a **future reusable component**, not yet called by any live Activity.

- **Identity:** \`occurrenceKey\` comes only from the owner-verified stable occurrence key. Blank keys and \`occurrenceIsActive=false\` fail closed. The component never synthesizes IDs or treats an elapsed timer as acceptance.
- **Fresh validation twice:** \`validateOnOpen()\` is a fast, synchronously callable owner predicate, checked at double-tap and again when transition completes. Exceptions fail closed. A newer reminder or cancellation while the animation runs cannot authorize stale MAIN content if the owner's predicate identifies the change.
- **Three phases:** \`ENTRANCE\`, \`EXITING\`, \`MAIN\` with pure policy guards; only the current occurrence is eligible for transition. \`EXITING\` refuses duplicate activations.
- **Actual foreground safety:** \`mainContent\` is not composed at all in ENTRANCE or EXITING. Unlike a translucent overlay that leaves underlying buttons interactive, this removes Done/Snooze/Working from the composition tree until the gate succeeds.
- **Recreation:** \`remember(occurrenceKey)\` is deliberately NOT saveable. A new Activity instance/rotation conservatively returns to entrance. No stale main-open state is restored across process death/account switch. Preserving a previously-open screen across rotation would require an independently certified owner-generation-bound saver and is **not** claimed in RX01-C.
- **Validity lifetime:** \`occurrenceIsActive\` is supplied by the original owner as a live signal and must transition false when the canonical occurrence expires or is cancelled. On invalidation the wrapper hides content and requests the owner's existing safe close/fallback. RX01-D/E are responsible for the actual liveness subscription, responsive cancellation and any full-screen permission checks. A Boolean alone cannot establish delivery or ongoing ownership.
- **No side effects:** The staged UI does not start/stop TTS, alarm audio, notification services, release a wake lock, post an event receipt, mutate user data or manipulate Android FSI entitlement. The actual current reminder remains sole action owner.

## Accessibility equivalence

- Ordinary touch: \`detectTapGestures(onDoubleTap)\` exclusively; no single-tap action, no click-through to underlying buttons.
- TalkBack/switch access: a single focusable semantic **Open reminder** action. Android's standard accessibility activation (commonly a system-managed double tap) performs the same validated open action without requiring unusually fast gestures.
- Keyboard/DPAD: focusable action activated by Enter/Space/center key release. The component does **not** make a normal one-tap touch path available. Mode/category description remains generic, even for locked Personal alerts.
- The decorative RX01-A/B visual sits behind a dedicated topmost interaction surface. Its mic is **not** a real recording indicator, its alarm clock is **not** a countdown, and no Challenge arc represents earned progress.
- Main content becomes available to accessibility focus only when MAIN is actually composed. The owner must manage focus to the first appropriate main-screen element and retain existing full-screen TalkBack semantics in RX01-D/E.

## Appearance and performance

- Entrance: vivid Creator Pulse Studio, Personal Living Bloom or Challenge Rise Core under the original optical icon.
- Exit: 320ms very slight expansion (1.055×) and fade into the domain's dark MAIN base. Reduced motion, disabled Android animators or battery saver must be passed as \`motionEnabled=false\`/\`lowPower=true\` by later owners and skip this transition entirely.
- Main: independently configured Studio Nocturne, Velvet Sanctuary or Ember Command from RX01-A; a legibility scrim is already provided beneath caller-owned controls.
- No MP4 playback, stock bitmap, blur texture, paid plugin, internet, ambient sensor, screen-recording, media upload, microphone permission or network operation is required.

## Existing production paths / later integration gates

| Domain | Current source owner | Deferred RX01 wiring |
|---|---|---|
| Creator Voice/Alarm | \`VoiceReminderActivity\`, \`AlarmActivity\`, \`ReminderCheckInScreen\` | Verify occurrence and original callback identity; insert the gate in the eligible Activity, then ensure original screen background does not hide MAIN artwork. Never block original service watchdog or silence notification actions |
| Personal | \`PersonalAlertActivity\` and existing Personal Voice notification paths | Preserve current full-screen Alarm-only entitlement and locked-user privacy; no surprise full-screen Voice launch or leakage of personal title/notes |
| 21-Day Challenge | \`Phase1HabitLiveDelivery\` + current Challenge voice/notification owner | Build an explicit owner-scoped, permission-correct Challenge Activity/notification-open route in RX01-E; cannot claim automatic full-screen for Challenge yet |

**Settings precedence:** Legacy seven Classic/five Cinematic \`V391\` scene preferences, opt-in Premiere and companion selection remain unchanged. In RX01-F explicitly choose precedence without overwriting any persisted value.

## Post-"ALL DONE" QA gates (planned, none executed)

1. Kotlin/Compose compilation and full JVM/lint/regression CI against the final integrated source.
2. Unit policy checks for blank ID, inactive owner, invalid start, invalid finish, duplicate tap, lifecycle transitions, double-tap only and accessible action.
3. Android instrumentation taps: one tap cannot enter MAIN, valid two taps enter exactly once, TalkBack semantic Open succeeds, keyboard activation works, no action click-through.
4. Stale/change/cancelled occurrence during EXITING never renders MAIN, with owner-verified identity rechecks before and after the transition.
5. Rotation, process recreation, Account/restore change, two concurrent occurrences and full-screen OS-denied fallback.
6. Creator/Personal/Challenge Voice/Alarm service timing, notification actions, Replay, Done, Snooze, pause, working, dismiss, custom music and old data all retain their original true behavior.
7. High font-scale 320dp, tablets, keyguard, screen reader, animation scale 0, battery saver, background lifecycle and normal icon performance all physically checked.
8. Confirm new MAIN palette is visible behind actual content, avoiding existing opaque View/Compose backgrounds that could cover it.
9. Physical-device screenshots and the founder's extended testing before any stable replacement or Play deployment.

## Known limitations and next stage

**RX01-C has zero current Android caller integration.** It does not yet create the Challenge Activity or enable an unavailable full-screen mode. The double-tap instruction currently exists only in the source-only entrance composition. Runtime acceptance remains **UNVERIFIED** until the founder authorizes the unified test campaign.

**Next:** RX01-D source-stage the verified **Creator and Personal** domain integrations, retaining all source-owned response and keyguard logic, in a separately reviewed draft PR. Then RX01-E Challenge, RX01-F precedence/fallback/audit. Founder "ALL DONE" triggers consolidated merge, testing, Android builds and acceptance, **not before**.

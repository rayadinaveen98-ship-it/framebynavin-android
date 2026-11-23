# Backlot P10C — Living Reminder Visual Cutover (source-stage)
**Date:** 2026-11-23  
**Founder approval:** "ok bro continue" following the P10A/early P10B source-stage report.  
**Status:** SOURCE STAGED / NOT COMPILED / NOT TESTED / NOT VISUALLY CERTIFIED / NOT MERGED / NO APK.

## Protected baseline
- Repository: `rayadinaveen98-ship-it/framebynavin-android`.
- Cumulative P10C source branch: `feature/backlot-v337-point10c-live-screen-source-after-p10b-20261123`, created from `feature/backlot-v337-point10a-reminder-visual-foundation-after-p9b-20261123`.
- Accepted stable V337 SHA: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, not modified. `main` untouched.
- No build, Gradle, JVM, unit/instrumented test, screenshot, device QA, signing or CI was run, as specifically requested by the founder.

## Stage scope: P10C source changes

### Creator real full-screen reminder
- `reminders/ReminderCheckInScreen.kt` now uses `P10ReminderEntranceStage` for Act I and `P10ReminderMainAtmosphere` for Act II, sharing the selected Creator P10 family.
- Touch requires **double-tap** instead of the old V195 single-tap silhouette; accessibility users can explicitly activate the semantics action. On Android animations disabled, controls may reveal immediately in accordance with previous accessibility behavior.
- Replace old live `V195ReminderParticleScene` and `V336DirectorBackdrop`/`V336DirectorSignalArt` call sites in this screen with new P10 family visuals and optical-weight emblem.
- Introduce `P10ReminderActionPalette.kt` to style existing actions, text and panels from the saved living family. A unified editorial context panel replaces two redundant staged-info cards.
- **Preserve** all existing Creator callbacks, response/snooze/working/stage check-in/pause/reschedule/replay/dismiss, full-screen intent owner, stage labels, notes and adaptive actions.
- V337 Creator response access over the keyguard remains available (do not silently require unlock just to respond). Gallery media is still withheld until Android reports unlocked. This is intentionally different from Personal's strict lock privacy gate.
- No change to Creator voice or music service, scheduling, notification/receipt or persistence.

### Personal real alert
- `personal/PersonalAlertActivity.kt` delegates only *visual presentation* to `P10PersonalReminderExperience` for a validated posted occurrence, replacing the old P1 gallery-only entrance. When no custom media is selected, the new living built-in entrance appears too.
- Personal lockscreen title and response actions remain hidden, and Done/Snooze/Dismiss still use the exact `PersonalDeliveryCoordinator.apply` with original id/occurrence/token. No automatic full-screen privilege added to Personal VOICE or NOTIFICATION; Android policy remains unchanged.
- Replace main `V336DirectorBackdrop` and the old Personal signal art with `P10ReminderMainAtmosphere` plus `P10ReminderIdentityEmblem`. The previous `V2233PersonalParticleWave` function name and UI tag remain as compatibility wrappers, but draw new P10 artwork.
- Merge separate Personal schedule/status panels into one quieter editorial information stage. Preserve status descriptions, plan notes, busy/errors, and original QA identity tags.

### 21-Day challenge user-opened notification
- Existing `P1ChallengeNotificationEntrance` remains attached to the original `MainActivity` action `ACTION_OPEN_PERSONAL` and a `challenge:` task ID; no new PendingIntent, automatic full-screen launch, permission, background scheduler, audio or action owner.
- After the existing challenge validity, date, pause/archive and keyguard checks pass, show `P10ReminderEntranceStage` with selected 21-Day P10 family, even if the user has no personal gallery file.
- On double tap, dismiss just the overlay and continue to the **existing normal app challenge destination**, rather than manufacturing a second challenge full-screen action screen.
- A neutral validation surface is recolored to the family; it never reveals the private challenge title while validating.
- The older `P1GalleryEntranceVisual` remains in the source for the legacy unused Personal host until P10E code deletion audit.

## Source-level change limits
- **Changed**: reminder presentation Kotlin files plus one P10 palette and one Personal presentation adapter. Did not change alarm owners, service, database, notifications, auth, app navigation, cloud or settings storage.
- **Not completed**: P10D gallery/main-stage continuity and selection page; media fallback removal; stage P10E actual old-class/resource deletion.
- The entry animations and icons remain UNVERIFIED on device; code presence is not visual acceptance.
- Some historical `v336-` and `v195-` test tags remain deliberately as compatibility selectors, **not** their old rendering. P10E must distinguish test tag retention from shipping old visuals.

## Deferred consolidated QA gates
1. Compile the cumulative P1–P10C branch only after founder explicitly authorizes consolidated QA. Inspect Compose symbol/overload and Android API compile errors.
2. Creator Alarm and Voice: double tap opens main, TalkBack opens main, back/close behavior unchanged, over-keyguard response works and private media does not display while locked.
3. Personal Alarm unlocked/locked, Voice notification and notification-only state: privacy/trigger policy; duplicate/stale/invalid occurrence actions cannot be accepted.
4. 21-Day user-opened notification with/without gallery media, paused, archived, stale, locked, out-of-date and notification-only challenge; no accidental auto full-screen launch.
5. Creator/Personal new family selected (all three), Act I to Act II color and depth continuity, reduced-motion, battery saver, low-RAM, foreground/background/rotation.
6. Original actions, screenreader names, touch target sizes, scroll at 320dp/360dp/420dp and 1.5×–2× fonts. No false completion or double-posted reminders.
7. Device performance/profile GPU: frame budgets, thermal/battery, no strobe or unreadable overlaid text; gallery codec/lifecycle still pending P10D.
8. Real signed owner preview only after P10D/P10E staging and explicit release-gate instruction.

## Outstanding decisions or non-bugs
- No separate full-screen 21-Day main was in the accepted notification flow. This stage intentionally preserves that behavior.
- The old V391 and V394 option/source classes, V336 visuals and the old unused P1 gallery-only visual functions still exist in the repository until P10E. **Do not report "completely removed" yet.**
- No claims of compiling or tests passing. Source-only auditing located and fixed temporary invalid palette imports and a challenge host brace while staging; both still need actual compiler verification.

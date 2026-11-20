# Backlot UX11-F — Personal Reminder creation experience cleanup

Date: 2026-11-20
Status: SOURCE-STAGED ONLY, UNMERGED, NOT COMPILED / NOT TESTED.
Founder control: RX01 and UX11 feature changes are separately source-staged; consolidated integration, unit/UI/Android testing and QA APK creation happen ONLY after the explicit phrase ALL DONE.

## Scope and exact source ancestry

- This stage stacks on UX11-E PR #458 head c18d6a93142e909e7ff6ebb89b6c7c1cd76e643d, followed by UX11-D #457, C #456, B #455, A #454, and RX01-A..F PRs #448–#453. UX09 excluded.
- Protected main and stable/backlot-ux01-08-ux10-owner-accepted-20261120 remain at abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
- Founder screenshot showed STEP 1 OF 4 and numbered 1 Purpose / 2 Schedule / 3 Alert / 4 Review chips, a functional but over-mechanical form, a narrow Review chip and heavy Continue/Cancel footer.
- This is a presentation-only correction; the four internal stages and their data/validation remain untouched.

## Actual source mapping and approach

1. Existing app/src/main/java/com/framebynavin/app/ui/V2232PersonalReminderHub.kt contains the live V2232PersonalEditor. It holds rememberSaveable values for title, notes, purpose, recurrence, schedule, alert, snooze, voice count/gap/music, alarm duration, retry, optional Space/Group links and composerStep.
2. For NEW reminders, composerStep is 0, 1, 2, 3. For EDITING a saved reminder, composerStep is -1 (all-fields layout with original V2271PersonalEditorFooter). This latter path must NOT be converted to a new wizard.
3. Original V406PersonalComposerProgress printed numeric progress/chips and was only substituted in the New Reminder live editor; the original implementation file remains present for compatibility.
4. Existing V296PersonalStepActions implemented the first three footer actions with the same provided onContinue/onBack lambdas, and V2271PersonalEditorFooter owns final Save/Cancel. Its code remains as a historical fallback; new UX11-F footer uses the same callback meaning and existing UI automation tags.
5. No changes to any PersonalReminderDraft checked() call, PersonalRepeat recurrence, V310 Review links, PersonalReminderStore, PersonalReminderCommands, scheduling/notification service, alerts, music/voice, account auth, UX09 or approved stable branch.

## UX11-F user-facing presentation

- One small premium header: PERSONAL / NEW REMINDER, Make it yours, warm muted rose on charcoal, without re-introducing a separate popup or Android window. Existing Close icon remains.
- Progress is a thin four-segment line plus **Purpose, Schedule, Alert, Review** text-only labels. No numeric "STEP 1 OF 4" and no numbered chips.
- Unlike the old implementation, this component sits **below** the header Row and spans the full screen width. At ordinary phone width all labels can fit; 200% fonts use horizontal scrolling rather than clipping.
- Active section has a subtle rose border, visited sections remain distinguishable, future sections stay inert. Clicking an earlier section calls the same onReturnTo predicate and clears the local error, but never advances past validation.
- Each section has a brief contextual instruction, with no false promises about Android alarm delivery.
- Original suggestions remain hidden behind their optional toggle; notes are still optional and preserve typed text on collapse. Their links are now left-aligned instead of floating/centered across the entire page.
- The existing title and notes input fields have larger rounded corners. Original values, 180-title-character and 2000-note-character limits and optional classification/suggestion behavior are unchanged.
- Steps 0–2 use a more compact footer: one full-width primary Continue and a minimum-48dp textual Cancel/Back below. Existing tags v296-composer-footer / -next / -back retained. Footer still delegates to the original in-source callbacks and first-stage title validation.
- At Review or while editing a saved reminder, existing Back to Alerts and V2271PersonalEditorFooter Save/Cancel actions remain unchanged. Original .checked() validation and V310 linked-Space/Group processing are untouched.

## Intended navigation and data safeguards

| Existing state | Presentation | Same source-authorized behavior |
|---|---|---|
| New reminder start | Purpose text pill, progress line | Suggestion/notes/title with no schedule written |
| Tap Continue with blank title | Remain in Purpose, show existing validation error | No advance/save |
| Continue from Purpose | Schedule text pill | Original date/time and repeat selections |
| Continue from Schedule | Alert text pill | Original voice/alarm/notification, music, snooze/retry |
| Continue from Alert | Review text pill | Original draft preview, optional Space/Group links |
| Tap a previous text pill | Earlier section | Existing local error reset, no future-step skip |
| Save | Original source-owned Save Footer | PersonalReminderDraft.checked(), onSaveWithLinks or onSave |
| Existing reminder Edit | Original all-fields mode | Same original Save/Cancel and existing values |
| Cancel/Close | Original onDismiss callback | Does not silently save reminder |
| Phone rotates mid-form | rememberSaveable existing values | No unexpected reminder creation or value loss |

## Strict testing and certification after founder ALL DONE (not run)

- Kotlin/Compose compile of complete RX01+UX11 stack and existing unit/lint/instrumentation/Android QA, including import, shape, semantics, ScrollState and saved state.
- Confirm no numeric chips or STEP n OF 4 text on new Personal reminder creation screen; only text-only labels and subtle segmented rail. Editing flow remains unchanged.
- On 320/360/393dp Samsung and tablet, portrait/landscape, 100–200% font: all four labels discoverable, Save/Continue/Cancel reachable, buttons >=48dp, no overlap with status bar, IME or gesture navigation.
- Test every keyboard, TalkBack, switch-access, accessible earlier-step navigation, disabled future-step behavior and progress announcements.
- No data loss when rotating/backgrounding, entering long title/notes, toggling suggestions, changing repeat, hourly/weekly selections or switching voice music URI.
- Title blank first-step error remains, date/time allowed policy and .checked() save validation unchanged; Save does not execute from first three steps.
- Verify current alert voice repeat/music, alarm duration, snooze, auto-retry, notifications/FSI disclosures, links and review state all survive back/forth.
- Verify old edit existing reminder path, cancel without write, saved linked Spaces/Groups and persistent store data remain intact.
- Cross-regression with UX11-E sanctuary create button, UX11-C restored setup, UX11-D Google sign-in and RX01 original reminder visual/notification system.
- Verify source, exact variant and CI evidence before providing any QA APK or asking for multi-day device acceptance.

## Release status

**UX11-A..F are source-staged only.** No Android build, unit test run, final integration, merged PR, physical-device validation, signed APK or Play Store release has been authorized or performed for this new change stack.
Next action after the founder explicitly says ALL DONE: reconcile latest live GitHub branch SHAs (RX01 + UX11, but NOT UX09), run compile/integration/regression/security/accessibility/physical QA, remediate any defects, and deliver a traceable QA APK for founder testing; any release promotion remains separate.
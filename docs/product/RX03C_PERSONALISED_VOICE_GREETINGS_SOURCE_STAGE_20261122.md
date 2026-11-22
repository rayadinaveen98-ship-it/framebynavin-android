# Backlot RX03C — Personalised Time-of-Day Greeting for All Spoken Reminders

Date: 2026-11-22  
Status: **SOURCE STAGED ONLY — NOT COMPILED, TESTED, DEVICE-LISTENED, MERGED, APK-BUILT OR RELEASED.**

## Owner's request

Have spoken reminders greet the user's known first name, e.g. "Hey Navin, good morning / afternoon / evening", and then speak the already-selected domain-specific Creator, Personal or 21-day Challenge voice instructions from RX03B. Never use "Good night" as the announcement greeting. Preserve voice packs, reminder actions, repetition options, Snooze and delivery authority.

### Greeting policy

- Local device timezone at **speech cycle startup**, not a cloud service. Calendar clock transitions are not recomputed while a single cycle repeats.
- 05:00–11:59: "Hey [first name], good morning."
- 12:00–16:59: "Hey [first name], good afternoon."
- 17:00–23:59: "Hey [first name], good evening."
- 00:00–04:59: "Hey [first name]." (neutral overnight; no ill-timed "good evening" or "good night").
- If no trustworthy locally cached username/display name is available: "Hey there, good morning/afternoon/evening." or "Hey there." overnight.
- Name source precedence: cached signed-in Google session display name; cached matching creator profile; existing local Creator profile name. Use **first name only**, never email address, no invented full name, and no network lookup in a live reminder. Existing Creator-style fallback "Creator" remains in the legacy \`resolve()\` API; new reminder speech calls use \`resolveKnownName()\` with an empty string when unknown.

### Repetition behavior

The greeting is said **once per new audible playback cycle**. For 3 repeats, the first utterance is "Hey [first name], good morning. [Full domain script]"; the second and third repeat only "[Full domain script]". An intentional replay starts a new cycle and may greet again. Repeat count, inter-repeat spacing and successful/failed audio receipt rules remain exactly the previous owner's code.

### Scope staged

- \`VoiceGreetingBuilder.kt\`: refines existing greeting text; retains original \`spokenReminder(...)\` interface and local-time-zone overload; adds \`preferredKnownName(...)\` for true name-or-empty semantics while preserving \`preferredName(...)\` legacy Creator fallback.
- \`VoiceCreatorNameResolver.kt\`: adds \`resolveKnownName(context)\`, using the existing cached Google/profile/local identity and no network call. Existing \`resolve(context)\` still falls back to "Creator" for older callers.
- \`VoiceReminderService.kt\`: Creator Voice first utterance receives greeting, later repeats only frozen task/priority/stage/notes script; existing explicit Replay or new occurrence starts a fresh cycle. Previous response service, notification and repeat handling retained.
- \`AlarmRingingService.kt\`: Creator Alarm spoken component uses the same greeting and now honors an explicitly selected RX03 local speaker if installed, like Creator Voice. Ringing and OS/lock-screen paths untouched.
- \`PersonalPlaybackService.kt\`: Personal Voice first utterance greets, later repeats original/newly selected domain script without greeting. Private notes still unspoken; all existing music/foreground/notification/response ownership unchanged.
- \`Phase1HabitLiveDelivery.kt\`: Challenge Voice first utterance greets, subsequent repetitions contain just the exact frozen activity script. Original 21-day Done, Snooze, ledger, watchdog, schedules, notifications and repeat count untouched.
- \`V403VoicePreviewPlayer.kt\`: Creator, Personal, Challenge and installed offline speaker comparison previews start with the same actual greeting. Preview remains one active transient local TTS engine and saves no reminder state.
- \`RX03BReminderVoicePacksSettings.kt\` / \`V140VoiceStudioPicker.kt\`: concise explanations of the personalised intro and retained script repetition.

### Examples

At 06:30, registered first name Navin:
- Creator Warm: "Hey Navin, good morning. A small creative nudge. Task: Finish short-film edit. Stage: Editing. One step at a time."
- Personal Funny: "Hey Navin, good morning. A little plot twist in your day: Drink water. Consider this your friendly nudge."
- Challenge Bright: "Hey Navin, good morning. Let's go! Your 21-day challenge is ready. Today's activity: Morning walk. You can do this."

At 13:30: "Hey Navin, good afternoon. [Selected domain script]"
At 20:30: "Hey Navin, good evening. [Selected domain script]"
At 02:30: "Hey Navin. [Selected domain script]"
No known user name at 13:30: "Hey there, good afternoon. [Selected domain script]"

No claim that the spoken greeting proves successful delivery, marks completion, changes notification privacy, or affects an alarm's eligibility.

## D8 acceptance checks — not yet executed

1. Compile Kotlin; inspect all changed speech owners against frozen RX03B parent; no audio stream or network registration introduced.
2. Pure greeting time-zone logic at local **04:59, 05:00, 11:59, 12:00, 16:59, 17:00, 23:59, 00:00**, with named and nameless cases. Never speak "good night".
3. Cached signed-in display name takes precedence; matching-profile identity only, local explicit creator name fallback and unknown-name fallback; no live Supabase/Google request during an alert. Ensure no email address, whitespace-only name, spoofed punctuation or blank name becomes a false username.
4. Creator Voice/Alarm, Personal Voice and Challenge Voice paths each include the greeting once and only on spoken alerts; Notification-only, Quiet or silent alarms do not newly speak. The main scripts remain exactly RX03B (or old personal/challenge body on null pack).
5. Repeat Count 1–5: greet once, all repeats contain the correct frozen title/stage/activity; explicit Creator Replay may restart intro. No unintended repeated greeting, wrong occurrence or overlapping music.
6. User response and lifecycle: Done/Snooze/Dismiss/Close, cancellation, stale occurrence IDs, playback errors, foreground-restricted cases, language-not-installed, low-power devices and redacted lock-screen content.
7. Voice Settings live Listen/Preview: greets identically to actual playback; comparison sample remains identical between speaker identities, no saved user mutation.
8. Telugu and English locale pronunciation, TalkBack and multilingual names are NOT certified; live audio owners use the currently installed Android TTS, not eight proprietary neural voices.
9. Preserve P0 Personal long-idle delivery investigation. Cumulative D8 Gradle/CI/physical-device tests and Lab APK only when owner issues explicit final instruction; stable main/merge separately approved.

## Stage checkpoint

- UX12 D0–D7 review closure, RX02 media, RX03 offline voices, RX03B domain scripts: source staged.
- **RX03C personalised name and greeting**: source staged, not runtime verified.
- Per-individual Personal/Challenge voice selection data-model extension: still proposed, not implemented.
- RX04 genuine original speaker identities: still unapproved provider/licensing/fee decision.
- D8 integrated QA / APK: ON HOLD, not automatically authorized by staging.

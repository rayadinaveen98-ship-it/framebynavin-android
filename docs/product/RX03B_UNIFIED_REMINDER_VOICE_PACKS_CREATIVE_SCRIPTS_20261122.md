# Backlot RX03B — Voice Packs and Clear, Creative Scripts

Date: 2026-11-22
Status: SOURCE STAGED ONLY. Script copy is proposed for owner listening review. NOT compiled, tested, audited on a real device, merged, built into an APK or released.

## Change control

Parent: RX03 @ 168effc4f394cec0ad4b0b9588f44153b60b82ef
Current isolated branch: feature/backlot-rx03b-unified-reminder-voice-packs-20261122
Protected stable main at stage entry: abbe88984bac16cfafbd990d9cb00ed54cfdd07e

Owner requested:
1. Give Personal and 21-day Challenge reminders the eight familiar voice-pack choices already available to Creator reminders.
2. Make Creator, Personal and Challenge speech scripts more comprehensible, differentiated, creative and useful.

No tests/CI/build/APK/merge until founder explicitly authorizes cumulative D8. No billed neural service, paid voice pack, cloned voice, third-party data transfer, or Supabase change.

## Scope staged

- New RX03BReminderScripts.kt: three different domains, eight persona scripts each (24 templates), deterministic text from real user title/activity; Creator additionally receives actual priority/stage and optional existing note, original greeting remains via VoiceGreetingBuilder. Template logic never invents progress/streak or a Done receipt.
- VoicePersonaEngine.kt: Creator now uses clearer v2 copy, and voice-preview sample matches the Creator runtime script. All existing eight persona IDs, saved task choices, pitch/rate, optional RX03 installed-speaker identity and playback lifecycle preserved.
- RX03BVoicePackPreferences.kt: independent Personal and Challenge default choices in a small private SharedPreferences file. No selection defaults to the exact old script and Android TTS behavior. Null/reset restores legacy behavior. Never modifies PersonalReminder or Phase1HabitChallenge persistence.
- PersonalPlaybackService.kt: on opted-in Personal pack only, changes the frozen phrase and uses the same persona's installed offline speaker fallback. No selection speaks the exact original “Personal reminder. [title]”. Existing personal notes remain deliberately unspoken; music/repeat/Snooze/action/notification owners unchanged.
- Phase1HabitLiveDelivery.kt: on opted-in Challenge pack only, changes the frozen repeated phrase and TTS persona. No selection speaks original “It's time for [activity]”. Completion still requires explicit action in challenge page; normal service/ledger/recovery unchanged.
- V403VoicePreviewPlayer.kt: user-initiated script preview override, still one active sample, stale-generation cancellation and cleanup.
- RX03BReminderVoicePacksSettings.kt: collapsible Personal and Challenge selection section under Settings → Voice; eight named styles with their actual domain-specific text preview, Listen without saving, Use Pack with confirmed persistence, Restore Original; current domain choices summarized when collapsed.
- V144SettingsHub.kt and V404SettingsIndex.kt: single Voice Settings entry and discoverability. Original Creator voice selector and RX03 installed-speaker lab remain.

## Script editorial criteria

Clarity: exact title/activity in the first two sentences; precise Creator priority and workflow stage. No fake facts, streaks, or claims that an alert was delivered/heard.
Creativity: warm, direct, focused, energetic, witty, cinematic, science-fiction transmission and procedural robot identities; no excessively long setup.
Safety: critical priority always overrides playful Creator language with “Critical creator deadline. Task: [title]. Stage: [stage]. This needs your attention now.”
Personal tone: avoid pressure or moralizing; neither private notes nor contextual medical advice is spoken.
Challenge tone: never says the user is done or has advanced a day; exact chosen activity only. An alert is NOT a completed day.
Language: English v1 scripts; quality in Telugu/English mixed speech is NOT verified or localized in this source stage.
Sound: no new distinct neural voices. Personas still rely on actual available local Android TTS engines, optional RX03 speaker assignments and their pitch/rate presets.

## Eight pack examples — actual proposed v2 script phrases

For Creator: title “Finish short-film edit”, priority Normal, stage Editing.
For Personal: title “Drink water”.
For Challenge: activity “Morning walk”.

| Pack | Creator | Personal | 21-day Challenge |
|---|---|---|---|
| Warm Human | A small creative nudge. Task: Finish short-film edit. Stage: Editing. One step at a time. | A gentle personal reminder. Drink water. Take a moment for this. | Your 21-day challenge check-in. Today's activity: Morning walk. One step at a time. |
| Clear Human | Creator check-in. Task: Finish short-film edit. Stage: Editing. Ready for your attention. | Your personal reminder is here. Drink water. It's on your schedule. | 21-day challenge reminder. Today's activity: Morning walk. Open Backlot when you're ready to record your progress. |
| Deep Human | Focus time. Task: Finish short-film edit. Stage: Editing. Choose your next move. | A quick personal check-in. Drink water. Time to give this your attention. | Your challenge is calling. Today's focus: Morning walk. Give it your attention. |
| Bright Human | Ready for the next scene? Task: Finish short-film edit. Stage: Editing. Let's make progress. | Hey! Here's your reminder. Drink water. Let's make it happen. | Let's go! Your 21-day challenge is ready. Today's activity: Morning walk. You can do this. |
| Funny | Plot twist! Your next scene is on the task list. Task: Finish short-film edit. Stage: Editing. Cue the action. | A little plot twist in your day: Drink water. Consider this your friendly nudge. | Today's plot twist: a mini mission! Activity: Morning walk. Small steps, big story. |
| Cartoon | Lights, camera, action! Creative mission: Finish short-film edit. Stage: Editing. Time to begin. | Attention, superstar! Your reminder has arrived: Drink water. Action time! | Lights, camera, challenge! Today's mission: Morning walk. Ready for the next move? |
| Alien | Backlot transmission. Mission: Finish short-film edit. Current phase: Editing. Awaiting your signal. | Incoming personal transmission. Subject: Drink water. Signal received. | Challenge transmission received. Today's mission: Morning walk. Awaiting your response. |
| Robot | Backlot task alert. Task: Finish short-film edit. Workflow stage: Editing. Action pending. | Personal reminder notification. Task: Drink water. Action pending. | 21-day challenge alert. Activity: Morning walk. Awaiting your action. |

Creator actual playback prepends the existing local-time greeting and first name (when available). The exact urgency and stage come from the existing task model, not guesses.

## Important limitation / future stage

Current Personal/Challenge selections are *domain defaults*, not per-reminder or per-challenge fields. Different Personal reminders cannot yet have different saved persona overrides in the same domain. Per-item selection requires updating PersonalReminder and Phase1HabitChallenge models, their codecs/backups/restores/editors, migration and ownership before implementing; it must not be simulated using orphanable sidecar IDs. No fields or schema contracts were changed here. Founder must approve that more invasive data-model stage separately. Creator already supports per-task voices.

Original truly unique licensed or neural speaker voices are a future premium voice milestone, with explicit licensing/consent, data privacy and any recurring provider fees requiring founder approval.

## Deferred D8 QA acceptance

- Check source compile, dependencies/imports, proper Compose rendering and preview audio cleanup only at final cumulative D8.
- For Personal/Challenge with no choice: exact original script and Android speaker. Existing notes remain private and unspoken, and Repeat/Gaps/Music/Alarm/Quiet/Notification behavior unchanged.
- For explicit selections: eight persona × three domains, actual stage/priority/title/activity, punctuation, long unusual titles, notes and locale. Reset to Original and app restart persist.
- Critical Creator priority overrides jokes, Important Creator priority is named, Normal remains concise. Check Creator greeting, existing project per-task setting, repeated speech and TTS failure handling.
- Check actual Android speaker IDs and English/Telugu device settings; do not call local preset modulation eight distinct speakers unless listening proof.
- Check Personal playback P0 long-idle restrictions, auto-retry, audio foreground lifecycle, Snooze and cancellation; check Challenge exact day completion path independent of voice alerts.
- Speaker preview never saves or marks a reminder delivered; only one active preview. Local no network; user-selected packs do not make new permissions or APIs.
- Visual accessibility at small screens, tablet, 2x font, label and whole Settings scroll; source stage QA isn't device certification.
- D8 final compile/tests/owner-signed Lab APK only when founder explicitly commands. Stable promotion separately gated.

Roadmap: UX12 D0–D7, RX02, RX03, RX03B are source staged; RX04 genuinely distinct original premium voice system remains separately proposed; D8 held; Personal P0 reliability gate open.

# Backlot UX modernization — 03 New Project Creation & Reminder Voices

Status: **STAGED IN SOURCE ON A STACKED DRAFT BRANCH, UNCOMPILED, UNTESTED, UNCERTIFIED, UNMERGED AND UNRELEASED**.
Parent UX02 branch: feature/backlot-ux02-onboarding-home-20261117 (draft PR #394).
Protected V337 baseline: stable/backlot-v337-premiere-owner-testing-20261114.
Branch: feature/backlot-ux03-project-voices-20261117.

## Approved owner request and implementation

### New Project
- Preserve seven progressive steps with edit/review and separate Quick Create: (1) "What are you making?" with a required project title and optional notes; (2) type; (3) platform; (4) format; (5) due date; (6) reminders; (7) final review/create.
- The existing onboarded creator mode, production styles, and platform preferences are suggestions only; all existing project types and complete-edit controls remain usable.
- Add an explicit "Project title · Required" label with clear help text and make Quick Create's required-title label equally clear. Do not alter existing underlying project fields, schema, save, discard/unsaved-change policies, or reminder scheduling.
- Surface the selected voice style, spoken repeat count, and gap in the review step only when a voice delivery may be involved. Keep voice and reminder fields editable.
- Existing Settings default voice continues to prefill new projects; per-project voice changes do not change Settings or old project voices.
- Preserve project title and optional notes across first-run/reopen/draft and do not create duplicate projects from a re-entry.

### Reminder voices
- Preserve all eight existing voice identities and their existing stable enum IDs: Warm Human, Clear Human, Deep Human, Bright Human, Funny, Cartoon, Alien, Robot.
- Differentiate especially Funny, plus Bright and Cartoon, via improved local Android TTS pitch/speed, persona-specific preview scripts, persona-specific reminder wording and higher-tier local voice selection where available.
- Be precise: these are locally styled TTS identities, not guaranteed distinct licensed neural voice recordings. Installed TTS engine, locale and device voices determine their sound. No new paid service, permission or network dependency.
- All voice samples from Settings/New Project/Edit now share one bounded in-process player: new preview cancels old one; initialization callbacks are generation-guarded; audio stops after completion or a 12-second safety timeout; screen exit stops preview.
- Preserve independent Creator voice engine and Personal reminder voice engine. Personal voice still uses on-device TTS; this scope does not introduce a new per-Personal-reminder VoicePersona schema.

### Speak this many times: 1–5
- Keep defaults at three spoken announcements for Creator, Personal and legacy challenge behavior. Only explicit choices change an occurrence to four or five spoken utterances.
- Update Creator control options, view-model save clamp, TaskStore deserialization clamp, active playback repeat clamp, service watchdog and wake-lock budget.
- Update Personal editor options, PersonalReminder validation, PersonalPlaybackPolicy next-step and maximum playback window. Existing persisted data/2.5-second Personal gap and 20-second gaps remain supported.
- Creator new choices for repeat gaps: 5, 10, 15, 30, 60 seconds. A project already saved with 20-second gap still shows the selected 20-second value on Edit.
- Repeats are multiple utterances of one original occurrence, not auto retries or separate alerts. No implicit Done/Snooze/acknowledgement; stop on user response, deleted reminder, changed occurrence, or shutdown.
- Watchdog formula includes up to five complete 45-second TTS windows and four bounded gaps; at maximum settings Creator total is 480s (+10s wake-lock), Personal is 470s (+5s wake-lock). This is a safety upper bound, **not** a promised audible duration.
- Do not change the independent challenge repeat default or separate opt-in automatic retry count.

## Changed source files

- reminders/V403VoiceRepeatPolicy.kt — shared count/window contract
- ui/V180ReminderTimingControls.kt — five Creator announcement options and approved gap choices
- data/CreatorViewModel.kt — Creator save-bound extended
- data/TaskStore.kt — persisted 4/5 Creator count retained on reload
- reminders/VoiceReminderService.kt — service repeat count and watchdog/wake-lock extended
- personal/PersonalReminder.kt — validate explicit 1–5 Personal count
- personal/PersonalPlaybackPolicy.kt — bounded 1–5 Personal spoken cycles
- ui/V2232PersonalReminderHub.kt — Personal 1–5 choices
- ui/V403VoicePreviewPlayer.kt — single nonoverlapping transient local TTS preview
- ui/V101BReminderUi.kt — Composer routes voice previews through new player
- ui/FrameByNavinV101BApp.kt — Settings routes voice previews through new player
- reminders/VoicePersonaEngine.kt — expressive device-local persona treatment
- ui/V140VoiceStudioPicker.kt — truthful local TTS disclosure and playback stop on exit
- ui/V207ProjectVoiceSelector.kt — honest project-only preview disclosure and cleanup
- ui/V20NewProjectWizard.kt — required title label and voice delivery review summary

## Deferred Stage-10 certification

1. Build compiles and runs against intended Android SDK and Kotlin Compose configuration.
2. Fresh and legacy project creation/quick create/edit retain all fields and reminder choices; discard/restore re-entry never duplicates projects.
3. Creator and Personal repeat counts 1–5 save/load, backup/restore and retain value, including service process death.
4. At 5 counts / 60s gap, Creator and Personal services finish valid utterances without a premature watchdog; user response stops active repeat immediately.
5. Stop on reminder deletion, snooze, done, account change, source reload, foreground-service interruption and Android OS restrictions; no ghost audio.
6. The completion of a spoken cycle is not a user acknowledgement; regular retry behavior remains distinct and opt-in.
7. Funny/Cartoon/other persona previews audibly differ where possible; truthful fallback behavior on devices with only one local voice or missing TTS data.
8. Rapid preview taps never overlap, old TTS init cannot play late, and leaving Settings/Composer releases audio.
9. Correct labels/values on phones/tablets, Telugu/English locale handling, large accessibility fonts, reduced motion and screen-reader names.
10. Full automated unit, integration, UI, security/performance, emulator, physical-device and upgrade certification after all ten stages are staged.

## No action taken

No unit/UI/integration test, build, CI, emulator, APK, stable/main merge, Play release, schema migration or production deployment executed during UX03. All stage implementation commits remain isolated and unmerged pending user-approved integration and final certification.

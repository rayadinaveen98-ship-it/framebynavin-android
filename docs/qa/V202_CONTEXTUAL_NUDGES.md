# Backlot V202 — contextual creator notification preview

Version code 222, version name `2.7.2-v202-contextual-nudges-preview`.
Stacked on V201 P1 `feature/backlot-v201-respectful-nudges-p1`.

## UX
The existing creator-context nudge selection stays grounded in active project state.
- Overdue publish: re-entry copy acknowledges the past **planned** time, never claims publication actually happened.
- Missed reminder: a calm check-in; not another loud reminder.
- Within 6 hours: concise final-stretch message including actual source workflow copy.
- Within 24 hours: actionable focus message with source progress.
- Critical project inside 48 hours: priority-specific, sourced context.
- Fallback: original engine text, not fabricated completion or view statistics.
- Tap: the pre-existing `CreatorWidgetContract.ACTION_OPEN_STUDIO` plus actual `EXTRA_TASK_ID` brings the creator to the relevant project instead of generic Home.

## Safety / release gates
- All V201 restrictions remain: optional default-off nudges, Android permission/channel respect, 09:00–19:59 local window, creator quiet hours, one nudge per rolling 24h and one same-key per 72h.
- No changes to reminder alarms, voice, Smart escalation, PendingIntent actions, task/data persistence, package identity, signing or 3-second V199 welcome.
- Same context notification ID, WorkManager name, and Android notification channel as before.
- Unit tests verify wording for real due/risk keys, trimmed names, and fallback behavior. Source guard verifies V200 branding plus V201 consent/anti-spam and V202 direct project navigation.
- V202 CI must pass unit/lint/personal APK signature/full Android emulator regression on exact head.
- Device smoke: V200→V202 in-place upgrade without project loss; optional nudges off yield zero prompts, permission/channel off respected, no activity opens from unsolicited background sends, tapping a real nudge opens the intended project, back navigation works, widget deep links still skip welcome, no duplicate sound.
- Keep V200 stable candidate and V201 P1 draft untouched. This is a draft preview only until explicitly accepted.

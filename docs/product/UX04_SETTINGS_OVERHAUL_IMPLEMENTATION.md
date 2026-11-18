# Backlot UX modernization — 04 Settings Overhaul

Status: **SOURCE IMPLEMENTATION STAGED; NOT COMPILED, TESTED, MERGED OR RELEASED.**
Parent: `feature/backlot-ux03-project-voices-20261117` (draft PR #395).
Branch: `feature/backlot-ux04-settings-overhaul-20261117`.
Protected stable ref: `stable/backlot-v337-premiere-owner-testing-20261114`.

## Scope

### Settings Home
- A searchable, grouped landing page, grouped into Account & Personalization, Reminders & Planning, Privacy & Help.
- A pure search index maps approximately twenty settings terms to **existing** destination screens and callbacks. No extra settings store, duplicated notification state, or new network access.
- Search results never perform a destructive action. Searching for local erasure opens Advanced, which contains the existing separately confirmed local-data reset.
- Search-to-detail Back returns to Settings Home. Normal Appearance & Sound subgroup navigation retains its existing parent path.
- Calm rows, readable labels and subtler card strokes; global design-system density remains reserved for UX10.

### Existing controls preserved
- Account and Creator ID editing, Google sign-in path, profile return from Home/Settings.
- Director / Light themes, icon selection, selected UX01 welcome animation and launch-sound toggle.
- Spark/Mira companion selection and optional suggestions.
- Existing Creator default voice, device-local music picker, previewing, and UX03 voice-selection contract.
- Existing notification request, precise timing, full-screen, battery, snooze, alarm auto-stop, unanswered follow-ups, quiet hours and optional context nudges.
- Weekly Auto Plan confirmation and saved templates, weekly planner, automation, Google Calendar, YouTube Insights.
- Data & Privacy activity, guided app tour, version display and original device-only erasure confirmation.

### Navigation and copy fixes
- Reminder scenes now have their own page under Appearance & Sound, rather than duplicated entry into Appearance. The original `V391ReminderSceneSettings` renderer and storage remain unchanged.
- Settings Voice uses the UX03 bounded TTS player, rather than an independent legacy preview engine.
- Account UI no longer falsely points to nonexistent `Settings → Backup & Data`, and no longer describes Google sign-in as an automatic backup.
- Permission status distinguishes notifications blocked vs advanced Android access incomplete vs fully available.
- Snooze and alarm-stop defaults are labeled as Creator defaults, keeping Personal reminder options independent.
- Guided-tour text reflects the actual six Creator/Both lessons, including Reminder Hub.
- Ready permission rows are disabled rather than clickable no-ops; meaningful control affordances remain.

## Explicit backup, deletion and cloud guard

A legacy `CloudSyncActivity` still combines sign-in, sync, and optional Drive backup/restore functionality. UX04 **does not create a new link to this legacy activity in Settings Home**, does not re-add a Backup/Restore section, and does not claim unified recovery or full account deletion works.

The existing Profile account entry still has its pre-existing account-management path. UX09 must reconcile this with the approved full data architecture, removal of outdated backup UI and safe deletion design before a final release.

Advanced **preserves** `FreshStartActivity` local erasure only. No hosted data, backup archive, project, reminder, profile, Supabase record or Google Drive file has been changed.

## Implementation inventory

- `ui/V404SettingsIndex.kt`: read-only destination index and token search.
- `ui/V167SettingsNavigationPolicy.kt`: dedicated reminder scene subpage, stable navigation enum.
- `ui/V144SettingsHub.kt`: searchable grouped Settings Home, source-of-truth routes, 01/03 consistency, accurate permissions and no-op removal.
- `ui/V23ProfileAccountUi.kt`: correct misleading backup path/copy.
- This document.

No Settings value storage migration, system permission grant, destructive action, schema, cloud backend update, user data mutation or production merge.

## Deferred acceptance criteria after UX10

1. Root search finds and opens all indexed categories; no empty back path or loop.
2. Correct search-to-detail Back routing, tab/Activity return, Android system Back and configuration changes.
3. Theme, icon, welcome animation, launch sound and scene pickers retain all choices across restart.
4. Companion, sound, TTS sample cleanup, default voice persistence, and custom music preference.
5. Full permission status updates on Android resume; precise timing, FSI, notifications, battery and quiet hours respected.
6. New Creator defaults do not overwrite Personal reminders or existing project choices.
7. Weekly Auto Plan remains confirmation-gated; Calendar sync exports only after existing authorization.
8. Full account sign-in/sign-out and returning-account interactions unchanged; no fake complete deletion or restore UI.
9. Existing erase-local-data screen still requires its original confirmation, and does not touch cloud state.
10. Landscape, small and large phones, tablets, TalkBack, large font, contrast, system insets, performance, all integration and regression tests.

## Workflow

Owner-approved implementation-first workflow: **no compilation, CI, unit, UI, integration, emulator, physical-device, performance or release tests performed for UX04**. Joint tests and repair/certification follow implementation of UX10. Keep all draft branches unmerged until owner review.

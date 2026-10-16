# Backlot V166 — Settings Phase S1: Safety and Reliability

**Base:** stacked on V165 Creator Progress / six-phase Quick Actions (`587d56b958cb34dba3c995ee34f22ef20411a9fa`). Draft preview branch only. Stable application is unchanged.

## Product contracts

### Auto Plan (Settings → Planning & Calendar)
- Settings uses the **same 14-day `CreatorWeeklyPlanPreviewEngine`** as Weekly Plan / Automation.
- Enabling is inert until the creator explicitly confirms a dialog explaining how many missing projects may be created.
- Cancel does not enable or generate projects. Disabling does not delete existing projects or weekly templates.
- Already-created, archived, skipped and creator-edited scheduled projects retain ownership.
- No reset or replacement of existing customized schedules.

### Google Calendar
- **No automatic choice of a calendar.** Require permission, an explicit writable Google Calendar destination and creator acceptance.
- Older calendar sync preferences require renewed S1 consent before exporting anything further. Do not automatically migrate an old enabled toggle to permission to share notes.
- Export only project title, due date, platform and content type. **Never export project notes, private recording metadata or reminder details.**
- Sync responds to actual Compose task snapshot updates. Background provider errors must not crash Backlot.
- Before updating or deleting an event, verify its ID still points to a `Creator project ·` entry within the currently selected calendar.
- Deleted, skipped and archived projects have their Backlot entries removed only if verified as app-managed in the selected calendar. A creator-renamed event and any entries in other calendars must not be deleted or overwritten.
- Changing calendar pauses sync until consent is repeated. Previously created events in a different calendar remain there (no silent move, overwrite or deletion).
- Turning calendar sync off leaves existing exported events unchanged. This is explained before consent.
- Show the last successful full sync and honest counts/warnings, not a success message immediately on toggle.

### Backup and restore
- Local portable backups contain creator data and supported Voice Idea recordings, but are **not encrypted**.
- Visual theme, app icon, launch sound, calendar consent/selection, Google identities and tokens are **not restored by portable backup**. Both Settings and the export/restore UI explain that clearly.
- Existing restore validation, pre-restore safety copy, media integrity checks and journal remain unchanged.
- No automatic restore or backup overwrite introduced.

## Acceptance gates
1. Inherit V161–V165 preservation scripts and compile/tests.
2. S1 static checks for confirmation, consent, ownership guard, stale event handling, privacy and backup copy.
3. JVM tests for skipped/archived export eligibility, private notes exclusion, reporting.
4. Android Compose tests for cancel vs confirm of Auto Plan in Settings.
5. Android lint, AndroidTest compilation and V166 APK build.
6. On an Android phone with Google Calendar: choose destination, refuse consent, enable and sync, change title/deadline, manually rename an exported entry (Backlot must not overwrite it), skip/archive/delete projects, switch calendars and disable. Validate actual provider entries.
7. Backup validation: export, inspect listed inclusion/exclusions, restore to test device and verify media, preferences and recovery copies.
8. Creator approval and real-device sign-off required before stable merge.

## Not in S1
- No full Settings home redesign; that is S2.
- No overhaul of creator profile, Appearance, Spark or voice; that is S3.
- No new reminder mechanism or permission workflow; that is S4.
- No cloud backup migration or encryption feature; any future change requires its own design/security review.
- Earlier V165 emulator checks were blocked by Android runner/ADB errors and are not implied to be passing by this source-level work.

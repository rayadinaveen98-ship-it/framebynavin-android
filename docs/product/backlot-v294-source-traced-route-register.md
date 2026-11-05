# V294 — Production Route Register (source-traced, not screenshot-verified)

**Scope:** V292 signed-owner preview source. Each entry below is backed by a route, screen invocation, Manifest entry, or settings enum. We still need emulator screenshot verification, copy-by-copy sign-off and large-font/accessibility checks. Do not treat every old versioned Kotlin file as a live page.

**Priorities:** P0 = task-critical, data-destructive or core navigation; P1 = frequent secondary workflow; P2 = supplementary/help. Each entry must eventually be tested in: fresh-empty, populated, loading/error, long text, small/large phone and permission-denied when relevant.

| # | Entry and route | User-facing destination | Source owner | Priority | Specific audit |
|---|---|---|---|---|---|
| 1 | Startup, launch gate | Welcome / entry | `MainActivity.kt`, `V131LaunchGate.kt` | P0 | No forced demo identity, clarity of sign-in |
| 2 | Account onboarding | Account setup | `V23AccountOnboarding.kt` | P0 | Neutral first run, sign-in options |
| 3 | Creator onboarding | Creator interests and goals | `V18CreatorOnboarding.kt` | P0 | No sample schedule or fake metrics created |
| 4 | Guided first-run | Guided tour | `V20GuidedFirstRun.kt`, `V207GuidedTourPolicy.kt` | P1 | Opt-out, dismissal and repeat frequency |
| 5 | `PTab.TODAY` | Today home | `PTodayScreen`, `V18TodayScreen.kt` | P0 | First-viewport density, focus and empty state |
| 6 | Home slideshow / companion | Today auxiliary media | `V148HomeTopPerformers.kt`, `V2214HomeCompanion.kt` | P1 | Hidden controls, accessible text and motion |
| 7 | `PTab.IDEAS` | Ideas | `V09IdeaVaultScreen` | P0 | Search, capture, empty/archived states |
| 8 | Ideas editor | New/edit idea | `V09IdeaVaultUi.kt`, `V14QuickCaptureUi.kt` | P0 | Small safe form, save confirmation |
| 9 | Voice idea | Recorder and playback | `VoiceIdeaRecorderUi.kt`, `VoiceIdeaPlaybackUi.kt` | P1 | Recording status and controls |
| 10 | `PTab.CREATE` | Projects / Studio | `V131StudioScreen` | P0 | Distinguish project list from creation |
| 11 | Project composer | New/edit project | `V20NewProjectWizard.kt` and composer in root shell | P0 | Primary action and no auto-created demo project |
| 12 | Project detail / workspace | Active project | `V175ProjectDetailsUi.kt`, `ContentWorkspaceActivity.kt` | P0 | Details vs tabs, focus and long notes |
| 13 | Publication | Publish readiness and post-publish | `V181PublicationDialog.kt`, `CreatorPostPublishEngine.kt` | P0 | Explicit confirmation, no false success |
| 14 | `PTab.CALENDAR` | Content calendar | `V15ContentCalendarScreen` | P0 | Dates, schedule provenance, empty state |
| 15 | `PTab.INSIGHTS` | Insights | `V11InsightsScreen` | P0 | No fake stats, disconnected Google/YouTube |
| 16 | Insights detail | Video analytics/drilldown | `V189VideoDetailScreen.kt`, `V20InsightsDrilldownUi.kt` | P1 | Sources and unavailable metrics |
| 17 | `POverlay.WEEK` | Weekly Plan | `PWeekScreen` | P0 | Templates optional, no stealth creation |
| 18 | `POverlay.RELEASE` | Release Day | `V09ReleaseDayScreen` | P1 | Correct schedule and action outcome |
| 19 | `POverlay.DAILY_BRIEF` | Daily Brief | `V15DailyBriefScreen` | P1 | Prioritize actual actionable content |
| 20 | `POverlay.CALENDAR` | Calendar modal | `V15ContentCalendarScreen` (duplicate destination) | P1 | Decide whether duplicate overlay is necessary |
| 21 | `POverlay.AUTOMATION` | Automation Center | `V17AutomationCenterScreen` | P1 | Real vs proposed automations |
| 22 | `POverlay.PROGRESS_JOURNEY` | Creator journey | `V282ProgressJourneyScreen` | P1 | Avoid duplicated progress narrative |
| 23 | `POverlay.CREATOR_PROGRESS` | Creator progress | `V23CreatorProgressScreen` | P1 | Consider consolidation with #22 |
| 24 | Quick actions modal | Actions hub | Root shell `FrameByNavinV101BApp.kt` | P0 | Label actions clearly, reduce duplicates |
| 25 | Reminders switcher | Creator / Personal | Root shell `showReminders` | P0 | Context-specific current selection |
| 26 | Creator Reminders | Creator reminders hub | `V157ReminderCenterUi.kt` / root shell | P0 | No project/personal crossover |
| 27 | Personal Reminders | My day | `V2232PersonalReminderHub.kt` | P0 | One clear focus, discoverable My Spaces |
| 28 | Personal Settings | Routines and permissions | `personalPageName = SETTINGS` | P1 | Remove duplication with universal Settings |
| 29 | Personal reminder editor | Create/edit personal reminder | `V2232PersonalReminderHub.kt` | P0 | Repeat, snooze, music and validation |
| 30 | My Spaces | Spaces index and empty state | `V289PersonalSpacesScreen.kt` | P0 | Explicit labeled entry, sparse empty state |
| 31 | Space detail | Goal, activity, archive | `V289PersonalSpacesScreen.kt` | P0 | Journal + goal separation |
| 32 | Space photo journal | Before/later and privacy | `V290PersonalPhotoSection.kt` | P0 | Original photo unchanged, local-only copy |
| 33 | Space export/import | Metadata-only backup | `V289PersonalSpacesScreen.kt` | P0 | Plaintext and no photos warnings remain |
| 34 | Alert runtime | Personal fullscreen alert | `PersonalAlertActivity` | P0 | Lock screen + permission reality |
| 35 | Alert runtime | Creator alarm / voice | `AlarmActivity`, `VoiceReminderActivity` | P0 | Consistent Done, Snooze and repeat audio |
| 36 | `POverlay.SETTINGS` | Universal Settings home | `V144SettingsHub.kt` | P0 | Remove excessive intro copy, neutral categories |
| 37 | Settings personalization | Appearance/companion/voice | `V167SettingsSection.PERSONALIZATION` and children | P1 | No overlapping preferences |
| 38 | Settings reminders | Timing/permissions/defaults | `V167SettingsSection.REMINDERS` | P0 | Explain only decisions and risks |
| 39 | Settings planning | Weekly planning, Calendar | `V167SettingsSection.PLANNING` | P1 | Separate from generic app defaults |
| 40 | Settings backup | Data, backup and recovery | `V167SettingsSection.DATA` | P0 | Explicit included vs excluded |
| 41 | Settings about | About and tour | `V167SettingsSection.ABOUT` | P2 | Version and helpful support |
| 42 | `POverlay.PROFILE` | Account and creator profile | `V23ProfileAccountScreen` | P0 | Account identity separate from global Settings |
| 43 | Cloud sync Activity | Backup & Recovery | `CloudSyncActivity` in AndroidManifest | P0 | Conflict states, no false full backup claim |
| 44 | Local backup Activity | Export/import | `BackupActivity` in AndroidManifest | P0 | File scope, failures, receipts |
| 45 | Personal backup Activity | Personal recovery | `PersonalBackupActivity` in AndroidManifest | P0 | Existing reminder history and permissions |
| 46 | Recovery copies Activity | Local history and copies | `RecoveryCopiesActivity` in AndroidManifest | P1 | Avoid accidental overwrites |
| 47 | Fresh Start Activity | Device-only erase | `FreshStartActivity` in AndroidManifest | P0 | Keep protected and out of ordinary Settings |
| 48 | Privacy Activity | Data and privacy | `V269DataPrivacyActivity` in AndroidManifest | P1 | Accessible data use and scope |
| 49 | Widget migration Activity | Widget setup/migration | `WidgetMigrationActivity` in AndroidManifest | P1 | Clear previews, no duplicate widgets |
| 50 | Content Workspace Activity | Full project workspace | `ContentWorkspaceActivity` in AndroidManifest | P0 | Accessible edit and cancel flow |
| 51 | Widget quick idea Activity | Device widget capture | `QuickIdeaActivity` in AndroidManifest | P1 | One-action capture and resilient save |

## Known route-complexity findings

- The five primary tabs are clear, but the separate Calendar overlay duplicates the main Calendar destination.
- Creator Progress and Progress Journey are different full-screen routes; evaluate their overlap rather than deleting one by assumption.
- Main Settings already has Reminder defaults, while Personal Settings has additional readiness/routine screens. Align shared permissions in one universal place without removing Personal-specific setup.
- A labelled **My Spaces** action is needed on Personal Today. Icon-only discoverability caused a confirmed user confusion.
- The root shell has a modal Quick Actions hub with many of the same destinations; simplify its content without losing shortcuts.
- AndroidManifest includes multiple utility and notification activities. These count as real screens but are not primary navigation tabs.

## Next acceptance gate

1. Validate which source-traced routes are reachable on V292 through a disposable Android emulator and capture screenshots.
2. Match each route against the generated V294 source-catalog CSV and manually classify all relevant direct/possible UI copy.
3. Mark every route as KEEP, MERGE, MOVE, or RETIRE with a verified replacement path; check accessibility and data retention.
4. Lock typography, reusable components and approved strings BEFORE producing a redesigned APK.

**No deletion, migration, database edits, user content reads or app APK modifications authorized by this register.**

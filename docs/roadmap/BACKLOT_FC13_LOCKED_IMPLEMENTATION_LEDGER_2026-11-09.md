# Backlot FC13 — single-branch implementation ledger

**Scope locked by owner (9 November 2026): exactly 13 earlier feedback items.**
No additional products, phases, companion services, AI features, or extra roadmap
items may be added to this phase. Use one development branch and one draft PR.

- Base: Phase 5 review branch, PR #355; NOT merged or installed on owner's V322.
- Working branch: `feature/backlot-fc13-feedback-completion`.
- Full Gradle/Android emulator/signed APK/device runs are **reserved for final
  integration after implementation**. This does not make untested source
  release-ready. Minimal source review and targeted assertions are allowed.
- No uninstall, force restore, data reset, release signing, test cloud account
  deletion or destructive real-user operations.
- Existing legacy Creator and Personal store authorities must stay intact.
- User-visible "backup complete", "restored" or "account deleted" messages
  require actual authenticated durable service confirmation, never optimism.

## Exactly thirteen acceptance lines

| No. | Required feature (no additions) | Initial verified baseline | Implementation status on FC13 branch |
|----:|---|---|---|
| 1 | Unified Personal + Creator Reminder UI | Shared headings/components, inconsistent screens | **Partial source integrated**: consistent REC-red filters, shared section headings and tab spacing on real hubs; nested review and native QA pending |
| 2 | 21-day single activity challenge, alerts, states, notes and links | One-activity store, Done receipts and scheduler | **Source integrated for Strict/Flexible past-day receipts, per-day notes and optional existing Space/Group links**; full snooze and photo-journal presentation require remaining integration |
| 3 | Simple progressive Personal Reminder creation | Show Suggestions/advanced disclosure in source | **Source integrated**: four-stage flow, suggestions and advanced options collapsed, plus notes collapsed for new reminders; edit notes preserved, full Android QA pending |
| 4 | Integrated Notification Center + Spark assistance | Separate reminder inbox/history/Spark digest | **Source integrated**: existing V306 dual-domain evidence feed now titled Notification Center, Director's Cut filter controls, and a Spark next-action card using only genuine V308 actionable records. Suppressed when either Personal/Creator source is unreadable; runtime/visual QA pending |
| 5 | Premium New Idea entry | Existing typed/voice capture and Idea Vault | **Source integrated**: cinematic one-focus capture surface, five universal writing prompts, existing voice/dictation and save authority; visual/device acceptance pending |
| 6 | Replace Dictate orb with reactive dot field | FramePulseOrb still present | **Source integrated**: old renderer replaced with real-recognizer-RMS dot field; final animation and accessibility QA pending |
| 7 | Content Workspace five zones | Existing Plan/Script/Publish, Phase4 enhancements | **Source integrated**: Overview, Plan, Content, Production, Activity panels and canonical editor routes, no new project store; final UI QA pending |
| 8 | Spark notification icon/expression set | Prior Spark visual policy | **Partial source integrated**: original Spark WebP for private medium-size notification art and CinePulseGuide poses on full Personal alert screens; existing monochrome Backlot status-bar icon retained. Spark-specific small silhouette and owner visual/device QA remain pending |
| 9 | Personal urgent/full-screen alert reliability | Existing permission/fallback logic | **Partial source integrated**: alarm-only FSI flag now excludes follow-ups/denial; special access rechecked when opening alert activity; Today/Settings explain tappable fallback and offer Android access settings. Existing notification and private Done/Snooze remain; Samsung permission/reboot and signed-upgrade QA pending |
| 10 | Premium audio-reactive dot/wave/bloom treatment | Some isolated effects | **Partial source integrated**: Dictate continues to use genuine recognizer RMS; Personal Alarm/Voice now emits token-scoped real MediaPlayer/TTS start/stop receipts to drive a restrained REC-red particle wave/bloom in the existing alert UI. Playback stage is NOT measured output amplitude; Creator reminder audio meter and Android performance QA remain pending |
| 11 | Reminder Groups pause/resume and genuine daily progress | Group links without actions | **Source integrated**: `FC13ReminderGroupOperations.kt` and existing `V298ReminderGroupsScreen.kt`, pending source review and final QA |
| 12 | Universal Backup / Restore / Delete Cloud Backup | Parked Trust draft PRs #343–#353, unmerged | **Safety disclosure integrated, feature NOT IMPLEMENTED**: Data & Privacy now identifies legacy Sync Now as incomplete, explicitly names uncovered challenge/Space/photo data, and never offers false universal Back Up / reviewed Restore / Delete Cloud Backup actions. Existing legacy manual exports remain available. Trust backend, verification, data coverage, two-device recovery and deletion remain pending |
| 13 | Complete Backlot account deletion | Sign-out only | Pending, never enable before authenticated cross-domain deletion proof |

## Work contract

1. Implement *only* the thirteen feature lines in the above table.
2. Reuse existing Personal/Creator stores, project editor, groups and Space data;
   do not create parallel data sources.
3. Any bulk reminder operation must be user-confirmed, preserve exact Done
   receipts, and perform Android alarm/notification reconciliation only
   AFTER the Personal DataStore transaction commits. UI must disclose that
   one reminder can be in several groups.
4. Challenge completion and Group progress must derive only from explicit
   Done events; never from notification delivery, expiry, schedule or UI views.
5. Integrate existing Trust source carefully, with owner-specific key and
   complete asset coverage; unfinished remote/backend work stays disabled.
6. At the end of FC13 feature changes, one final acceptance pass includes
   full compilation, unit tests, Android11/15, background/alarm edge cases,
   visual screenshots at 2× font, signed in-place V322 upgrade, exact daily
   receipt/photo/voice original preservation, two-device restore and explicit
   verified account/cloud deletion on disposable accounts.

**Status distinction:** "source integrated" != "compiled" != "Android tested"
!= "owner accepted" != "released". Update this table only when there is an
actual corresponding committed source change.

### FC13 Spark + Personal full-screen permission boundary (source review only)
- Small system icon remains the approved monochrome Backlot vector, not an unapproved colored Spark raster. Private medium notification art uses approved original Spark WebPs scaled to a maximum 192px, and can be safely omitted if decoding fails.
- Personal public lock-screen notification stays generic and has no large Spark illustration, private title or notes.
- PersonalFullScreenPolicy.eligible remains the FSI gate. Its result controls both the notification fullScreenIntent and alarm-visual opening extra. The Activity also rechecks Android special access when opening.
- Denied FSI, follow-up, Voice and ordinary notification cases retain existing content opening and original token-scoped Done/Snooze/Dismiss actions.
- Existing synthetic Android tests were extended for private/public icon separation and locked/unlocked Spark UI. They were not executed, and this source is not APK-ready.
- Android may show a heads-up notification rather than full-screen. Owner permission, Play eligibility and Samsung behavior require final release acceptance.

### FC13 audio and Trust continuation (source-only, no APK)
- New FC13PersonalPlaybackVisualState keeps only an ephemeral in-process reminder ID + exact occurrence token + actual ALARM_RINGING / VOICE_SPEAKING stage. It does not capture sound, read audio files, or create a second reminder authority. MediaPlayer's successful start sets the ringing state, TTS onStart/onDone set/clear speaking, and every stop/preemption clears it. No automatic Done is written.
- V2272PersonalParticleWave draws a restrained REC-red bloom and slightly stronger movement only while the matching actual playback stage is active, with reduced motion and power saver respected. Genuine microphone-RMS animation stays in the existing Dictate component. The user's request for true output-amplitude response is not claimed complete because MediaPlayer/TTS do not expose amplitude to this UI.
- Pure playback receipt tests are written (cross-reminder token isolation, stale clear, invalid/silent input) but not executed. Android service, TTS race, alarm, locked-screen and accessibility QA remain deferred.
- Data & Privacy now explicitly differentiates limited legacy Sync Now from not-yet-available universal encrypted Back Up, owner-reviewed Restore and Delete Cloud Backup. Existing separate manual export actions are retained. This is disclosure and safety preparation, not remote backup implementation or cloud deletion.
- No Trust PR #343–#353 was merged; legacy PersonalCloudSyncManager blocks challenge-losing operations, but it cannot claim universal coverage. No real account or remote data was accessed.

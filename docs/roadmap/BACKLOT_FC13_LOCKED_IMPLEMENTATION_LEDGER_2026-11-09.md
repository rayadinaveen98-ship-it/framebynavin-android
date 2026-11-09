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
| 2 | 21-day single activity challenge, alerts, states, notes and links | One-activity store, Done receipts and scheduler | **Source integrated**: Strict/Flexible receipts, notes, Space/Group links, exact-day 10-minute snooze action/receiver and navigation to an existing Space photo journal. Compile, device delivery and stale-occurrence QA remain pending |
| 3 | Simple progressive Personal Reminder creation | Show Suggestions/advanced disclosure in source | **Source integrated**: four-stage flow, suggestions and advanced options collapsed, plus notes collapsed for new reminders; edit notes preserved, full Android QA pending |
| 4 | Integrated Notification Center + Spark assistance | Separate reminder inbox/history/Spark digest | **Source integrated**: existing V306 dual-domain evidence feed now titled Notification Center, Director's Cut filter controls, and a Spark next-action card using only genuine V308 actionable records. Suppressed when either Personal/Creator source is unreadable; runtime/visual QA pending |
| 5 | Premium New Idea entry | Existing typed/voice capture and Idea Vault | **Source integrated**: cinematic one-focus capture surface, five universal writing prompts, existing voice/dictation and save authority; visual/device acceptance pending |
| 6 | Replace Dictate orb with reactive dot field | FramePulseOrb still present | **Source integrated**: old renderer replaced with real-recognizer-RMS dot field; final animation and accessibility QA pending |
| 7 | Content Workspace five zones | Existing Plan/Script/Publish, Phase4 enhancements | **Source integrated**: Overview, Plan, Content, Production, Activity panels and canonical editor routes, no new project store; final UI QA pending |
| 8 | Spark notification icon/expression set | Prior Spark visual policy | **Partial source integrated**: original Spark WebP for private medium-size notification art and CinePulseGuide poses on full Personal alert screens; existing monochrome Backlot status-bar icon retained. Spark-specific small silhouette and owner visual/device QA remain pending |
| 9 | Personal urgent/full-screen alert reliability | Existing permission/fallback logic | **Partial source integrated**: alarm-only FSI flag now excludes follow-ups/denial; special access rechecked when opening alert activity; Today/Settings explain tappable fallback and offer Android access settings. Existing notification and private Done/Snooze remain; Samsung permission/reboot and signed-upgrade QA pending |
| 10 | Premium audio-reactive dot/wave/bloom treatment | Some isolated effects | **Partial source integrated**: genuine Dictate recognizer RMS, Personal Alarm/Voice token-scoped playback receipts, and Creator MediaPlayer/TTS lifecycle receipts connected to existing reminder wave and Spark. Output amplitude is NOT measured; service lifecycle, performance and Android QA are pending |
| 11 | Reminder Groups pause/resume and genuine daily progress | Group links without actions | **Source integrated**: `FC13ReminderGroupOperations.kt` and existing `V298ReminderGroupsScreen.kt`, pending source review and final QA |
| 12 | Universal Backup / Restore / Delete Cloud Backup | Parked Trust draft PRs #343–#353, unmerged | **Partial source groundwork, NOT a functioning universal backup**: legacy limitations disclosed; existing Trust inventory/frozen-source/encrypted local review modules integrated with non-seeding Creator readers and startup stage cleanup; newer Personal archives protected against legacy sync downgrade. Separate review-only owner-scoped SQL, immutable encrypted cloud transport, and opt-in fenced Delete Cloud Backup coordinator added but NOT deployed or enabled. All-domain source coverage, genuinely restorable encrypted backups, qualified cross-store snapshot, owner restore, two-device validation and final device QA remain pending |
| 13 | Complete Backlot account deletion | Sign-out only | **Partial source contract only, NOT an operational account deletion**: existing RC17 Auth-last deletion core has a new server-only extension contract for Personal and Trust records/Storage, alongside pre-existing offline queue. No production adapter, live write fence, server reauthentication endpoint, worker or completion receipts have been deployed; never enable or claim deletion before end-to-end disposable-account proof |

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


### 9 November continuation — integrated source status, not tested
- Reused reviewed Trust source files from the pre-existing Trust branch rather than re-implementing them; connected TaskStore, ScriptStudioStore and WeeklyScheduleStore non-seeding inspection readers. The Trust T1C3 startup staging cleanup has a guarded MainActivity hook. None of this authorizes a complete cloud archive claim.
- The single-activity challenge has an owner-initiated 10-minute snooze persisted on the existing challenge store, exact-occurrence notification action/receiver, Android alarm reconciliation, and a route into an already-linked Space photo journal. The owner's saved Done instants remain the only completion authority.
- Creator Voice and Alarm services now emit actual TTS/MediaPlayer lifecycle states to existing particle, wave and Spark surfaces. Visual stage is NOT captured audio amplitude. Android lifecycle/permission/device proof is pending.
- Added review-only Supabase SQL for owner-only immutable ciphertext backup revision CAS and explicit owner-fenced Delete Cloud Backup; not applied to the live project. Added app-side authenticated transport and fenced deletion coordinator source. All-domain capture and owner-reviewed restore are NOT wired or launchable; the trust sources deliberately fail closed for partial coverage.
- Added an offline server-only extension interface to the existing Auth-last account-deletion core for Personal v2/Trust domains and related Storage. There is no deployed production writer/worker, so complete account deletion remains disabled.
- PR #356 remains a DRAFT, stacked on other unmerged PRs. No Gradle compile, unit tests, emulator tests, cloud migrations, user-cloud writes, signed APK or V322 in-place upgrade occurred in this continuation. These checks must take place only after all remaining FC13 source integration is complete as locked by the owner.

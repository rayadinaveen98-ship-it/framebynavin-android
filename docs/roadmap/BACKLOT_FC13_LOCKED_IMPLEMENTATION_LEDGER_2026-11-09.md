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
| 4 | Integrated Notification Center + Spark assistance | Separate reminder inbox/history/Spark digest | Pending |
| 5 | Premium New Idea entry | Existing typed/voice capture and Idea Vault | **Source integrated**: cinematic one-focus capture surface, five universal writing prompts, existing voice/dictation and save authority; visual/device acceptance pending |
| 6 | Replace Dictate orb with reactive dot field | FramePulseOrb still present | **Source integrated**: old renderer replaced with real-recognizer-RMS dot field; final animation and accessibility QA pending |
| 7 | Content Workspace five zones | Existing Plan/Script/Publish, Phase4 enhancements | **Source integrated**: Overview, Plan, Content, Production, Activity panels and canonical editor routes, no new project store; final UI QA pending |
| 8 | Spark notification icon/expression set | Prior Spark visual policy | Pending |
| 9 | Personal urgent/full-screen alert reliability | Existing permission/fallback logic | Pending |
| 10 | Premium audio-reactive dot/wave/bloom treatment | Some isolated effects | **Partial**: real-audio reactive dot/wave in Dictate; reminder alerts/Spark and consistency not yet integrated |
| 11 | Reminder Groups pause/resume and genuine daily progress | Group links without actions | **Source integrated**: `FC13ReminderGroupOperations.kt` and existing `V298ReminderGroupsScreen.kt`, pending source review and final QA |
| 12 | Universal Backup / Restore / Delete Cloud Backup | Parked Trust draft PRs #343–#353, unmerged | Pending, never enable before safe service and data coverage |
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

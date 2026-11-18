# Backlot UX08F — Cumulative UX08 integration audit & UX09 multi-device sync handoff

**Date:** 2026-11-17
**Scope:** SOURCE INSPECTION / UX08F documentation only. **No runtime evidence, Gradle, JVM, CI, Android tests, APK, merge, release, Supabase/project switch, server migration or cloud write.**
**Stacked UX08 parent:** UX08E draft PR #421 at `a1c13694bd0ce2a546b7912c780b00a137f6732f`.
**Protected owner-accepted V337 stable:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged, and still requires reviewed reconciliation of 19 accepted P0 correction commits touching 22 paths.
**Founder goal for UX09:** A Google-authenticated user logging into two devices expects saved reminders, projects, ideas, challenges and other meaningful Backlot work on one device to **appear on the other**, with practical local-first reliability, privacy, low cost and safe account controls. A successful login alone does NOT provide that parity.

## 1. UX08 source-staging ledger (not release status)

| Stage | Draft PR | Existing source advanced | Integration contract |
|---|---:|---|---|
| UX08A | #417 | Quick Actions/Progress/Rewards/Brief/Calendar inventory | Source truth, owner routes, 36 test requirements |
| UX08B | #418 | `V282ProgressJourney`, Home preview, pure policy | Actual Creator events, Personal Done optional/off, 100-event list bound, typed timeline decoder gap open |
| UX08C | #419 | Creator newly credited reward UI, Spark/Mira three-second rendering | Original CreatorRewardStore and ViewModel queue; no replay/duplicate XP, Personal points not authorized |
| UX08D | #420 | Original Daily Brief source groups and missing project-load UI | Original priority/nudges, saved project vs weekly occurrence, Weekly Plan typed-read gap |
| UX08E | #421 | Original Content Calendar Week/Month/Agenda, 90-date future planning and 80-row paging | Original recurrence dedup and Project/Weekly routing, no historical publish claim |
| UX08F | CURRENT | Existing `V161QuickActionsHub` grouped shortcuts; true task counts, confirmed Skip | All 12 original route tags preserved, real overdue actions, no new data stores |

**UX08A–F are isolated source staging, not compiled or tested.** They are not merged to stable/main and cannot yet be called a new stable APK or completed release candidate. Every stage retains deferred QA and mandatory owner approval.

## 2. Existing cloud architecture — actual repository findings

Source owners inspected from the same UX08E staged ancestor:
- `cloud/CloudSyncManager.kt`: Google sign-in through the existing Supabase auth API, generation-fenced session/account transitions and refresh. Protects against stale account sessions.
- `cloud/CloudAccountGate.kt`: mutex and session generation checks to avoid cross-account token operations.
- `cloud/CreatorCloudSyncManager.kt` and `cloud/CreatorCloudReconciliationPolicy.kt`: **local-first snapshot sync** with full snapshot hash/revision, cloud-head compare, explicit `keepThisPhone` / `restoreCloud` conflict actions. The existing contract says concurrent two-device/account changes must not be automatically overwritten.
- `cloud/CreatorCloudSyncWorker.kt`: a worker may enqueue sync after original Creator writes; this is NOT proof each remote change instantly appears on another currently open device.
- `cloud/PersonalCloudSyncManager.kt`: separate Personal reminder snapshot/hashes/revisions and explicit `keepThisPhone` / `useCloudCopy` conflict resolution. **Key blocker:** `PersonalCloudChallengeCoveragePolicy.requireFullCoverage` currently refuses Personal cloud sync when a single-activity 21-day Challenge is present; its own error message states this domain is not included in Personal cloud sync yet.
- `cloud/PersonalArchiveCloudV2Manager.kt`: separate encrypted archive object flow with owner-approved preview, conflict inspection and explicit restore; not interchangeable with the legacy Personal JSON sync API and not automatic per-record device parity.
- `cloud/CloudApiClient.kt`, `cloud/CloudModels.kt`, `cloud/CloudConfig.kt`: existing API URLs/auth headers and published app config; **the staged source currently references the earlier cloud project**, so a separately approved, verified cutover to the founder's newly provisioned Supabase project is prerequisite. Never commit service-role/secret keys, share private URLs/credentials in documentation or silently replace existing config.
- `data/CreatorBackupManager.kt`, `personal/PersonalBackupTransfer.kt`, `data/V341UnifiedBackupManager.kt` and V342–V354 recovery owners: independently versioned backup/restore and forensic precommit checkpoints. Sync must not overwrite, bypass, or be mistaken for full coverage of these safety guarantees.

**Actual baseline:** Session support, some Creator snapshot sync, Personal reminder snapshot and separate encrypted Personal archive already exist. **Not proven:** realtime multi-device replication for all Backlot entities. Treat "autosync is built" as an inaccurate release claim until explicit coverage, RLS, migrations and device proof exist.

## 3. Proposed UX09 authoritative domain inventory & protection matrix

| Domain | Current owner / source of truth | UX09 outcome needed | Critical risk |
|---|---|---|---|
| Creator Projects and stages | Original CreatorTask store/Creator workflow receipts | Per-owner synchronized mutations including stage history and retained stable IDs | Two-device stage completion races, false XP, duplicate Creator alarms |
| Ideas, Quick Capture, attachments | Original CreatorIdea store and capture media | Sync safe text/state with media as separately consented/size-bounded objects | Loss of private drafts/audio, duplicate idea IDs |
| Weekly Plan and Calendar materialization | Original `WeeklyScheduleSlot`, task `scheduleOccurrenceKey` | Same recurring slot + future occurrence identity across devices | Double project generation/false publish deadline |
| Creator reward ledger | `CreatorRewardStore` unique `eventKey` | Stable per-account idempotent credited events and later local projection | XP inflation, imported historical celebration replay |
| Personal reminders and Done | `PersonalReminderStore` exact owner IDs and `PersonalCompletionJournal` accepted receipt IDs | True mirrored reminders and response receipts, with per-device AlarmManager schedules | Double firing across devices, false Done from notification delivery, loss of Snooze |
| Single-activity 21-day Challenges | `Phase1HabitStore` owner target-day confirmations, notes/voice preferences | Complete encrypted/safely merged domain first; **remove current deliberate sync blocker only after full coverage** | Target date vs actual confirmation confusion, missed/duplicate alerts, silent discard |
| My Spaces and manual activity | Room `V289SpacesDatabase`, Space journal/metric/note IDs | Source-complete owner-bound sync with reliable archive, linked IDs and restore | Private notes disclosure, non-cascading/unsafe deletes |
| Space Photo Journal | App-private metadata-stripped photos with recovery and per-Space references | Separate encrypted/blob sync choice, upload bandwidth/cost gate, recoverable access | Lost photo, sensitive metadata, size/cost exposure |
| Personal Reminder Groups | `V298ReminderGroupStore` stable Personal ID membership | Merge memberships by ID, no duplicate reminders or phantom links | Group bulk Pause/Resume races and wrong Device alerts |
| Personal + Creator progress | Source-backed journal, Challenge confirmations, stage events, Creator XP ledger | Recompute read-only projections from synchronized canonical records | False achievements, private Personal leak into Creator timeline |
| Saved Settings/Companion | Existing account/local preference stores, Spark/Mira choice | Explicit classify device-local vs account-scoped preferences and migrate deliberately | Cross-device alarm/voice conflicts, unwanted companion swaps |
| Notification delivery, Snooze, full-screen alarms | Android device-specific scheduler / P0 delivery ledger | Keep per-device scheduled mechanics and account-scoped user intent distinct | Duplicate audible alerts, unreliable exact AlarmManager token validation |
| Backup/Restore/Account deletion | Owner-protected V337 P0 recovery and account privacy states | Verifiable export/restore/delete coverage, account switching gate, tombstones | Silent overwrite, data resurrection, incomplete deletion |

Important: **Syncing the same reminder to two devices does not automatically mean only one device will ring.** Product policy must decide whether both devices are expected to schedule/deliver or whether a "primary device" is required; either requires explicit in-app disclosure and race-safe response handling. Don't promise real-time identical Android permissions or lockscreen behaviour across phones.

## 4. UX09 proposed source-stage sequence (NOT implemented)

1. **UX09A — Full existing auth/cloud/domains/coverage/RLS/backup inventory and cutover plan:** inspect exact endpoint/env, public publishable key vs forbidden service secret, database schema, RLS/grants, trigger/version contracts, existing remote tables, auth redirect/Google OAuth. Identify which new Supabase resources actually exist. A fresh account avoids old QA-user preservation need only when the founder explicitly approves that *isolated* test data reset; it does not authorize rewriting existing users' data.
2. **UX09B — Canonical per-entity sync model and identity/security design (approval gate):** choose owner-scoped stable ID keys, object/version clocks, deletion tombstones, origin device, source write attribution, idempotency, offline queue, 2-device conflict policy, media encryption, RLS and GDPR/Indian privacy considerations. Define stop-the-line invariants. **No blind 'last-write-wins' for irreversible user content.**
3. **UX09C — Supabase schema and security foundations (separately approved server work):** migrations on new isolated database with user/owner RLS, deny untrusted writes, test secondary account isolation. No service role in APK. Estimate free-tier versus expected storage/egress/realtime costs **before any paid service**.
4. **UX09D — Creator projects/ideas/weekly source sync (isolated data path):** preserve offline/local-first editing, journal revision, conflict UI and recurrence uniqueness, no replay XP. Support explicit first-device and second-device adoption sequence.
5. **UX09E — Personal Reminders/Done/Challenges/Groups/Spaces/Notes/Photo metadata (separate protected domain packages):** complete 21-day Challenge coverage and conflict tests *before* disabling existing protective block. Photos separate byte-encryption and quota/consent gate. Keep user-private Personal sources separate from Creator progress.
6. **UX09F — Real device sync UX / background retries / account switch / backup/erase/delete guards:** visible pending/synced/conflict/offline statuses, sign-in/restore flow, retry/queue, notifications and token-based Done idempotency. Reconnection never replays credited celebrations or produces duplicate Android reminders. Account deletion means remote+local correct policy, not merely logging out.
7. **UX09G — Source integration handoff to UX10:** trace all source domains, annotate migration and recovery limitations, preserve V337 owner-stable lineage; no merge/release. Consolidated build/unit/instrumentation/two-device physical certification after UX10 with founder approval.

### Practical two-device acceptance experiment (post-UX10)

- Sign into Device A and B as the **same** validated account, with distinct physical Android installations; also use a second account as an isolation adversary. Check token refresh, logout and device revocation.
- Offline A: create a Creator idea/project, edit stage, Personal reminder and Challenge confirmation while B modifies independent records. Bring A/B online in either order; no IDs overwritten, one expected record each, no skipped conflict presentation.
- Concurrent same-record edits: detect conflict and preserve both recoverable intentions, not silently overwrite a note, completed day, project deadline or journal entry.
- Confirm a Personal Done on A; B sees accepted receipt, UI chronology and original ID. Never append a second Done just because both devices received Android notification. Confirm policy for ringtone/voice alert duplication across physical devices.
- Test Challenge date catch-up, Space notes/photos, Group links, Creator XP stage credit, archive/delete tombstones, backup/restore and full account erasure without resurrection.
- Check one device offline for days, device time drift, timezone change, slow/spotty network, Supabase auth token expiration, RLS isolation and changes from the other user's account.
- Verify per-domain Sync Coverage screen: **Pending / Synced / Conflict / Unavailable / Not Covered**, with real counts/timestamps, not a single misleading "All synced" label.

## 5. Approval, cost, security and roadmap gates

- **Founder approval needed before UX09B architecture and UX09C cloud schema/backend changes.** This audit is a proposal, not hidden authorization for a new database/security deployment or migration. No paid plan/subscription/services assumed.
- No new Supabase account project URL or keys are committed in UX08F. Old `CloudConfig` mismatch is a visible **blocker** for UX09A verification, not permission for an unreviewed endpoint swap.
- RLS is a mandatory server-enforced security boundary, not a client flag; cross-account tests and no service-role credentials in Android.
- Multi-device data sync differs from restore and does not replace P0 accepted backups and recovery writes. Upgrade/rollback safety must preserve old V337 local data and cloud snapshots; account switch requires an explicit protected preview, no silent cross-user adoption.
- Avoid unbounded realtime subscriptions and heavy private media uploads by default; begin with costed incremental change feed and reliable fallback. Confirm quota/egress projections before paid provider changes.
- UX10's app-wide design density/typography/first-launch, notification, error/offline, accessibility, QA and release gate must receive UX09 states. Owner controls release after real full-stack/physical verification.

**Current truth:** UX08 stage source-complete on isolated drafts only; UX09 source implementation **NOT STARTED**. A secure fully automatic two-device experience remains a major, independently scoped architecture and testing stage.

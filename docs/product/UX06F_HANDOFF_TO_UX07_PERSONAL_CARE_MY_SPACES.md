# Backlot UX06F → UX07 Handoff: Personal Care & My Spaces

**Date:** 2026-11-17
**Current:** UX06F source audit and one narrow P0 compatibility fix staged; no compile, CI, APK, merge or runtime certification.
**Next approved roadmap point:** UX07 — Personal Care & My Spaces, source-stage implementation only. Preserve V337 and the existing founder-approved meaningful feature set. Consolidated QA after UX10.

## 1. Approved UX07 mission — refine, do not replace

Create a calmer, more useful Personal experience with a coherent Home, My Day, reminders/agenda, Spaces, photos/journaling, routines, Reminder Groups and 21-day Challenge progress. The goal is richer daily usefulness without turning private care into a pressure score or inventing completion. This is **not** a new Personal app, a new backend or a rewrite of core stores.

Preserve these exact visible workflows:
1. Personal Home / My Day with relevant reminders, existing routines, challenge status and safe access to the existing My Spaces.
2. My Spaces index → existing Space detail → manual log/progress entries → notes/journal → private photo journal/management → linked existing Personal reminders → safe archive/reopen.
3. Personal reminder create/edit/details, recurrence, voice/alarm/notification, groups, explicit Done/Snooze/Dismiss and history.
4. Reminder Groups create/edit, linking/unlinking and navigation; deleting a group must NOT delete a reminder.
5. Unified Personal Agenda as a read-only mix of Personal reminders and current single-activity challenges, with source-specific actions and no inferred Done.
6. Single-activity 21-day Challenge including edit/notes/strict or flexible confirmation, Undo, alert choices, linked Space/group and preserved exact daily confirmation receipts.
7. Legacy V297 challenge records and existing owner-specific backup/restore safeguards are not silently migrated.

## 2. Verified source boundary map

| Concern | Existing source anchor | Contract for UX07 |
|---|---|---|
| Space persistence and reminder links | `personal/spaces/V289SpacesDatabase.kt` | Independent Room `personal_spaces`, `personal_space_reminder_links`, `personal_space_entries`; links by stable IDs, no cloning Personal reminders |
| Space progress | `personal/spaces/V289SpaceProgressPolicy.kt` | Separate manual check-ins from confirmed Done receipts; dates are timezone-aware and future-dated events cannot inflate progress |
| Space photos | `personal/spaces/V290PrivatePhotoFiles.kt` and `V290SpacePhotos.kt` | Owner-opt-in app-private files; bounds, orientation normalization, EXIF/GPS stripping; no invisible network upload/sync |
| Space metadata/recovery | `V289SpacesMetadataCodec.kt`, `V289SpacesMetadataRecovery.kt`, `V290InterruptedPhotoRecovery.kt` | Preserve safe recovery, partial failure handling, and photo/metadata consistency |
| Space screens | `ui/V289PersonalSpacesScreen.kt` and `ui/V290PersonalPhotoSection.kt` | Improve navigation/cards/details without substituting dummy data or new canonical writers |
| Reminder Groups | `personal/V298ReminderGroupStore.kt` and `ui/V298ReminderGroupsScreen.kt` | Group membership stores Personal reminder IDs only; deleting group does not delete reminder |
| Unified agenda | `personal/V337UnifiedPersonalAgenda.kt` | Existing read-only Personal + current Challenge projection; no second alarm, no new cross-domain store |
| Personal reminders | `personal/PersonalReminderStore.kt` and `ui/V2232PersonalReminderHub.kt` | Original canonical owner, existing 4-step editor and response semantics remain |
| 21-day Challenge | `personal/Phase1HabitStore.kt` / `Phase1HabitCodec.kt` / `Phase1HabitLiveDelivery.kt` | Preserve exact receipt history, v1/v2/v3 codec and alarm identity; UX06F P0 Snooze fix must survive |
| Personal home + routine | `ui/V288PersonalTodayCard.kt`, `ui/V295UniversalPersonalRoutines.kt`, `ui/V340HomeSpacesEntry.kt` | Reuse existing actual source data and routes; no fake activity/streaks |

The source anchors above were discovered/read from the current cumulative draft branch; this is an architecture handoff, not a claim that all UX07 code was deeply audited or tested.

## 3. UX07 proposed implementation slices — no scope expansion

**UX07A — map and protection contract (source/docs):** Inventory actual Home/Spaces/group/challenge UI and route callbacks; record each writer, ID, photo file lifecycle, serialization and recovery owner. Add acceptance flows and mark any proposed schema changes for separate owner approval. No destructive migration or account action.

**UX07B — Personal Home / My Day hierarchy:** A clear "what matters today" view based on actual Personal Agenda, reminders, routines and unfinished manually acknowledged challenges. Read-only projection and fast links to the existing Space/detail/Reminder Group; preserve offline behaviour, empty states and accessibility. Show **No activity yet** rather than false progress.

**UX07C — My Spaces cards, detail and journaling:** Stronger, calmer cards; clear intent, recent owner entries, linked reminders and history; tidy action hierarchy for Log Progress, Notes/Journal and Photo Journal. Preserve existing metric suggestions as optional, not imposed; maintain local data and accurate timestamps.

**UX07D — Reminder Group and Space linking:** Distinct, predictable navigation for link/unlink, grouped agenda and existing Personal reminders. No copying reminder data or cascading deleted reminder records when removing a Group or Space link. Display a stale-link recovery state where necessary.

**UX07E — Challenge context and reflection:** Place current single-activity Challenge in Personal day/Space context using original challenge store and explicit completion receipts. Never treat Voice/Alarm delivery, opening a journal, viewing a photo or adding notes as a successful day; no punitive broken-streak behaviour.

**UX07F — static integration handoff to UX08:** Review all UX07 patches, source overlap with UX06 Personal hub/Challenge/Agenda, backup and photos, draft QA tests and UX08 achievement boundaries. Preserve test deferral until the one post-UX10 integrated campaign.

These slices organize previously approved Personal features. They are **not** approval for new hardware integrations, a health diagnosis engine, analytics platform, generative AI advice, photo cloud synchronization or monetization.

## 4. Core task paths and empty/error behavior

`Home → Personal → My Day (real agenda) → [Personal reminder / Challenge / Space / Reminder Group] → matching source-owned detail → optional explicit action → source store confirms durable result → linked history/progress refreshes`.

`My Spaces → select existing Space → read entries, notes and photos → Add manual log / note / opt-in photo / Link existing reminder → owner-approved save → show source-tagged history`.

`Reminder notification → valid original owner action (Done/Snooze/Dismiss/Stage Done as appropriate) → authoritative receipt in its own store → only then project completion into Space progress`.

Empty: show a useful first-action and privacy context, not invented zero-to-one achievements.
Loading: no stale private content or double-submit.
Error: preserve editor contents, explain retry and source-of-truth; do not replace a corrupted store with empty data.
Offline: existing local flows remain actionable; cloud parity is UX09 and must not be assumed in UX07.
Upgrade/erase/reinstall: explicitly disclose that app-private photo bytes and local-only audio documents are not magically restored by a generic JSON export, account sign-in or second-device session.

## 5. Privacy and wellbeing rules

- Photos are private and opt-in. Source metadata stripping/re-encoding and current recovery rules must stay. Never expose file paths or embed personal images in notifications, public previews, widgets, analytics or diagnostic logs.
- A Space is not a medical record. Optional self-reflection/wellbeing templates can be considered only in clearly labeled future approval; never make clinical claims or ask for health-device access.
- No streak punishment, pressure animation, shame copy or compulsory metrics. Prefer gentle history and owner-initiated check-ins.
- Explicit Done receipts, manual check-ins and ordinary notes are distinct. Avoid fabricated productivity percentages and duplicate cross-domain scoring.
- No paid service, new data collection, new Supabase tables, subscription or cloud media migration without separate cost/security and founder review.

## 6. UX07 acceptance criteria (deferred runtime verification)

1. Existing user Spaces, links, photos, entries, notes, archived state, Personal reminders/groups and challenge receipts survive V337 → integrated candidate upgrade.
2. Every link opens the same canonical item; stale/deleted links fail safely and do not delete source-owned data.
3. Manual entry and confirmed reminder Done remain visually distinguished; notifications, Snooze, Dismiss and mere app launches never count as Done.
4. Photo import/delete, interrupted write recovery and app-private privacy remain intact; no silent gallery permission escalation.
5. Back navigation, cancellation, rapid taps, process death, accessibility large type/TalkBack, landscape and tablet are safe.
6. No regressions to UX06 reminder functionality, Android lockscreen privacy, 21-day v3 format or accepted P0 Snooze fix.
7. Final Stage UX07 remains source-staged until the post-UX10 unified QA gate executes and the founder accepts its physical build.

## 7. Order of work

Start with **UX07A** against the isolated UX06F branch after its draft PR is opened and reviewed as the new stacked parent. No source rewrite of Spaces or local photos is authorized merely by the UX06F handoff. If baseline P0 divergence blocks a safe change, record that blocker rather than altering V337 or rewriting protected owner data.

**UX06F handoff verdict:** approved Personal feature inventory recovered, source boundaries identified, implementation sequence and post-UX10 acceptance drafted. **UX07 not yet implemented.**

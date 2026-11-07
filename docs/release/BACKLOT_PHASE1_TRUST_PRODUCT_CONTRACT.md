# Backlot Phase 1 — trust-first product contract (DRAFT, user visual approval required)

**Baseline:** Build 320 / commit `f5694e0ba4c64775c5a3135f1a3f4a08c65ed05a`.
**Branch:** `feature/backlot-phase1-trust-review`.
**State:** architecture/audit and screenshot proposals only; **not an implemented feature**.

## User approval boundary

- Do not change production visuals, icons, motion, colors, layout, or navigation until user explicitly approves a screen screenshot or a named visual direction.
- Mockup PNGs in the current conversation are visually inspectable HTML/CSS captures, **not Android APK screenshots** and **not live Backlot UI**.
- After approval, implement exactly the selected visuals in Android Compose, then capture actual Android emulator screenshots for side-by-side review before release.
- Freeze Build 320 and do not merge this work into the release branch until behavioral tests + screenshot approval + in-place upgrade acceptance pass.
- Preserve existing user projects, reminders, cloud data and previous 21-day challenges; no production account/data deletion while developing.

## Evidence audited from Build 320

1. `CreatorCloudSyncManager.syncNow` delegates to `syncAuthenticated`, whose reconciliation chooses upload, restore, no change, or conflict. It then calls a **separate** `PersonalCloudSyncManager.syncNow`. This is not a universal backup protocol.
2. `PersonalCloudSyncManager` exports Personal reminders/history only. Neither the Creator nor Personal cloud head currently proves full Challenges, Groups, Space notes, private JPEG and all media coverage.
3. `V297Challenge` stores **21 separately editable activity strings** and user-confirmed completion, and `V297ChallengeScreen` explicitly says it does **not** create alarms.
4. `V298ReminderGroup` is currently a tagged collection of **existing Personal reminder IDs**; linking doesn't schedule anything. Bulk scheduling/pause is not a shipped group capability.
5. `PersonalFullScreenPolicy.eligible` returns true only for original `PersonalAlert.ALARM` with FSI access, not for VOICE or ordinary NOTIFICATION.
6. The account-deletion RC17–RC24 files describe synthetic/offline safety groundwork; live authenticated deletion and a working public deletion request path are **not established by those tests**.

## Phase 1 target — technical contracts before UI implementation

### P1-A · Explicit universal cloud vault

- Three separate commands: **Back up**, **Restore**, **Delete cloud backup**. No normal 'Sync Now' call may silently restore remote records. Do not relabel a partial sync as a complete snapshot.
- Compute a coverage manifest over Creator projects + reminders, Ideas + recordings, full Content Workspace, Personal reminders + completed history, Reminder Groups, 21-day Challenges, Spaces + notes + journal + photo bytes, user-owned settings and other app-managed persisted assets. Classify app-managed bytes vs device-bound alarms, URI grants, auth tokens and external source media accurately.
- No success until each included domain is exported, authenticated, uploaded, independently re-fetched and SHA-verified, within a single snapshot identity. If **any mandatory domain cannot be included**, surface a blocker; don't claim 'everything' backed up.
- Backup must not treat user deletion as a request to restore cloud data. Restore must first display exact snapshot timestamp, coverage, counts, size, media/device limitations, and the effect on existing local data; require explicit consent and rollback preparation.
- Decide and test whether restore **replaces** or **adds** per domain. No silent cross-account imports, live-write bypass, automatic orphan deletion, or data wipe when corruption/partial restore occurs.
- Delete cloud backup affects only owned cloud backup history, not local app data or authentication; require fresh confirmation, server verified identity and no false success. Account deletion is a separate operation.
- Existing 'Sync Now' must have an explicitly reviewed deprecation/migration strategy; keep cloud records intact during rollout.

### P1-B · One-activity, 21-day challenge

- One activity text and immutable local date range (21 days, start through start+20) in chosen timezone; optional owner-selected alert time and permitted notification type. A saved challenge should associate one challenge-owned alert schedule, not 21 unrelated reminders.
- User confirmation alone marks a day Done. Store completion *per local challenge date*; deduplicate repeat alarms; support reboot, DST, timezone changes, edit, pause/resume, early completion, schedule end after day 21, and clear missed-day semantics.
- Day 22 must not fire the old challenge. Failed schedule, disabled exact alarm, or notification denial must show an honest warning and an actionable fix path.
- Existing 21-distinct-activity plans MUST be readable and retained. Do not silently coerce them to one habit, erase days, or recalculate already confirmed Done receipts. Offer an explicit owner-driven migration later.

### P1-C · Personal alert delivery and full-screen policy

- Test ALARM delivery with screen off, locked, unlocked, permissions denied, Doze, reboot, process death, and repeated occurrences. Preserve proper voice playback behavior and notification fallback.
- On Android 14+, full-screen intent is a gated user permission and is intended for genuine alarms/calls. An unlocked phone may show a persistent heads-up rather than launch an activity. Never promise unconditional takeover or force background launches outside OS policy.
- Any change to allow VOICE alarm-style presentation needs explicit owner selection of an actual alarm-mode alert and Play/policy review. Quiet notifications remain noninterruptive.
- User must be able to open a safe demo preview independently of whether the system will permit a live full-screen intent.

### P1-D · Complete account deletion

- Account deletion is different from deleting a backup or signing out. Require verified ownership and recent reauthentication; service-role authority lives only on trusted server.
- Stage and exercise durable deletion request queue, exclusive storage+database write fence, per-user Supabase Storage removal, Creator data, Personal cloud head, all newly added universal snapshots, profile, auth user LAST, and immutable post-auth receipt.
- Prove zero surviving user-associated data in two synthetic accounts and after process death, retry, denied auth, and token race. Keep local state if remote deletion failed and report 'pending/needs attention', never 'complete'.
- Provide a functioning external web account-deletion path and accurate privacy/retention language for Play submission. Don't deploy deletion to the owner's real backend without explicit go-live approval.

### P1-E · Reminder group truth

- Continue to model reminder groups as optional organization; linking should not auto-reschedule or delete underlying reminders.
- Adding a group chooser during reminder creation is a separate visually approved UI action. Bulk pause/progress features may not be promised until implemented and QA'd.

## Proposed design screenshot review

- **Direction A — Cinema / Warm Minimal:** existing Backlot charcoal surfaces, warm amber-coral accent, compact high-contrast information hierarchy.
- **Direction B — Pulse / Vibrant Minimal:** deeper plum-black, restrained peach-magenta-lilac gradient, larger breathing space.
- Both screenshot sets contain provisional Cloud Backup, 21-day setup, 21-day dashboard, Alarm, and Account Deletion screens.
- These directions are **alternatives, not locked or approved**. Don't change production code based on either until user confirms.

## Acceptance gates

1. User chooses/adjusts screenshots for EVERY Phase 1 screen before visual code is finalized.
2. Nonvisual feature code and synthetic tests prove backup coverage, restore safety, schedule correctness, full-screen fallback and deletion idempotence.
3. Emulated Android screenshots capture the REAL Compose implementation and are visually compared against user-approved source images.
4. In-place signed upgrade over Build 320 retains projects, personal reminders, Spaces, groups, challenges, stored media and account state.
5. End-to-end cloud restore on two disposable accounts/devices; no live owner data touched in QA.
6. The only releasable APK is a tested, exactly identified candidate with no fake success wording.

## External constraints

- Android 14+ full-screen intent behavior: https://source.android.com/docs/core/permissions/fsi-limits
- Google Play account deletion requirement: https://support.google.com/googleplay/android-developer/answer/13327111
- Supabase Auth deletion caveats: https://supabase.com/docs/guides/auth/managing-user-data

**Current gate: WAITING FOR USER SCREENSHOT APPROVAL. Production UI remains unchanged.**
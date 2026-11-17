# Backlot — Exact Lab V337 + P0 + cumulative UX + UX09 integration sequence

**2026-11-17 | Integration preparation DOCUMENTATION ONLY. Not approval to merge, rebase, cut over or build a production APK.**
**Founder preference:** No old Supabase cloud-data migration; preserve local data by default, avoid automatic deletion.

## GitHub reality, independently verified

- Owner accepted **original V337 P0:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e` / branch `stable/backlot-v337-p0-owner-accepted-20261116`.
- Owner accepted **new Supabase Lab V337:** `4ba1263124d118d3917f988228aec2248068466f` / branch `stable/backlot-lab-v337-owner-tested-20261117`. Android package `com.framebynavin.app.lab`, isolated sign-in test accepted. Draft PR #411 remains unmerged.
- Cumulative **UX10F:** `6654c730971309b5047473e2f69292fe799b3f46` / draft PR #428 based on full UX01–08 + UX10A–F, none certified as a production APK.
- From V337 P0 to Lab V337: **25 commits ahead, 0 behind; 18 changed paths**, including `CloudConfig.kt`, Lab package/config, Android startup/AppCheck, API26 `LocalDate.ofInstant` replacements, new Lab CI and bootstrapping docs.
- From V337 P0 to UX10F: **427 ahead, 19 behind; 218 changed paths**. UX10F has the UX visual evolution but is **missing 19 accepted corrective P0 commits**. These are already in the Lab V337 ancestry, so start any integration candidate from Lab's accepted SHA rather than stripping or manually reintroducing P0.
- **Exactly three shared changed-path names** between Lab and UX relative to V337 P0:
  1. `app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt`
  2. `app/src/main/java/com/framebynavin/app/personal/Phase1HabitPolicy.kt`
  3. `app/src/main/java/com/framebynavin/app/ui/Phase1HabitSharedUi.kt`
  Shared paths are NOT the full behavior/conflict graph: original Android Manifest, Kotlin minSdk lint, Firebase initialization, synced phone scheduling, store revisions and test fixtures also interact.
- The immutable owner-tested Lab branch retains the actual accepted 19 P0 corrections, **but this alone is not proof that an unbuilt UX merge compiles or preserves all of them**.

## Recommended integration strategy

**Reference branch:** owner-tested Lab V337, untouched.
**Integration target:** NEW isolated branch/candidate created only after review, NEVER owner-stable/main. Freeze exact SHA and keep rollback references.
**Work sequence:**

1. Confirm all cumulative draft PRs and exact pinned UX10F ancestry; extract individual UX feature packages with no lost file or surprise side-effect. Audit 218 diff paths, not just the UX10F nine-file surface.
2. Review Lab's three shared changed paths at line/behavior level. Preserve all accepted P0 behavior, API26 source replacements, original Audio/Android 15 Snooze exact milliseconds, missing-channel correctness, fail-closed recovery and no stale backup UI. Do not blindly cherry-pick on top of a changed old branch.
3. Stage a conservative source integration candidate from Lab V337 and reconcile all cumulative UX01–08/10 handlers, navigation and design tokens. Take new `CloudConfig` Lab mode, package/label, Google OAuth identity and App Check startup behavior from verified Lab source. Old-backend default still remains reference only.
4. Keep existing snapshot Sync Now limited and its Challenge coverage guard honest. Define UX09 server domain migration as its own PR, with RLS/schema/cost/rollback review. Do NOT enable per-record engine before server APIs/role/quality gates are available.
5. Stage UX09 Android outbox/account fence and domain adapters with carefully bounded file scopes, no partial cross-store destructive restore. Domain coverage must be labelled in UI until full tested parity.
6. At the agreed final consolidated test gate (after the last source stage), execute full Kotlin/JVM/lint/Android instrumentation, real RLS two-account negative, OS Snooze and Personal/Creator/Google flows, restore, conflict and offline tests on exact candidate SHA. A safe DB migration must first be validated against disposable PostgreSQL + synthetic identities before applying to hosted Lab—no skipping this security gate for schedule reasons.
7. Produce an installable QA-only `com.framebynavin.app.lab` APK **only when actual build/evidence exists**, with screenshot/smoke/integrity and signing SHA; founder then performs real-device acceptance.
8. Any later original package `com.framebynavin.app` cutover to new backend is a **separate production release approval** after sign-in reauthentication, rollback/account deletion, privacy, store permissions and launcher behavior accepted. Never silently replace the old app's Supabase URL in the historical stable baseline.

## Blocking design constraints

- An upgrade from the original app must never assume old Supabase JWTs belong to Lab. Existing local Creator/Personal work may be left on-device; adoption must be user-reviewed or Lab install fresh. No copying old cloud data.
- Android 15 OS alarms must be recreated on each device from the intended reminder configuration after permission check. Synced completed events are not notification PendingIntents.
- Protected P0 fixes, original restore-gate, voice/music delivery and account-switch fencing must survive, even when UI labels become prettier.
- Google login's successful token exchange does not prove completed backup, Personal 21-day coverage, private photo transfer or two-device sync.
- Widget providers and the prior request to redesign them remain separately gated; never wipe existing user launcher widgets during a cloud source migration.
- Public account deletion is not equivalent to `delete_creator_sync_data`; a full data/Storage/Auth deletion path and stale-session handling require separate later security approval.
- Do not silently introduce Supabase Pro, Realtime-heavy operating costs, Edge Functions or new Firebase app configuration. Defer costs to explicit founder review.

## Proposed next decision artifact

BL-02A static foundation is verified; BL-02B authenticated dynamic checks remain open. UX09 architecture is PROPOSED. The next **implementation** request should approve one bounded package such as:

**"Approve UX09-01 documentation-and-disposable-schema implementation only, based on owner-tested Backlot Lab V337; no hosted migration, Android production code, old data deletion, merge or release until real two-user RLS/atomicity evidence is reviewed."**

That authorization is NOT assumed from the current BL-02/BL-03 documentation approval. When the founder approves the plan and next stage, implement the narrow boundary, provide exact source SHA/PR/test evidence and update this handoff.

**Current status:** docs-only source-staged readiness; zero UX09 product code/schema deployed; zero two-device acceptance runs; both accepted baseline branches untouched.

# Backlot UX09-01 — Disposable Postgres contract package for Creator Projects + Ideas

**Status (2026-11-17):** SOURCE CODE / TEST HARNESS STAGED on a draft GitHub branch. **No hosted Lab SQL, no local disposable PostgreSQL execution, no Android app changes, no merged migration, no CI PASS, no two-device sync, no APK.**
**Parent:** BL-02/BL-03 documentation-only draft PR #429 at `9445aa62fcf8fd58fb300bc4dd7e15b10ef2537b`, stacked on owner-tested Backlot Lab V337 `4ba1263124d118d3917f988228aec2248068466f`.
**Immutable protected baselines:** original accepted V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, owner-tested Lab V337 `4ba1263124d118d3917f988228aec2248068466f`, staged UX10F `6654c730971309b5047473e2f69292fe799b3f46`.
**Founder direction:** Start fresh, no historical cloud migration. **Do not interpret that as permission to delete existing app data, current Lab records, accounts, private media or production Supabase.**

## Why this scope is deliberately small

UX09 is **Sync + Backup + Restore + Delete**, but a dependable core requires strict per-record state, account-scoped revisions and tombstones BEFORE expanding to personal life data or enabling cloud recovery. The current Lab already has Creator/Personal snapshot tables, but has no proven full Personal data coverage. The existing single-activity Challenge guard intentionally rejects partial backups and is preserved.

**UX09-01 supports only Creator Projects and Creator Ideas as an isolated contract.** It does not replace existing Creator cloud snapshots, alter saved Room IDs, auto-upload any data, or add a cloud deletion UI. There are NO new customer-facing sync features in this PR.

## Stage file register

1. `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql` — source-only **prototype, not a migration**:
   - Internal non-Data-API schema `backlot_sync_internal` with owner-scoped `account_sequences`, `entities`, `changes`, and `applied_operations`. Each table enables RLS and gets explicit authenticated-only owner policies. Anonymous table privileges and internal schema usage denied.
   - `public.backlot_ux09_apply_v1` RPC, security-invoker, `auth.uid()` account owner. Writes entity, immutable change event, immutable idempotency acknowledgement and per-account ordered cursor in the SAME PostgreSQL transaction.
   - `public.backlot_ux09_pull_v1` RPC, security-invoker, bounded 1..100 change events with stable `next_cursor`, `has_more` and exact payload revision as of commit.
   - Optimistic concurrency: create expects revision 0; update/delete expects current revision. Returns CONFLICT rather than overwriting a changed record.
   - Repeated identical operation UUID returns the original receipt, changed request using same UUID is rejected. No background replay can fake two commits.
   - Deletes use durable **tombstones**, not immediate hard deletes. An old offline device cannot silently resurrect an old record. The database FK cascade permits future authenticated-admin full-account erasure, but **no account-delete RPC/privilege exists today**.
   - Strict initial domain allowlist `creator_project`, `creator_idea`, max 16 KiB JSON object, reserved account/session/alarm token fields rejected, no anonymous execution, no generic Personal or private-media storage.
2. `supabase/prototypes/ux09_01_security_assertions_v1.sql` — read-only disposable-database assertions for RLS, role grants, two authenticated-only RPCs, no direct hard-delete grant and no security-definer RPCs.
3. `tools/qa/backlot_ux09_01_disposable_auth_matrix.sh` — localhost:54321-only two-JWT black-box API test: A/B identity and same entity UUID isolation, version conflicts, retry idempotency, changed-payload reuse rejection, deltas/pagination, tombstone/no resurrection, both Creator domains, invalid-domain rejection. Redacts JWT/body from failures.
4. `docs/engineering/UX09_01_DISPOSABLE_SYNC_SERVER_CONTRACT_IMPLEMENTATION_20261117.md` — this implementation and safety handoff.
5. `docs/qa/UX09_01_SERVER_CONTRACT_CERTIFICATION_20261117.md` — proposed automated/security acceptance list, **UNEXECUTED**.

## SQL application procedure: NOT PERFORMED

The environment accessible to this task has **no Supabase CLI, Docker or PostgreSQL executable**, so there is no safe disposable database on which to compile or dynamically test this source. The user-facing connected Supabase plugin points to the actual hosted Lab project; its read-only audits are NOT a sandbox. **Do not run this prototype against hosted Lab in order to claim testing.**

After a developer is on a disposable LOCAL Supabase CLI stack (with a working Docker backend):
1. Verify `supabase --version` and read `supabase --help` / `supabase migration --help`; check [official CLI docs](https://supabase.com/docs/reference/cli/introduction).
2. Start a **disposable local** instance with two distinct disposable authenticated users. Do not use real Google identities, production tokens, private photos or actual account data.
3. Apply `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql` to that local database with the CLI/psql `ON_ERROR_STOP=1` option. The source is one explicit PostgreSQL transaction; any error means NO PASS.
4. Run `supabase/prototypes/ux09_01_security_assertions_v1.sql` and local-only `tools/qa/backlot_ux09_01_disposable_auth_matrix.sh`. Capture redacted logs and SQL fixture/test exit codes. **No tests have run as of this document**.
5. Review snapshot/legacy data coexistence, Data API's **exposed schemas** setting (internal schema must NOT be exposed), role grants, RLS and App JWT behavior. Two-user negative tests MUST use ordinary authenticated REST RPC calls, not privileged admin SQL.
6. Only after successful local contracts, architectural security review and founder authorization, generate a real migration file using `supabase migration new <approved_name>` and commit it with the tested SQL. No timestamp migration filename was guessed here because no local Supabase CLI exists.
7. Hosted `vasleafrmsovdbdlrcsw` migration, paid branch creation, production/server key changes, Android sync client integration, cleanup or release require separate scoped founder approval. If hosted migration is ever approved, perform explicit before/after schema/role/policy/advisor verification and rollback review.

## Security boundaries and open risk

- **Exposed schema trap:** A SECURITY INVOKER function must have data privileges, so `authenticated` has RLS-limited SQL rights on private tables. The `backlot_sync_internal` schema MUST remain absent from Supabase Data API exposed-schema settings; otherwise a client with a valid JWT could sidestep revision logic via direct REST table writes. Static RLS protects tenant isolation but not that alternate concurrency path. Hosted rollout MUST verify this setting explicitly; if uncertain, redesign with a tightly audited private privileged function in a non-exposed schema and RPC facade rather than bypassing authorization silently.
- **Generic JSON validation:** Only two Creator domains are allowed for this prototype, with bounded JSON objects. Real Project/Idea domain codecs and cross-record ownership/reference validation are NOT yet written. No Android or import may consume arbitrary remote JSON as trusted app data.
- **Account deletion:** This prototype supports tombstones for *records* and chain-safe auth-user cascading **when a separately authorized admin account-removal service eventually exists**. It does not implement full deletion, identity verification, Storage cleanup, stale access-token revocation, request evidence, legal retention or Android account closure UX. All are future explicit gates.
- **Backup vs sync:** An append-only account change log plus current entities is NOT yet a point-in-time encrypted whole-account backup, disaster recovery archive or verified fresh-device restore. No "Backup complete" badge may use these RPCs until exact coverage and recoverability are proven.
- **Privacy:** Never automatically upload Personal Spaces, Photo Journal, challenge receipts, custom songs or media. Device-local reminder alarm permissions and playback tokens are not portable.
- **Retention:** Tombstones and change events are durable without an approved pruning policy. This is correct for safety but could grow with use; before hosted rollout define offline-device expiration, account delete, pagination, retention and Free-tier size alarms. No paid Realtime service introduced.
- **Supabase Auth:** `auth.uid()` and nonanonymous checks only, no user-editable `user_metadata` authorization, no secret in Android, no anonymous grants.
- **No branch merge or app release:** P0 original and Lab V337 remain unchanged. Cumulative UX10F is a separate, uncompiled branch and must be reconciled in a later isolated integration candidate (known shared changed paths: `Phase1HabitLiveDelivery.kt`, `Phase1HabitPolicy.kt`, `Phase1HabitSharedUi.kt`).

## Sequence after this stage

UX09-01 exact schema security review and real disposable test evidence (OPEN) -> separate hosted Lab migration approval -> UX09-02 durable Android outbox and Creator adapter -> subsequent Personal + groups + Challenge + Spaces with genuine source/receipt parity -> **explicit full backup and restore** architecture with encrypted/verified recovery -> **explicit cloud-data deletion and Auth account closure** -> final two-device/Android real-life certification on integrated UX source -> founder APK promotion and publication decision.

**Stage verdict:** Project/Idea server SQL and negative test harness **authored in GitHub**; SQL compile, live migration, 2-JWT runtime QA, Android adaptation, Backup/Restore/Delete implementation **NOT COMPLETE**.

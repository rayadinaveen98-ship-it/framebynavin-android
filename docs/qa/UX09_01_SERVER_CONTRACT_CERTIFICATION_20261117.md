# Backlot UX09-01 — Database foundation certification / no production mutations

**2026-11-17 | 40 planned tests, 0 executed.** Source prototype and black-box harness staged. No local Supabase CLI/Postgres/Docker in current tool runtime, and no hosted SQL change permitted during isolated-source staging. Static source inspection is not a SQL compiler or authorization proof.

**Exact parent:** UX09 design docs PR #429 head `9445aa62fcf8fd58fb300bc4dd7e15b10ef2537b`; prototype stacked on owner-tested Lab V337 `4ba1263124d118d3917f988228aec2248068466f`.
**Protected:** original V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; cumulative UX10F `6654c730971309b5047473e2f69292fe799b3f46`.
**Current live Backlot Lab schema:** Four old app tables and two migrations; the new `backlot_sync_internal` schema has NOT been created in hosted Lab. No old test-data migration, deletion, backup write or Auth call was made.

| ID | Domain | Test/requirement | Status |
|---|---|---|---|
| S01 | SQL | Prototype applies in one disposable transaction; no partial DDL on failure | NOT RUN |
| S02 | SQL | Four internal tables exist with correct FK and indexes; no current Lab table modified | NOT RUN |
| S03 | SQL | All four internal tables have RLS ON and 10 authenticated owner-bound policies | NOT RUN |
| S04 | SEC | Anonymous and PUBLIC cannot use internal schema or table read/write | NOT RUN |
| S05 | SEC | Apply/Pull anonymous RPC EXECUTE denied; authenticated EXECUTE granted | NOT RUN |
| S06 | SEC | Both exposed public RPCs SECURITY INVOKER with fixed search_path and auth.uid | NOT RUN |
| S07 | SEC | An authenticated account cannot access another account's entity via pull | NOT RUN |
| S08 | SEC | Same entity UUID under two different users remains fully isolated | NOT RUN |
| S09 | SEC | Cross-user INSERT/UPDATE/DELETE cannot set a foreign user_id | NOT RUN |
| S10 | SEC | A/B testing uses genuine distinct Supabase Auth JWTs, not service-role SQL | NOT RUN |
| S11 | SEC | The internal schema is absent from Supabase exposed API schemas | NOT RUN |
| S12 | SEC | No Android client/server code contains a service-role or other private secret | NOT RUN |
| S13 | CAS | New Creator Project requires expected_revision 0 and receives revision 1 | NOT RUN |
| S14 | CAS | New Creator Idea applies with the same typed domain contract | NOT RUN |
| S15 | CAS | Existing Project edited with exact revision advances by one | NOT RUN |
| S16 | CAS | Stale edit expected revision returns CONFLICT, original value unchanged | NOT RUN |
| S17 | CAS | Two concurrent account writes allocate monotonic commit-ordered sequences | NOT RUN |
| S18 | CAS | Same op-id and identical payload returns exact previous acknowledged result | NOT RUN |
| S19 | CAS | Same op-id and different payload is rejected, without changing record | NOT RUN |
| S20 | CAS | New offline client replay of same op-id after timeout remains idempotent | NOT RUN |
| S21 | CAS | Unsupported domain, scalar, oversized JSON, owner/device secret fields rejected | NOT RUN |
| S22 | DELETE | Delete requires observed version and saves a tombstone at new revision | NOT RUN |
| S23 | DELETE | Old offline device cannot recreate a tombstoned entity via create-0 | NOT RUN |
| S24 | DELETE | Two different account records sharing UUID are not cross-deleted | NOT RUN |
| S25 | DELETE | No authenticated direct DELETE grant circumvents tombstones | NOT RUN |
| S26 | DELETE | Internal FKs support eventual admin Auth-user removal without orphaned rows | NOT RUN |
| S27 | PULL | Pull from zero returns ordered finite change events and original revision payload | NOT RUN |
| S28 | PULL | Limit 1 pages correctly and has_more reflects unread change records | NOT RUN |
| S29 | PULL | Empty pull returns [] and cursor unchanged, not fake success events | NOT RUN |
| S30 | PULL | Malformed cursor, limit 0 and limit >100 rejected with no write | NOT RUN |
| S31 | RUNTIME | Disconnection during RPC results in safe resend not duplicate mutation | NOT RUN |
| S32 | RUNTIME | Two-device edit conflict must present explicit review, not last-write-wins | NOT RUN |
| S33 | RUNTIME | No initial sync updates old Creator snapshot or Personal head tables | NOT RUN |
| S34 | BACKUP | Project/Idea sync success never claims full device restore or archive backup | NOT RUN |
| S35 | RESTORE | New phone rebuild requires a separate verified local adapter and recovery test | NOT RUN |
| S36 | DELETE | No full cloud-data or Auth account delete endpoint deployed from this stage | NOT RUN |
| S37 | SAFETY | Old V337, owner-accepted Lab V337 and UX10F source heads unchanged | NOT RUN |
| S38 | SAFETY | Hosted Lab has no new internal schema/migration; no paid resources created | NOT RUN |
| S39 | QA | Disposable SQL test, A/B RPC smoke and RLS reviewer evidence linked to exact SHA | NOT RUN |
| S40 | GATE | Founder reviews compiled/disposable evidence before hosted DDL or Android sync | NOT RUN |

## Executable artifacts prepared, but UNEXECUTED

- `supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql`: private-schema tables plus optimistic CAS/tombstone and authenticated delta RPC SQL. **Not an approved migration.**
- `supabase/prototypes/ux09_01_security_assertions_v1.sql`: pure SQL fail-if-insecure introspection, no data mutation.
- `tools/qa/backlot_ux09_01_disposable_auth_matrix.sh`: LOCALHOST:54321-only synthetic two-JWT QA. Requires two genuinely separate locally provisioned accounts, curl, jq and Python3. It refuses any hosted URL and never logs tokens.
- Normal source tests cannot prove PostgreSQL DDL correctness or concurrent transaction safety. A disposable stack with concurrent clients MUST run before changing a hosted schema. Full CI, Android physical and 2-device end-to-end cases remain later stages.
- Versioned migration creation must use an actual local `supabase migration new` command after test and security review; no invented migration filename in this PR.

## Mandatory negative tests

- B must never pull/update/delete A's entity; having a matching `entity_id` UUID does not confer access.
- A stale writer must not overwrite; retrying identical operation UUID is one persisted mutation only, mutated retry is rejected.
- Deletions remain durable tombstones until a separately approved retention/account delete process. Unconnected older devices cannot resurrect deleted Projects or Ideas.
- Lack of Android account session means no sync; no fake green/backup badge based on local success alone.
- Restore of reminders with existing 21-day Challenge still refuses incomplete legacy Personal JSON; no code change to bypass guard.
- The old Backup/Restore Settings features remain removed per founder's prior UI decisions. UX09 does NOT silently re-add them or expose a delete-account control that does nothing.
- The full Account Delete request, server cleanup of active and historical snapshots, private Storage objects and Auth identity, stale JWT revocation/expiry, abuse/regulatory retention and confirmation UI are NOT part of this prototype. **Do not call record tombstones full account deletion.**

**Blocker:** no disposable PostgreSQL executor or dual-JWT test credentials in current environment; therefore ZERO runtime/SQL compile PASS. Real hosted Lab is NOT an acceptable substitute for the disposable test. No source merge/hosted change/QA APK/release until later approved gates.

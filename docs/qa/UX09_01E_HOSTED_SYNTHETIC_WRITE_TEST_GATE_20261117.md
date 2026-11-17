# Backlot UX09-01E — hosted synthetic Project/Idea write acceptance

**Date: 2026-11-17 | STATUS: HOSTED LAB WRITE CONTRACT E01–E16 ALL PASSED AND CLEANUP VERIFIED.**
**Exclusive target:** Backlot Lab Supabase project `vasleafrmsovdbdlrcsw` (PG 17.12).
**Change authorization:** founder approved continuation from the UX09-01D PASS into real synthetic Project/Idea testing, with original V337 and production database preserved. This does not authorize any stable merges, new paid resources, production releases, original Google-account mutation, account-delete product implementation, or unrelated schema change.

## 2026-11-17 live owner-run E01-E15 evidence — PASSED; cleanup E16 OPEN

**Owner-uploaded sanitized artifact:** `Backlot-UX09E-Write-Report.txt`; SHA-256 `96eafc572082debc0333bfc7a064e2a7e1501b0b6eee94fd322661fd07a195ec`. The uploaded report states that all write, concurrency, input-negative, cursor, account-isolation, tombstone and signed sign-out checks passed. This is **owner-executed Windows V1**, not a hosted Github Actions write test. No passwords, user identifiers or raw payloads are included in that report.

**Independent connected Backlot Lab read-only SQL after report:**
- Exactly 3 Auth identities: 1 original non-QA + exactly 1 QA A + 1 QA B.
- QA Auth sessions: A **0**, B **0** (original's session is not touched).
- QA A: `account_sequences=1`, `entities=2` (1 `creator_project` tombstone at revision 3; 1 `creator_idea` at revision 1), `changes=4` (project seq 1,2,3; idea seq 4), `applied_operations=4`.
- QA B: `account_sequences=1`, `entities=1` (creator project at revision 2, not tombstoned), `changes=2` (seq 1,2), `applied_operations=2`.
- All four `backlot_sync_internal` tables contain **zero non-QA-owned records**. The installed UX09 migration count remains **one**.
- Clean-up preflight: QA-owned `creator_profiles`, `creator_sync_snapshots`, `creator_sync_heads`, `personal_sync_heads`, and `storage.objects` all have **0 rows**; QA Auth session rows 0.
- Existing FK constraints of all four new internal tables reference `auth.users` with `ON DELETE CASCADE`; app user tables also reference Auth users using cascades.
- **Important evidence boundary:** server data independently corroborates final-state revision, sequencing, tombstone and identity isolation; the race result, idempotent replay response, and rejected-input HTTP behavior are evidenced by the runner's owner-uploaded report, not by a separate new independent CI replay.
- No SQL writes, schema changes, account deletions, Auth provider setting changes, branch merges or stable app updates performed during this evidence review.

**Certification decision:** `UX09-01E E01–E15: PASS` for the scoped **hosted Lab Project/Idea RPC contract**. `E16: PENDING` until the founder uses Dashboard to delete **only** the two disposable QA identities with the `backlot-ux09-a-b17c2026@gmail.com` and `backlot-ux09-b-b17c2026@gmail.com` suffixes, then the connected database confirms zero QA accounts and zero residual sync records. User Google account must never be deleted. Do not re-run E01-E15 on these accounts. Deleting QA identities before the evidence was recorded would have destroyed the positive test audit.

**Still not implemented or verified:** Android offline outbox and multi-device sync, Personal/Spaces/Challenges, complete Backup/Restore/Account deletion, cumulative UX01–08/UX10 integration, physical APK acceptance and production release. Preserve V337 and original production backend.

## 2026-11-17 E16 cleanup independently verified — UX09-01E COMPLETE

**Owner action:** deleted **only** the two synthetic Backlot Lab QA accounts using the Supabase Dashboard.

**Independent connected read-only SQL after deletion, against Backlot Lab project `vasleafrmsovdbdlrcsw`:**
- `auth.users` **1 total**; QA A/B matching `backlot-ux09-[ab]-b17c2026@gmail.com` **0**; Google identity still present **1**.
- QA Auth sessions **0**; total Auth sessions **1** (belongs to the surviving original Google user).
- `backlot_sync_internal.account_sequences` **0**, `entities` **0**, `changes` **0**, `applied_operations` **0**. Tombstone records and operation history from UX09-01E were removed via the verified FK cascade after the temporary account deletion. No orphaned sync data remains.
- Existing `public.creator_profiles=1`, `creator_sync_snapshots=1`, `creator_sync_heads=1`, `personal_sync_heads=0`, Storage objects **0**. Original app data remains in place.
- Three hosted migrations remain installed: `20261117070805` Lab fresh install, `20261117070925` private voice policies, `20261117141908` UX09-01 Project/Idea contract.
- Supabase security advisor reports only the previously known `auth_leaked_password_protection` warning; no new advisory detected.
- **No writes or deletions were performed by this evidence check.** Original Backlot production backend and stable V337 source were not changed.

**Final decision:** all UX09-01E scoped E01–E16 tests and cleanup **PASSED / CLOSED**. The server-side Creator Project/Idea synchronization RPC contract is certified on Backlot Lab for the tested scenarios. This **does not** certify Android synchronization, full Personal/Spaces/Challenges coverage, automatic backups, recovery/restore, account deletion product UX or APK release.

**Next:** UX09-02 Android offline-first design/source audit and isolated implementation proposals, preserving protected V337 and UX10 P0 corrections. PR stays draft/unmerged; do not silently merge, mutate original production data or release.

## Prerequisites verified read-only on hosted Lab
- Owner-executed signed read-only test succeeded for two independent disposable QA users, both HTTP 406 on private sync schema, read-only feeds empty, both signouts accepted (evidence in UX09-01D PR #434).
- Fresh connected read-only SQL before this stage: exactly **3** Auth users (owner + QA A + QA B), both QA identities confirmed with password login, **0** QA sessions, and **0** records in each of four `backlot_sync_internal` tables.
- Live Foreign Key inspection confirmed all four new internal tables link to `auth.users` with `ON DELETE CASCADE`. Existing `public.creator_profiles`, creator/personal sync heads/snapshots also cascade on Auth user deletion. No owner data should ever be selected or deleted during QA.
- `backlot_sync_internal` is not in Exposed Schemas. `backlot_ux09_apply_v1` and `backlot_ux09_pull_v1` use SECURITY INVOKER, deny anon EXECUTE, grant authenticated EXECUTE.

## Runner prepared (local owner handoff; not a repository CI deployment)
- Local self-contained Windows PowerShell 5.1 package: `Backlot-UX09E-Windows-Write-Test.zip`, SHA256 `f7df2337e5bf63032bf6e0e21dfb2f124a503370b1c157fa9e21e2e62d406b1a`.
- Package contains `Backlot-UX09E-Write-QA.ps1`, `Run-Backlot-UX09E-Write-QA.cmd`, `README-UX09E.txt`.
- ZIP contents, exact embedded script, strict Lab host pin, seven allowlisted Auth/GET/POST RPC routes, refused signup/admin DELETE/direct table-write routes, synthetic payloads, logout in finally, and sanitized reporting checked locally with Python static assertions. **Windows PowerShell execution and live hosted tests have NOT been verified.**
- No tokens/emails/passwords/UUIDs in the report; the runner takes secrets only from local private password prompts and never contacts the old production backend.
- Only accepts the two existing disposable `backlot-ux09-[a|b]-b17c2026@gmail.com` suffix identities (same-batch plus alias or strict no-plus formats). No Google owner identity.

## Exact test cases once the owner runs it
| ID | Verification | Expected |
|---|---|---|
| E01 | Both signed identities distinct and no preexisting sync events | Initial empty delta per user |
| E02 | A project create with expected revision 0 | Applied, revision 1, sequence 1 |
| E03 | Exact request repeated under same operation UUID | Exact original sequence and revision |
| E04 | Reused operation UUID with changed payload | HTTP 4xx denial |
| E05 | Stale revision for A project | Conflict, expected current revision 1 |
| E06 | B attempts revision-1 write against same A entity UUID | Conflict, B has no previous entity |
| E07 | B creates same entity UUID in its own account | Independent revision 1 |
| E08 | A/B each pull change list | No cross-account payloads |
| E09 | Two simultaneous B updates with expected revision 1 | Exactly one winner rev2 and one conflict |
| E10 | A update, tombstone deletion | Revision 2 and deletion rev3 |
| E11 | A attempts stale and current revision resurrection | Both conflict, tombstone preserved |
| E12 | A creates synthetic Creator Idea | Applied revision 1 |
| E13 | Pull sequence/cursors and historical snapshots | A 4 change events, B 2, ordered and bounded pages |
| E14 | Unlisted Personal domain, owner-ID injection, limit > 100 | HTTP 4xx, no new sync events |
| E15 | Logout both signed QA sessions | Both accepted; independent SQL verification required |
| E16 | Cleanup via Dashboard deletion of exact two QA identities ONLY | Separate review and independently verified zero residual rows |

## Security & cleanup rules
- This test intentionally creates **six committed QA change events** across both test accounts if successful, with durable tombstones. It does NOT delete synthetic rows automatically.
- If it fails midway, **partial synthetic records may remain**. Do **not** rerun, reset QA accounts, or delete them until exact database state has been reviewed. Do not mistake any failure as a successful security certification.
- After the report is uploaded and reviewed, founder may delete **only** the two exact disposable QA accounts through Backlot Lab Dashboard; all new sync FK chains use `ON DELETE CASCADE`. Verify 0 sessions, 0 QA rows across new and legacy user-owned tables and original owner account still exists. Auth JWTs might outlive deletion until expiry.
- Never delete the original user, use the service-role key in a client, remove RLS, expose internal schema, change Auth providers, or replay original migrations.
- `UX09-01E` **will not certify** Android offline-first two-device auto-sync, personal/space/challenge sync, complete Backup/Restore, cloud-data or Auth Account Delete, UX01–08/UX10 integration, APK or Play release.

## Evidence acceptance
- **Current**: E01–E16 PASS; independent post-deletion SQL verified zero QA identities, sessions, synthetic sync rows and preserved owner Google account. Prior 01D hosted read-only PASS remains valid.
- On owner-executed runner PASS, compare sanitized `Backlot-UX09E-Write-Report.txt` against independent connected Lab SQL row counts, exact QA user ownership and migration stability. Keep PR draft. An unverified report or partial pass must not advance the Android gate.

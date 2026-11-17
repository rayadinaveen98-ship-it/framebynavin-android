# Backlot UX09-01E — hosted synthetic Project/Idea write acceptance

**Date: 2026-11-17 | STATUS: LOCAL QA RUNNER PREPARED; LIVE WRITE ACCEPTANCE NOT RUN.**
**Exclusive target:** Backlot Lab Supabase project `vasleafrmsovdbdlrcsw` (PG 17.12).
**Change authorization:** founder approved continuation from the UX09-01D PASS into real synthetic Project/Idea testing, with original V337 and production database preserved. This does not authorize any stable merges, new paid resources, production releases, original Google-account mutation, account-delete product implementation, or unrelated schema change.

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
- **Current**: static package safety only; 01D hosted read-only PASS. E01–E16 are all **PENDING**.
- On owner-executed runner PASS, compare sanitized `Backlot-UX09E-Write-Report.txt` against independent connected Lab SQL row counts, exact QA user ownership and migration stability. Keep PR draft. An unverified report or partial pass must not advance the Android gate.

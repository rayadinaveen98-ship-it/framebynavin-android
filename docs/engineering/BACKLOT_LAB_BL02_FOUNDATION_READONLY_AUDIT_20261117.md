# Backlot Lab — BL-02 live backend foundation inspection (READ ONLY)

**Date:** 2026-11-17.  
**Authority:** Founder approved continuation on a **fresh Backlot Lab environment**, with **no migration of old test data**. This does NOT authorize deleting existing Lab/old data, changing live RLS, shipping a new APK, merging draft PRs or replacing production.
**Status:** Read-only cloud metadata verified; physical Google sign-in previously owner-accepted in Lab V337; full Creator/Personal/voice/sync dynamic acceptance OPEN.

## Exact source-of-truth pins

| Item | Verified identity | Treatment |
|---|---|---|
| Official frozen original V337 | `stable/backlot-v337-p0-owner-accepted-20261116` at `815ba69775f75b6e5f25bb838c2c17b4f09c203e` | Preserve; still points to old backend |
| Owner-tested Backlot Lab V337 | `stable/backlot-lab-v337-owner-tested-20261117` at `4ba1263124d118d3917f988228aec2248068466f` | Planning/source test reference. `com.framebynavin.app.lab`, new Lab Supabase; Google sign-in physically owner-accepted, **not** full backup tested |
| Visual/source enhancements | UX10F draft PR #428 head `6654c730971309b5047473e2f69292fe799b3f46` | Cumulative UX source-stage, not integrated into Lab V337 |
| Old Supabase | `kukkqgpzxnfanynbddiw`, original Creator OS | Old production/test resource; no cutover or deletion |
| New Supabase | `vasleafrmsovdbdlrcsw`, project **Backlot Lab**, region ap-south-1 | Connected in second Supabase account; live API returns ACTIVE_HEALTHY |

**In this audit:** No write SQL, no credentials accessed, no cloud data export/erase, no user impersonation, no Auth token minting, no new schema, no Android source edits and no GitHub branch merge.

## Live Supabase facts (metadata/aggregate read only)

| Verification | Observed result | Evidence class |
|---|---|---|
| Project | Backlot Lab ACTIVE_HEALTHY | Live Supabase project metadata |
| Installed SQL migrations | `backlot_lab_v337_fresh_install_20261117`, `backlot_lab_v337_private_voice_storage_policies` | Live migration history |
| Public app tables | `creator_profiles`, `creator_sync_snapshots`, `creator_sync_heads`, `personal_sync_heads` | Four tables, 4/4 with RLS ENABLED |
| App public table policies | 4 + 3 + 4 + 3 = **14** | All scoped to `authenticated` and predicates referencing `auth.uid()` |
| Anonymous table SELECT | DENIED on all four app tables | Postgres role privilege introspection |
| Authenticated table SELECT | Granted, **subject to RLS** | Role privilege introspection, NOT evidence of true two-account isolation |
| App RPCs | `claim_creator_username`, `get_creator_sync_head`, `push_creator_sync_snapshot`, `push_personal_sync_snapshot`, `delete_creator_sync_data` | 5/5 denied anonymous EXECUTE, authenticated EXECUTE granted, SECURITY INVOKER, reference `auth.uid()` |
| RLS helper | `public.rls_auto_enable()` | SECURITY DEFINER but no anonymous/authenticated EXECUTE grant (read-only ACL confirmed) |
| Voice bucket | `creator-voice-ideas` private, 20 MiB cap, allowed `audio/mp4` | 3 owner-scoped storage policies for SELECT/INSERT/UPDATE |
| Voice objects | 0 | Aggregate `storage.objects` count; **no actual upload/download success proven** |
| Creator profile/snapshot records | One profile, one head and one revision-1 snapshot | Aggregate counts only; **not** a restored Creator project acceptance test |
| Personal sync records | **0** | No Personal cloud backup success established |
| Encrypted V2 archive | `personal_archive_v2_heads` table absent and `backlot-personal-archive-v2` bucket absent | Client's existing V2 encrypted Personal archive backend is **NOT provisioned** on Lab |
| Supabase health advisor | No findings | Live read-only advisor response |
| Supabase security advisor | 1 warning: leaked-password protection disabled | No critical table/RLS finding, but review before public Auth launch |
| Former Lab Android acceptance | PR #411 documents successful compile/JVM/lint/API30+35 startup and owner-installed Google login at `4ba126...` | Prior CI/owner acceptance record; **not newly executed** |

**Important:** `pg_policies` and role privilege metadata make an excellent static gate, but they do NOT substitute for two separately authenticated JWT users attempting create/read/update/delete across account boundaries through the real Data API. Only those tests can verify runtime RLS and OAuth session behavior.

The live `delete_creator_sync_data` RPC is only scoped Creator snapshot deletion; **never** call it full account deletion. Full Auth identity / related data / Storage deletion requires a separately approved public-release design.

### Current Android/client facts, verified at owner-tested Lab SHA

- `app/build.gradle.kts`: `-PbacklotLab=true` selects independently installable `com.framebynavin.app.lab` package. Default Backlot app/package and old backend remain unchanged.
- `CloudConfig.kt`: Lab variant uses new project URL and publishable client key (no service-role or secret embedded). Original Google Web Client ID remains.
- `CloudApiClient.kt`: exchanges native Google ID token for a *new project's* Supabase session; refreshes credentials; authenticated Creator RPC plus Personal snapshot operations use that scoped session.
- `CreatorCloudSyncManager.kt`: versioned Creator snapshot with CAS/conflict handling, separately attempts Personal sync; safe fallback can require manual conflict choice.
- `PersonalCloudSyncManager.kt`: intentionally throws `PersonalCloudIncompleteChallengeCoverage` whenever a single-activity 21-day Challenge exists: the current Personal JSON backup excludes true Challenge receipts; restore/upload refuses to delete them.
- `V290CloudCoverageDisclosure.kt`: current Sync Now does **not** cover Spaces, private photos, Challenge receipts, Reminder Groups, Best Frame photo bytes or nonportable songs. Successful Creator backup is NOT proof of complete Backlot recovery.
- `PersonalArchiveCloudV2Api.kt`: references non-provisioned V2 archive table/storage. Do not enable/archive-advertise before schema, encrypted media and supported restore are reviewed.
- `CloudSyncActivity.kt`: Google login can trigger a Creator then Personal sync attempt; a successful Google sign-in does not imply Personal sync passed.
- Lab PR #411 fixed the initial debug App Check crash without changing original Backlot; a past failed Lab APK must not be confused with owner-accepted current Lab source.

## Evidence status matrix

| Work item | BL-02 status | Blocker / next safe action |
|---|---|---|
| Backend reachability | VERIFIED | No blocker |
| Fresh database tables/migrations | VERIFIED | Versioned schema/locking audit needed before new per-record design |
| Static RLS, role grants and RPC contracts | VERIFIED STATIC | Real dual-JWT A/B negative tests pending |
| Voice storage privacy configuration | VERIFIED STATIC | Upload/download/delete behavior not tested: 0 objects |
| Lab Android package + Google sign-in | OWNER-ACCEPTED PREVIOUSLY | Keep exact Lab pin; do not infer session expiry or second-device login |
| Creator snapshot creation | SERVER RECORD OBSERVED | Authenticated end-to-end save/restore/conflict with disposable data not certified |
| Personal snapshot | NOT VERIFIED | 0 rows; existing Challenge coverage guard can intentionally refuse |
| Full Private Personal V2 archive | NOT PROVISIONED | Separate scoped encrypted archive/UX09 coverage decision; no backend DDL approved yet |
| UX09 per-record two-device sync | NOT IMPLEMENTED | Must design, approve and implement first |
| Full account erasure | NOT IMPLEMENTED | Separate public release security gate |
| Production release | BLOCKED | Preserve V337/Lab baselines, reconcile cumulative UX, run full campaign |

**No destructive migrations needed.** The founder's fresh-start decision means avoid copying **old** Supabase profiles/snapshots or old app test data. Existing Lab test records are left untouched. If the Lab QA install contains local records, switching account must never silently overwrite or expose them.

## Recommended BL-02B follow-up evidence (not executed now)

Test with two **disposable** distinct Google/Supabase identities and Lab application contexts (e.g. emulator A + emulator B) using real project publishable key. No admin/service-role token is embedded in Android. Capture only redacted request status, exact report/test SHA and public test fixture IDs; do not log access/refresh tokens, email or private reminder content.

1. Same-account login and session refresh, restart, logout/login with original `com.framebynavin.app.lab`.
2. Creator project/idea snapshot save; fetch/checksum/restart/explicit restore, version-conflict flow.
3. Different account must fail SELECT/INSERT/UPDATE/DELETE on another account's record; authenticated app RPC must derive user from token; verify no cross-account Google / Personal content.
4. Voice idea upload/download and reject cross-user object URLs; MIME/cap and signed URLs; no publicly accessible bucket.
5. Personal Reminders + completion receipts backup only when supported, intentionally expect Challenge-coverage refusal if a Challenge exists, verify no mutation.
6. Network loss, retry/backoff, duplicate request, app killed while uploading, stale session and app switching; no duplicate Creator tasks, data loss or phantom success.
7. Explicitly confirm `personal_archive_v2` reports unavailable rather than pretending full backup works.
8. Source/OS data cleanup: no personal-secret logs, no alert token propagation between devices, Android permissions rechecked on each phone.

**BL-02 verdict:** lab foundation static security PASS with known independent runtime/coverage blockers. Ready for design work and isolated *future* test fixtures; not ready to assert UX09 or complete cloud recovery.

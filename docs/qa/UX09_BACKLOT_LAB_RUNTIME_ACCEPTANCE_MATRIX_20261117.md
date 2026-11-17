# Backlot Lab UX09 — Foundation and future two-user runtime acceptance

**2026-11-17 — Documentary checklist, not executed live test results.**

- 43 acceptance cases: **7 static verified checks** and **1 prior owner-accepted sign-in record**, remaining **35 are PENDING**. Static checks are NOT full behavioral passes.
- Read-only Supabase audit performed on `vasleafrmsovdbdlrcsw` using project metadata, table/migration inventory, `pg_policies`, role grants, RPC signatures and bucket metadata. No token impersonation, no test fixture rows or data changes.
- Reproducible Android source: owner-accepted Lab V337 `4ba1263124d118d3917f988228aec2248068466f`. Visual UX stack `6654c730971309b5047473e2f69292fe799b3f46` NOT integrated.
- The founder explicitly chose no migration of old cloud test data. Existing live Lab test records are not removed.

| ID | Area | State | Actual requirement | Correct result / evidence |
|---|---|---|---|---|
| L01 | Foundation | STATIC VERIFIED | Lab project ACTIVE_HEALTHY | Supabase connected live project metadata |
| L02 | Foundation | STATIC VERIFIED | Installed fresh V337 migrations | Two migration history names returned |
| L03 | Security | STATIC VERIFIED | Four exposed app tables use RLS | Four / four enabled; 14 auth.uid-scoped policies |
| L04 | Security | STATIC VERIFIED | Anonymous app table reads denied | No anon SELECT on four app tables |
| L05 | Security | STATIC VERIFIED | Creator/Personal RPC anonymous execute denied | Five app RPCs non-public, auth.uid-owned SECURITY INVOKER |
| L06 | Security | STATIC VERIFIED | RLS helper function execute closed | No anon/authenticated EXECUTE on rls_auto_enable |
| L07 | Storage | STATIC VERIFIED | Private voice bucket config | creator-voice-ideas private, audio/mp4, 20MiB, 3 owner-scoped policies |
| L08 | Storage | PENDING | Actual voice upload, restore, deletion and cross-account denial | Zero objects at audit; real authenticated API/Android evidence required |
| L09 | Auth | PRIOR OWNER ACCEPTANCE | Lab APK fresh Android launch and Google sign-in | PR #411 owner-accepted SHA 4ba126; not retested in this audit |
| L10 | Auth | PENDING | Refresh persisted session after process death/reboot | Real Google test identity; old Google JWT not reused in Lab |
| L11 | Auth | PENDING | Logout/second-login generation fencing | No callbacks or queued ops may write under previous user |
| L12 | Auth | PENDING | OAuth new project Web Client and Android SHA-1 variant | Lab package + signer and prod cutover independently validated |
| L13 | Security | PENDING | Two distinct Google user JWTs: SELECT cross account | Second user sees no creator project/profile/Personal records |
| L14 | Security | PENDING | Two distinct JWTs: INSERT foreign user ID | RLS rejects or auth.uid overrides; no foreign record created |
| L15 | Security | PENDING | Two distinct JWTs: UPDATE/DELETE foreign records | Denied, remains unchanged; status captured without private data |
| L16 | Security | PENDING | Account A voice Storage path accessed by account B | Denied, no public URL bypass |
| L17 | Sync | PENDING | Existing Creator snapshot explicit save | Real test project + idea, revision/hash and checked receipt |
| L18 | Sync | PENDING | Existing Creator snapshot explicit restore | Identical restored metadata/content, no phantom project duplicates |
| L19 | Sync | PENDING | Existing Creator head concurrent CAS revision conflict | Second unsynced client cannot silently replace unrelated edit |
| L20 | Personal | PENDING | Current Personal JSON snapshot save with no Challenge | Real Reminder + completion, account-scoped CAS |
| L21 | Personal | PENDING | Challenge coverage guard intentionally refuses partial archive | No upload/overwrite if single-activity challenge exists |
| L22 | Personal | PENDING | Broader encrypted Personal archive not provisioned | No advertised success; V2 heads and bucket not present |
| L23 | Sync v1 | PENDING IMPLEMENTATION | Schema proposal reviewed and migration isolated | No DDL without founder approval, RLS/lint verified |
| L24 | Sync v1 | PENDING IMPLEMENTATION | Atomic per-record CAS | Correct revision advances once; stale version returns CONFLICT |
| L25 | Sync v1 | PENDING IMPLEMENTATION | Idempotent retry by operation_id | Same payload same ack; changed payload same op rejected |
| L26 | Sync v1 | PENDING IMPLEMENTATION | Device/account delta pull cursor | No lost out-of-order changes, no duplicates, bounded pages |
| L27 | Sync v1 | PENDING IMPLEMENTATION | Tombstone after delete vs offline phone reappearing | Old device cannot resurrect deleted record |
| L28 | Sync v1 | PENDING IMPLEMENTATION | Offline append/edit with durable outbox | Local save succeeds and reconnect syncs once |
| L29 | Sync v1 | PENDING IMPLEMENTATION | Simultaneous script editing two devices | Explicit conflict resolution no blind last-write-wins |
| L30 | Sync v1 | PENDING IMPLEMENTATION | Project and Creator Idea bidirectional update | Exact stage/asset IDs, no duplicate XP, real linked project |
| L31 | Sync v1 | PENDING IMPLEMENTATION | Personal Reminders and Groups bidirectional update | Same portable schedule, no cross-device notification tokens |
| L32 | Sync v1 | PENDING IMPLEMENTATION | 21-day Challenge Done/Undo receipts and dates | No fabricated/backdated completion or double reward |
| L33 | Sync v1 | PENDING IMPLEMENTATION | My Spaces and Activity log replication | Actual source-linked entries, no phantom progress |
| L34 | Sync v1 | PENDING IMPLEMENTATION | Private photos/voice opt-in policy if approved | Authorized encrypted upload, content hash, cost bound |
| L35 | Sync v1 | PENDING IMPLEMENTATION | Per-domain status UI after partial success | Creator saved + Personal failed shown separately |
| L36 | Sync v1 | PENDING IMPLEMENTATION | Limit exceeded, 429/5xx and expired JWT | Exponential retry, no false green status or local data loss |
| L37 | Sync v1 | PENDING IMPLEMENTATION | Phone OS kills app mid-batch | Outbox resumes once and handles server-already-applied op |
| L38 | Sync v1 | PENDING IMPLEMENTATION | Two phones same account with lost network | On reconnect converge or clearly present conflicts |
| L39 | Sync v1 | PENDING IMPLEMENTATION | Sign out, sign in as different account | No cross-account local UI/workers/outbox/uploads |
| L40 | Sync v1 | PENDING IMPLEMENTATION | Device notification permissions and schedules | Sync plans not actual OS alarm lease; both devices are independent |
| L41 | Integration | PENDING | Lab V337 + UX10F cumulative source merge audit | Keep 3 shared source paths/API26 fixes/recovery verified |
| L42 | Integration | PENDING | Full Kotlin, JVM, Android 15, app restore and physical QA | Exact source APK hash, screenshot, test logs, founder acceptance |
| L43 | Release | PENDING | Accurate backup, sync, account deletion and privacy policy | No false complete backup, no public release without owner approval |

## Execution discipline

A. **BL-02A static discovery (performed)** confirms API surface ownership and a safe starting point; it cannot simulate two real Supabase Auth JWT users.
B. **BL-02B bounded runtime validation (not performed)** needs disposable Lab-only Google credentials/identities and an isolated test APK. Do not log IDs, emails, refresh tokens or user media. Test the real Data API with both JWTs, not privileged admin SQL.
C. **UX09 staging (not started)** requires founder review of the exact schema and Android architecture brief. Follow the App source invariants, use disposable databases for destructive race/tombstone tests. No new resource cost without approval.
D. **Final full-candidate verification (not performed)** occurs only after actual UX09 source integration, Lab V337/P0/API26 reconciliation and the staged UX01–08/UX10 stack. Full JVM/Android/physical acceptance belongs to that final campaign.
E. **Never fix missing Personal Challenge coverage by disabling the guard**. Reject partial backups until receipts, groups, Spaces and related original stores are covered and proven. Never turn old snapshot 'sync' badges into full backup or automatic 2-device guarantees.
F. **No account/data deletion in this acceptance phase.** Complete account erasure must have a separately reviewed Auth/Storage/server cleanup design before public release.

**Stop-the-line:** any account-crossed read/write, missing RLS, hardcoded service role secret, user data clobber, duplicate confirmed Done/XP, old alarms incorrectly activated on a new device, backdated Challenge completion, unsafe restore, unreviewed Supabase charge, or missing protected V337 safety fix.

**Current decision:** BL-02 static foundation VERIFIED, BL-02B real dual-auth and cloud behavior OPEN. UX09 architecture PROPOSED and awaiting controlled source/DDL approval. No merged PR, new APK, or public release.

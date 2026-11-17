# Backlot UX09 — Account-scoped, offline-first cross-device synchronization architecture V1

**2026-11-17 | PROPOSED architecture, documentation-only. NOT implemented or approved as a schema/app-code change.**
**Foundation:** owner-tested Backlot Lab V337 `4ba1263124d118d3917f988228aec2248068466f`, separate application ID `com.framebynavin.app.lab`, fresh Supabase `vasleafrmsovdbdlrcsw`.
**Feature source:** UX10F draft head `6654c730971309b5047473e2f69292fe799b3f46` has the remaining UX01–08/UX10 source staging. Never merge UX code unreviewed or claim it has been compiled.
**Founder preference:** start afresh; **no copy of old Supabase data**. Do not delete the old project, a Lab account, or an app's local records without explicit separate consent.

## 1. Problem and differentiated experience

- Two signed-in Android devices should see the same **supported account-owned Creator and Personal records**, without requiring a user to export/import entire backups or tap Sync Now after every small edit.
- **Local-first remains essential**: the app is usable without Wi-Fi, but explicitly communicates pending/offline/failed/conflicted sync, and never marks a record cloud-safe before its server revision is confirmed.
- The current V337 snapshot backup is useful as an emergency *limited* point-in-time backup but NOT an adequate foundation for safe concurrent fine-grained editing: entire payload replacement is wasteful, conflicts unrelated entities, and cannot protect current Challenge, Spaces and some media.
- Sync success, backup recovery, and device-side reminder delivery are three separate features. Do not imply sync completeness when photos, private songs, alarms or other domain records are excluded.
- Business constraint: Free-tier-conscious, avoid mandatory paid infrastructure or always-on websocket Realtime on day one; Android foreground/connectivity + bounded WorkManager sync. Background latency is best-effort, not an instant guarantee.

### Five invariants (non-negotiable)

1. **Canonical account owner:** the *new* Supabase `auth.uid()` identifies account ownership, not email, user-editable Google metadata, or any payload-supplied user ID. The client has a publishable key; never embed `service_role` or secret.
2. **Local transactional persistence first:** commit the domain record AND durable outbox operation in the same local transaction before presenting it as saved. User work is never dropped because of network failure.
3. **No blind overwrite:** server applies a change only against the revision the client actually observed, or returns CONFLICT. Identical retries with the same operation ID are idempotent. Do not silently resolve script text, Notes, due times, Challenge confirmations or private photos via last-write-wins.
4. **Tombstones and deterministic identity:** stable UUID record IDs scoped to `auth.uid()`; durable deleted markers prevent offline device resurrection. Do not re-use old Creator/Personal ID as an implicit cross-account identity. Old account local data adoption always requires explicit review.
5. **Device-local alarm/music:** synchronized reminder plans are *data*; each phone separately checks Android permissions and schedules its own alarm. No PendingIntent, OS permission status, lock-screen playback token, or local file URI is synced as if portable.

## 2. Proposed server data model (review before any migration)

**Hybrid lean envelope + append-only facts:** one generic, strictly validated record store for most domain payloads; immutable receipts for confirmations/achievements; private media only if explicitly approved.

| Proposed resource | Key fields | Purpose / protection |
|---|---|---|
| `sync_entities` | `(user_id,domain,entity_id)` PK, `revision bigint`, `schema_version`, `payload jsonb`, `deleted_at`, `updated_at`, `last_operation_id` | Canonical versioned domain entity. RLS `user_id=(select auth.uid())`, authenticated only; JSON type and size validation by domain, no privileged JSON ownership override |
| `sync_device_cursors` | `(user_id,installation_id)`, `last_ack_sequence`, `last_seen_at` | Per-installation pull progress and bounded cleanup; installation ID randomly generated, not IMEI/advertising ID; server trust bounded |
| `sync_account_sequences` | `user_id`, `next_sequence` | Account-scoped transaction row lock assigns committed monotonic change sequence. Avoid lost pull events from out-of-order timestamp/sequence commits |
| `sync_change_log` | `(user_id,sequence)`, domain, entity ID, revision, delete flag, operation ID | Incremental delta pull, reliable cursors and change replay; apply transaction is atomic with entity update and sequence allocation |
| `sync_applied_operations` | `(user_id,operation_id)`, request digest, result revision/sequence | Retry deduplication; same op returns same result; same UUID with different digest rejected. Bounded retention policy must be approved with cleanup/cursor safety |
| `sync_receipt_events` | `(user_id,event_id)`, activity/challenge/stage reference, kind, original occurred-at, timezone and source record version | Immutable confirm/undo and reward event stream where needed. Avoid fabricated/doubled 21-day Done/XP on multiple devices |
| `sync_media_manifests` + private Storage | account path, content SHA-256, MIME, byte length, owning record/revision; private media bytes | Only for approved Voice Ideas and future encrypted optional private photos. No public bucket/signed URL without authorization; no local URI portability |
| Existing `creator_sync_snapshots` / `personal_sync_heads` | current Lab tables, unchanged during initial planning | Interim legacy snapshot backup. Do not run two unsupervised conflicting writers against both old snapshot and new per-record sync |

These are **proposals**, not live tables. The existing Lab database still has only its four app tables and its voice bucket. There is no need to replay historical backup migrations or provision `personal_archive_v2` before a separate encryption/restore decision.

### Atomic server contract (proposal only)

`apply_sync_operation(opId,domain,entityId,expectedRevision,schemaVersion,payload,deleted)`:
1. Authenticate and derive `user_id` from `auth.uid()`, reject unauthenticated/anonymous identities, invalid owner/domain/size.
2. Check the exact `(user_id,opId)` idempotency record; if found and digest identical, return confirmed previous result.
3. Lock the account sequence + target record in transaction. Reject a different `expectedRevision` as CONFLICT (return current version metadata, no content from other users).
4. Validate schema version and allowed fields/domain, writes to a distinct entity or a tombstone, never silently erase a parent/child.
5. Allocate monotonically committed per-account sequence while serialized; insert operation acknowledgement and changelog atomically.
6. Return `APPLIED`, `ALREADY_APPLIED` or `CONFLICT` with record revision/sequence. No false success on offline timeout.

`pull_sync_changes(sinceSequence,limit)`:
- Authenticated user only, bounded page sizes and timeouts, deterministic ordered `sequence` and next cursor, include tombstones and schema version, never expose records owned by other JWTs.
- Conflict negotiation is app-level and domain-specific. Never use untrusted `user_metadata` in authorization. Prefer SECURITY INVOKER functions and carefully reviewed `GRANT` / `WITH CHECK`; no public SECURITY DEFINER workaround.
- A compromised/malformed payload must fail input validation without arbitrary executable SQL/RPC.

**Important design risk:** a universal JSON envelope is inexpensive to iterate but needs strict per-domain codec/schema validation, indexes and payload size limits. If a domain needs server-side queries or integrity constraints (e.g. ownership of cross-linked projects), promote it to a typed table in an explicitly reviewed later migration. Do not hide data integrity in unvalidated JSON blobs.

## 3. Domain coverage & conflict semantics

| Domain | Candidate scope | Conflict / special rule |
|---|---|---|
| Creator projects + tasks | Phase 1 | Stable canonical ID, stage/checklist/due/source links; same-record concurrent edits require review; no fake publish or XP |
| Creator ideas, formats, script notes and Content Workspace | Phase 1 + 2 | Separate record IDs/revisions for large editable modules; no whole-project overwrite when script changes on another device; original voice media referenced by validated manifest |
| Creator reminders/content calendar | Phase 2 | Sync scheduled *intent*, not PendingIntents/notification delivery tokens; each device revalidates permissions before schedule |
| Personal Reminders, completion records and groups | Phase 2 | Preserve occurrence/Done unique keys, group membership and ownership, portable schedule only; no account-crossed tokens |
| Single-activity 21-day Challenges | Phase 3 | Original start date/timezone and exact confirmed/undo receipts; no backdated Done, no duplicate XP, per-device 10-minute snooze remains local |
| My Spaces, notes, activity logs, progress and Personal routines | Phase 3 | Stable Space/entry IDs; accurately recorded activity, real linked references; no 'synced' badge until exact domain confirmed |
| Progress Journey, Creator achievements, Daily Brief, Content Calendar | Phase 3 | Prefer derived read-only views from canonical source + idempotent event ledger. Do not sync duplicate derived counts or recompute historical rewards blindly |
| Private photos / Best Frame photos / original music | Separate privacy/media gate | Explicit opt-in, private account path, appropriate client-side encryption for sensitive archives, robust download/conflict/retention cost controls; never silently upload to cloud |

**No full-backup claim** until all approved domains included, media/restore and true cross-device restore are proven. Supported-domain receipts must enumerate exactly what is and is not covered. An existing 21-day Challenge guard must not be simply deleted to make old partial JSON backup green: enable new protected Challenge sync first and change the gate only once certified.

## 4. Android/local architecture (no implementation in this PR)

- Keep Room/DataStore/Creator stores as the UI's immediate read source; avoid rendering direct REST results as durable data until locally validated and committed. Compose UI only observes local source state.
- Create an account-scoped durable `SyncOutbox` + `SyncCursor`, with installation UUID and schema/version. Domain adapter writes include original record source and revision; isolate when user switches Google identities.
- Local mutation transaction: **record update + queued operation**; idempotent uploads use exact stable opId. Pull/apply validates checksum/schema/reference before commit; no implicit replacement of local-only private files.
- On user editing while online: debounce and sync on supported network in foreground. On connectivity regained, app foreground and eligible WorkManager periodic best-effort: bounded page fetch, exponential backoff, rate limiting and battery/network requirements.
- Avoid continuously open Realtime sockets for MVP; foreground pull/sync and user-driven refresh first. If measured usability needs subsecond live updates, separately review authenticated Realtime costs and RLS policy.
- Account transition: invalidate previous generation, stop old account workers, fence all queued callbacks, keep old local records quarantined from new identity. Founder accepts fresh setup; new Lab test install begins with fresh workspace by default, not forced destructive wiping.
- Conflict UI: clear side-by-side `This device`, `Cloud`, `Review changes` with original edit timestamps and domain labels. No automatic keep-cloud that destroys a Creator script, missing Personal receipt or private media.
- Status UI: `Local saved`, `Waiting for network`, `Syncing`, `Synced on this account`, `Needs review`, `Error / Retry`; separate `Backup recovery ready` only after verified encrypted archive or replayable full history. Prevent source-run status ambiguity when Creator succeeds but Personal fails.
- No cross-device notification duplicate promises. Each phone is allowed to alert for a synced reminder as its own Android-local policy; offer per-device reminder enablement and review (default policy explicitly approved before launch).
- Portable schedule excludes custom `content://` music URIs, Android exact alarm authorization, alarm occurrence leases, UI focus or device file paths. Private Photo Journal excluded from unapproved cloud scope.

## 5. Low-cost model and safeguards

- **No additional Supabase account, paid tier, Edge Function, Realtime add-on, Firebase setup, storage CDN, push provider or third-party subscription as default.**
- Versioned Postgres migration on current Backlot Lab only *after* founder approves scope and security. Publishable Android key only; secrets are server-side, never copied into app/GitHub source. New OAuth Android package+signer must be verified for each release variant.
- Restrict record envelope byte size, change log page size, media size, upload frequency, idle network sync and retained history. Monitor Free-tier database/storage/egress/Auth/project usage using dashboard; introduce rate-limit/backoff without throttling local saves.
- Security: RLS and Data API GRANTs for each resource, account-isolation negative tests, no unsafe user_metadata owner decisions, no `service_role` client code, Storage folder ownership, photo encryption, secure logs and role-limited RPC.
- Policy requires explicit later review of device-loss recovery, account termination and deletion of cloud object versions (including private Storage), update conflict UI and long-tail offline device tombstone retention.

## 6. Execution roadmap / approval gates

| Gate | Planned implementation boundary | Pass criteria |
|---|---|---|
| UX09-00 | This BL-02 foundation audit + architecture draft, DOCS ONLY | Project/account/static security identified; no cloud mutation |
| UX09-01 | Isolated schema and migration PR **after approval** | Disposable Postgres tests, RLS 2-auth negative tests, atomic revision/opId/sequence/tombstone correctness |
| UX09-02 | Android sync core/outbox + Creator domain adapters | Two Lab devices; add/edit/delete/restore Projects/Ideas and script with conflict guards; offline retry |
| UX09-03 | Personal Reminders/Groups and device-local scheduling | True exact Done and schedule parity; no token cross-device leak; Creator sync unaffected |
| UX09-04 | 21-day Challenge, Spaces, logs and event/reward parity | All 21 original dates/receipts, Space identity, no false XP; no Legacy backup guard bypass |
| UX09-05 | Controlled private media/cloud archive opt-in policy and recovery | Explicit owner-approved coverage, encrypted/authorized upload/download, device restore, deletions and cost cap |
| UX09-06 | Cross-account/2-device negative QA, UX status copy and integrity gate | Full supported-domain checklist; latency/battery/data measured, offline, tombstones, conflict and Android alarm verified |
| UX09-07 | Reconcile complete UX10F + Lab V337 P0/API26 startup fixes on NEW isolated integration branch | Original stable and owner-tested Lab SHA untouched; complete JVM/Android/physical test evidence |
| Launch | Founder physical APK acceptance and new release authorization | Rollback, account deletion, privacy, Play/OAuth and backup truth signed off |

**Ordering nuance:** UX09 server and core adapters may be implemented on an isolated Lab-derived candidate, but the complete app UX integration must happen before final release certification. The founder's earlier one-time **full** test campaign remains the final step; targeted disposable schema/auth contract checks are necessary during feature development to avoid unsafe migrations.

## 7. Founder decisions required before invasive work (NOT BLOCKING DOCUMENTATION)

- Confirm whether first launch supports Google-only plus Local-only mode (recommended: yes), and which Personal sensitive domains are approved for cloud storage vs strictly device-private.
- Confirm synchronization expectation: foreground near-real-time and background best-effort (recommended, avoids paid push/realtime commitment).
- Confirm how to handle same record edited on two phones: explicit review on conflict (recommended) instead of automatic last-write-wins.
- Separate product decision on deleting legacy launcher widgets; never include provider removal in a backend migration.
- Require explicit approval for actual SQL DDL, one-domain-at-a-time production source changes, Android server URL cutover, Auth/account deletion and releases.

**Status conclusion:** UX09 design direction PROPOSED, full feature NOT IMPLEMENTED. Backlot Lab is a viable secure foundation for staged implementation, but real two-user Google/RLS and media coverage acceptance remains pending before any release claim.

# RC8 — Server deletion barrier checkpoint

Base: verified RC7 source `7e6913321726f2221054cd03b3e4df2483e4a7cb`. This branch is backend-only and is NOT deployed, released, or promoted to the foundation integration branch.

## Confirmed finding

RC7's local deletion journal cannot stop another device from writing to Supabase. Existing owner-scoped RLS allows authenticated owners to use direct table writes and the old save_creator_backup/claim_creator_username RPCs. A delayed old deletion request can also arrive after reactivation. Client-only guards are insufficient.

## Reviewed candidate

The local source artifact is `FrameByNavin-v1.8.3-RC8-server-barrier-candidate.zip`, SHA-256 `00f6ea2aac790b580ed5ef9a800f71bf1e301d57b56980b5de0c576f1e3d2670`. It is a conversation artifact, not a GitHub release. It contains the migration, isolated SQL test generator, generated test, README and manifest. Migration SHA-256: `be3f2ac7b1eca945c443f370e90cad5398a974d53648d91c83bb9400196e0f71`. Generator SHA-256: `1470521334b103149176861faa92dd4538189fd4795ea96cd59c0204ecb3a298`.

The proposed migration adds a private owner lifecycle row with phase and generation, guarded INSERT/UPDATE/DELETE paths on the existing three creator tables, atomic owner-only deletion, and explicit generation-checked reactivation. A private transaction permit allows atomic cleanup while tombstoned. Both stale writes and stale deletion requests are fenced. No arbitrary target-user argument is exposed. The migration does not change or delete existing user rows when installed.

## Verification boundary

The existing production schema/RLS/function definitions were read. An earlier isolated, rollback-only SQL model passed synthetic owner-isolation, deletion, retry and reactivation tests. The revised full migration and generated acceptance harness have not yet completed authoritative PostgreSQL CI or production-schema compatibility verification. Do not describe this candidate as verified, deploy it, or ship a client depending on it. A compressed GitHub transport attempt returned the wrong blob hash and was rejected. The source bundle is preserved locally and in the conversation artifact; the complete revised source is not yet committed to GitHub.

## Next gates

Use a reliable exact-byte source transfer, then run the generated SQL in disposable PostgreSQL 16 with ON_ERROR_STOP. Verify atomic rollback, direct legacy DELETE, stale delete replay after reactivation, owner isolation, privileges and concurrency. Review the migration against the real schema in a rollback-only transaction and run security advisors. Integrate the server generation into Android writes/deletion/reactivation and run full Android CI. Deploy only after staging two-account HTTP tests and a safe rollout plan. Full Auth identity deletion requires a separate privileged server endpoint, fresh identity checks, session revocation and storage inventory. No live data deletion or production migration has been performed. RC7 and frozen v1.8.2 remain the safe baselines.
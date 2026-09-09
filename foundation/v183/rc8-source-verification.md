# RC8 backend verification checkpoint

Base: verified RC7 `7e6913321726f2221054cd03b3e4df2483e4a7cb`. The original source bundle is `FrameByNavin-v1.8.3-RC8-server-barrier-candidate.zip`, SHA-256 `00f6ea2aac790b580ed5ef9a800f71bf1e301d57b56980b5de0c576f1e3d2670`. Exact migration SHA-256: `be3f2ac7b1eca945c443f370e90cad5398a974d53648d91c83bb9400196e0f71`. Test generator SHA-256: `1470521334b103149176861faa92dd4538189fd4795ea96cd59c0204ecb3a298`.

## Current verification

The candidate archive and embedded source manifest were verified locally. The existing production schema was inspected read-only: all three creator tables have owner-scoped foreign keys to `auth.users` with ON DELETE CASCADE. The full revised migration has not yet passed authoritative PostgreSQL CI or a staging HTTP test. No production migration or live creator-data deletion has occurred. No RC8 APK exists.

## Additional review findings

The proposed generation header is a stale-request fence, not an authentication or user-consent mechanism. A current authenticated session can obtain its generation; explicit reactivation must not be confused with fresh identity verification or full account deletion. Existing direct writes and legacy RPCs must all be covered. The three-table barrier does not cover future tables, storage objects, refresh sessions, or Google grants. The planned full Auth-account deletion needs a separate privileged server flow and an explicit user confirmation.

The migration's private transaction permit, statement/row trigger ordering, owner checks, function privileges, zero-row DELETE behavior, and Auth cascade compatibility require real PostgreSQL tests. The test generator creates isolated schemas and synthetic identities inside a rollback transaction. Passing the earlier simplified SQL model does not certify the revised migration.

## Next gates

1. Preserve the exact migration and test generator in GitHub; reject any payload whose Git blob or SHA-256 differs.
2. Run the exact generated acceptance SQL in disposable PostgreSQL 16 with ON_ERROR_STOP, then add real concurrent-transaction tests.
3. Verify migration compatibility and RLS/function privileges against a staging copy of the real schema, including existing RPCs and Auth deletion behavior.
4. Coordinate the backend rollout with a generation-aware Android client. Do not enable a new write contract without a safe existing-device migration path.
5. Complete two-account HTTP tests, security advisors, and a safe rollback plan before production deployment. Then run Android CI and deliver an APK.

RC7 and frozen v1.8.2 remain the verified baselines. Do not claim RC8 is deployed, released, or fully verified.
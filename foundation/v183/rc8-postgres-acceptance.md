# RC8 PostgreSQL acceptance — 9 September 2026

## Verified checkpoint

The original backend migration and test generator are committed as readable source on `feature/v1.8.3-server-deletion-barrier`. Migration SHA-256: `be3f2ac7b1eca945c443f370e90cad5398a974d53648d91c83bb9400196e0f71`. Original generator SHA-256: `1470521334b103149176861faa92dd4538189fd4795ea96cd59c0204ecb3a298`. The original migration is unchanged by the three test-fixture revisions.

GitHub Actions run `34315277241`, commit `c52cafd1deeaa6957bdd376bc9617fa49d5a0b60`, completed successfully. All source, PostgreSQL 16 acceptance, rollback, and evidence-upload gates passed. Evidence artifact `10089837867`, ZIP SHA-256 `2b33169445b8676c6fcc4d3d6a2c38a332675a53cdb25e3b80796b00d7c4601d`. The downloaded artifact was independently checked. Its SQL log reports synthetic tests passed and ROLLBACK; the cleanup log confirms synthetic schemas and roles did not survive. No production credentials or live creator rows were used.

The suite covers owner-scoped deletion, injected failure rollback, retry idempotency, deleted-state write rejection, reactivation generation advances, old deletion replay rejection, stale and malformed write epochs, and cross-owner preservation/visibility. The first two CI attempts failed due to test-fixture errors: private function name resolution without schema access, then reading another owner's RLS-hidden rows. The corrected fixture uses catalog OIDs and a test-only privileged snapshot. These failures are preserved in the run history rather than hidden.

## Not yet verified or released

The core synthetic suite does not establish production readiness. The original migration remains undeployed and has not been promoted to integration. No RC8 Android APK exists. The current migration's NULL-auth DELETE path skips the statement guard but the row guard still requires auth.uid(), creating an unresolved compatibility risk for Auth-user ON DELETE CASCADE. The live schema confirms all three creator tables reference auth.users with ON DELETE CASCADE. The current migration must be tested and corrected against the actual privileged Auth deletion path before deployment.

Additional mandatory gates: separate-session concurrent write/delete/reactivation tests; real-schema migration compatibility and existing RPC tests; private-function privilege and search_path review; two-account authenticated HTTP tests; generation-aware Android client and legacy-device rollout; storage/identity inventory; and a reversible deployment plan. Full Auth-account deletion remains a separate privileged, explicitly confirmed feature. No live migration, deletion, or account identity change has occurred. RC7 and frozen v1.8.2 remain the verified Android baselines.

# Backlot UX09-02A-S2 — per-entity OS lease for sealed offline-chain writes

**Status:** source-staged, exact-head CI not yet certified. **No existing app runtime caller.**

## Parent evidence
- UX09-02A-S source-only encrypted chain store PR #441 (`3d96387abcc55d9bb818ab738b72132a5f7b7da3`) remains draft and unmerged; synthetic 48-test Gradle gate #44142814555 was still running at branch creation.
- Prior PR #440 pure offline operation chain Gradle #44141130999 was still running. PR #437 and field coverage PRs #438/#439 previously passed their scoped CI.
- Original stable V337, Lab baseline and live production backend are not modified.

## Exact four-file change budget
1. `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09SealedChainStore.kt`: wraps the **entire existing local storage revision check plus atomic encrypted rename** in a stable, per-account/per-domain/per-entity Java NIO FileChannel OS lease. The lock-file inode is **never deleted** to avoid two processes leasing different inodes. Symlinked lock files/account directories are refused. Lock contention fails closed with no queue deletion or overwrites.
2. `app/src/test/java/com/framebynavin/app/cloud/ux09/Ux09SealedChainStoreTest.kt`: two synthetic JVM regression cases added (existing 17 + 2 = 19). One orchestrates two distinct store objects using synchronized latches and proves a competing stale writer cannot commit, while the lease holder advances revision. Another proves a symlinked guard cannot redirect access or mutate external files.
3. `.github/workflows/backlot-ux09-02a-sealed-lease.yml`: focused Android JVM CI for all 18 prior journal tests, 13 offline-chain tests and 19 sealed-chain cases: 50 test methods. No hosted Auth, no Android APK and no runtime sync activation.
4. This gate document.

## Verified by construction, but not yet physically certified
- A per-instance in-memory lock is insufficient because processes or separate instances can read the same `storageRevision`. The OS-level lock must encompass read → validate expected local revision → encrypt → temp fsync → rename → parent directory fsync.
- `FileChannel.tryLock()` is deliberately nonblocking; competing writers are rejected so the future caller must retry after reading the committed revision. A process crash releases its OS lock. The stable `.guard` file remains in place; do not auto-delete it on logout before lifecycle review.
- The protected data file is still authenticated using AES-GCM owner/epoch/entity/revision AAD as in parent. Readers rely on atomic rename to see the old or new complete sealed file; there is no unverified plaintext fallback.
- **Important boundary:** JVM tests exercise separate instances in one JVM, not actual Android multi-process/app death. Verify device NIO `NOFOLLOW_LINKS`, directory fsync support, filesystem power-loss behavior, and keystore separately.

## Unresolved critical hazards
- **Rollback:** a previous correctly encrypted blob remains authentic. No anti-rollback checkpoint exists yet across device restarts.
- **Two-store atomicity:** this encrypted queue is not atomically coordinated with existing DataStore Project/Idea editors or V344 restore. The future integration must not acknowledge queued sync unless local canonical save is durable.
- **Cross-process enforcement:** static/os-level locking claims need Android instrumentation and true separate-process test. A lock file itself must reside in an app-private, symlink-safe directory; permissions and backup exclusions still need review.
- **No Cloud integration:** no WorkManager, OAuth sign-in, Project/Idea codec, voice media uploads, scripts beyond 16KiB, remote conflict UX, Auth/data account deletion, full backup or restore.
- **No release:** original V337, old snapshot sync and Backlot Lab remain untouched; no merges/APKs/hosted writes.

## CI acceptance
- Exact PR head must be completed SUCCESS in its own focused 50-test suite, including both new guard tests and no-network disabled-feature check.
- Review only these four changed files and retain **draft/unmerged** status.
- Proceed to the separate key-loss/rollback checkpoint design and DataStore transaction recovery only after this evidence passes. No production integration without explicit founder review and dual-device physical QA.

# Backlot UX09-02A-S — protected, disconnected offline operation-chain store

**Date:** 2026-11-17  
**Status:** Kotlin source staged, focused JVM tests PENDING. No app runtime integration, no device testing, no APK.  
**Base:** UX09-02A-O pure offline chain PR #440 (prior Actions #44141130999 running at initial branch cut).

## Scope and exact files
1. `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09SealedChainStore.kt`: pure injected-secret-key AES-256-compatible GCM encrypted chain serialization; owner, account epoch, restore epoch, domain, entity ID and storage revision all authenticated through AEAD associated data; binary format `UX09GCM1`; unique random 96-bit nonce; strict local 512 KiB plaintext and 1 MiB blob ceiling; fsync temp → same-directory ATOMIC_MOVE → directory fsync with no silent unsafe fallback.
2. `app/src/main/java/com/framebynavin/app/cloud/ux09/Ux09AndroidKeyProvider.kt`: *unconnected* per-account Android Keystore AES-256/GCM key generation, account-keyed alias. Never exports raw device keys. **Unit tests use synthetic JVM `SecretKeySpec`, NOT AndroidKeyStore.**
3. `app/src/test/java/com/framebynavin/app/cloud/ux09/Ux09SealedChainStoreTest.kt`: 17 synthetic JVM test cases exercising encryption at rest, same-operation retry across process recreation, optimistic local revision checks, wrong account/restore generation, wrong key, ciphertext tampering, cross-account and cross-domain transplant, interrupted tmp handling, conflict and tombstone persistence, strict exact-key JSON, IV uniqueness, symlink protection, and honest rollback limitations.
4. `.github/workflows/backlot-ux09-02a-sealed-chain.yml`: focused JVM compile+tests (17 new + 13 original chain + 18 journal = 48), existing feature gate must remain OFF and no cloud clients/workers in `cloud.ux09`. No APK artifact.
5. This documentation only.

## Security and durability design
- Each chain has its own local encrypted file under an account-scoped directory; filename is fixed from reviewed domain enum + canonical UUID. An AndroidKeyStore key is associated with that account only. Under the same device key, swapping encrypted ciphertext across owner/epoch/entity/file name fails AEAD authentication. Authentication failures **preserve original bytes**; never silently clear/repair the cloud cursor.
- One writer *instance* requires an exact previous storage revision, incremented on every successful replacement. An incompatible version refuses to overwrite.
- The queued edits and prepared immutable `Ux09Intent` are encoded with exact-field-set JSON and validated for chronological local before/after hash chain, operation UUID identity, stable expected revision, tombstone terminality, 16KiB canonical per-operation payload and no device-local paths.
- Crash boundaries are represented by pre-existing .sealed and .tmp files; an unfinished temp prevents the next writer from silently advancing. This class **does not** coordinate with existing DataStore writes, WorkManager, or the remote RPC.

## Explicit known limitations, release blockers
1. **NO real app callers / feature OFF:** the new encrypted store is neither connected to `TaskStore` nor `IdeaVaultStore`, `CreatorDataGate`, Auth, backup/restore or the old snapshot worker. No user data passed through it. The earlier plaintext `Ux09DiskIntentJournal` continues to exist as a disabled experimental predecessor.
2. **Rollback risk remains:** GCM verifies authenticity, not freshness. Replacing a valid sealed file with an earlier valid revision can only be detected if some *independently trusted, durable checkpoint* is retained by the caller. Current tests demonstrate that a caller holding expected revision detects the rollback; this prototype alone does NOT solve restart-time rollback.
3. **Atomicity not proven:** `ATOMIC_MOVE`+fsync is not a transaction across DataStore, key generation and a second file. Directory fsync support on Android devices and actual power-cut behavior must be tested. The in-memory monitor does NOT prevent concurrent writes from different processes/instances; add OS-level lock and robust stale-writer checks before enabling.
4. **Key loss/reinstall/restore:** AndroidKeyStore keys normally do not survive app uninstall or device migration. Full Backlot backup must not include ciphertext without an explicit key-portability/restore strategy, and Auto Backup exclusion must be designed. This prototype must not advertise cloud recovery from these files.
5. **Security and lifecycle:** actual app-private encrypted storage root, Android Auto Backup exclusion, account-sign-out cleanup/quarantine, complete Auth/data delete, key rotation/deletion, cross-account logout and restore generation, keystore invalidation and memory privacy require separate integration/test phases.
6. **Payload completeness:** 47 top-level Project fields, 26 Idea fields and 21 nested workspace models must be encoded losslessly without leaking device-specific media URIs. The server's 16KiB JSON cap remains a hard blocker for full scripts, deliverables and histories.
7. **Offline flow:** existing file journal rejects second same-entity operation; the pure operation chain accepts them but this encrypted store is not a drop-in integration. Must demonstrate durable chain and original local DataStore save consistency before enabling.
8. **No hosted tests:** No new Supabase write, no QA accounts, no schema migrations, no Google Auth setting or production backend changes.

## Acceptance gate
- GitHub Actions focused sealed-store run **successful at exact PR head**. Static isolation/no-network check plus focused old/new JVM tests green. Source-only review against expected 5 changed files. PR remains DRAFT and UNMERGED.
- **Next:** isolated restart/fault-injection tests of concurrent store instances; cryptographic archive/restore threat model; typed Creator Project/Idea portable codecs; and migration handoff plan. Do not connect real records until durability, privacy, model fidelity and dual-device physical testing are independently certified.

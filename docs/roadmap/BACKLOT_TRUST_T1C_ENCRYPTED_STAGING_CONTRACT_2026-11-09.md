# Backlot Trust T1C — encrypted staging contract
9 November 2026 • code-review candidate, not production storage

## Evidence status

T1C1 source introduces \`TrustEncryptedStagingCodec\` and eight pure JVM
test cases plus one Android 15 fake-owner test, registered for the **single
T1 phase-end integration gate**. The source has not been compiled or executed
as part of this increment. Do not mark T1C as complete or ship an APK.

Unlike existing Creator JSON + Best Frames archives and the encrypted Personal
ZIP v2, this codec can encrypt arbitrary *provided and already-frozen* source
parts. It does **not** itself know where all Backlot data lives and cannot
freeze Creator+Personal stores. No call from a Back Up button, cloud client,
scheduler, account sync or restore path was added.

## Intended temporary format

- ZIP has anonymous generic entry names only: \`header.bin\`,
  \`part-0000.bin\` encrypted manifest, then \`part-0001.bin\` etc.
- Header contains format/version, PBKDF2-HMAC-SHA256 iteration count,
  random 16-byte salt and part count. It contains NO owner name/id,
  reminder titles, filenames, tokens, project titles or private content.
- PBKDF2-HMAC-SHA256 (210,000 iterations) derives an AES-256 key from an
  owner-supplied passphrase (at least 12 characters), using the new random
  salt. The passphrase is not stored in archive.
- Encrypted manifest includes an asserted owner ID and each source's
  identifier, record count, expected raw byte length and SHA-256 checksum.
  Future authenticated cloud binding is NOT provided by the asserted ID alone.
- Every 64 KiB plaintext chunk is AES-256-GCM encrypted with a fresh random
  96-bit nonce and 128-bit authentication tag. Associated data authenticates
  the SHA-256 of exact header bytes, part index, chunk index and terminal-frame
  indicator. Final zero-plaintext authenticated frames detect truncation and
  cross-entry rearrangement.
- No single part exceeds 64 MiB; total plaintext does not exceed 1 GiB;
  <=800 source parts, bounded manifest and header sizes. Avoids monolithic
  media buffers. These limits must be reconciled with future actual archive
  sizing before permitting cloud upload.
- Writer compares actual streamed source byte length and SHA-256 against a
  caller's explicit immutable-source declaration. If source changed, the
  output is incomplete and **must be discarded**.
- Reader validates header, passphrase, AES-GCM tags, encrypted account ID,
  every chunk, mandatory terminal frames, part sequence, source SHA-256 and
  exact byte lengths. Only then returns disposable app-private staged files.
- \`TrustVerifiedStagedBundle.close()\` unlinks staged files. On Android flash
  storage, unlink does NOT guarantee physical secure erase; production must
  minimize sensitive plaintext staging lifetime and use app-private storage.

## Critical nonclaims / pending gates

1. This is **not** Backlot universal backup. Both
   \`canClaimCompleteBackup\` and \`canUploadAsUniversalBackup\` are hard-false,
   even after exact cryptographic verification. A partial set of sources
   remains partial even if encrypted perfectly.
2. Owner ID inside an encrypted manifest is an assertion from the input
   caller, NOT proof of server authentication. T1C2/3 requires verified
   \`CloudSession.userId\`, independent authenticated owner-scoped keys,
   device/key recovery and account-switch protections.
3. Two-pass T1B4 inventory cannot guarantee a frozen version across
   CreatorDataGate, Personal mutation/Room/photo lock, SharedPreferences
   producers and independent file readers. T1C2 must freeze actual bytes,
   not just reference hashes, and make partial captures fail closed.
4. No backend object create, no temporary remote upload, no CAS, no history
   pointer update, no deletion and no automatic backup run. T1C3 needs
   immutable multi-version remote objects, readback confirmation, retained
   last-known-good revision, honest failure states and quota limits.
5. Existing T1B domain gaps still include some notifications, Spark runtime,
   retired Hero frames and unreferenced/external Creator media. T1C must
   present explicit unsupported exclusions and cannot claim all data copied.
6. T2 restores require a separate explicit owner-reviewed and journaled
   cross-domain recovery transaction. Decrypting a staged package is NOT
   authorization to import anything.
7. Automated crypto/data tests must cover wrong credentials, tampering,
   missing/extra ZIP entries, truncation, Unicode, >64 KiB media, duplicate
   paths, source mutation, temporary plaintext cleanup and signed Android
   upgrade effects. All full emulators remain at the phase-end gate.
8. Release depends on source compilation, unit/device test success, physical
   owner testing and separate signed APK acceptance, not this document.

## Next implementation stages

- **T1C2:** capture actual immutable authenticated owner source bytes under a
  documented multi-store consistency protocol. Reuse existing validated
  Creator portable and Personal encrypted ZIP v2 where appropriate, without
  double-encrypting huge byte arrays in memory or losing new challenge Done
  timestamps. Ensure voice/original media and Space JPEG bytes are included.
- **T1C3:** internal encrypted local staging coordinator using AtomicFile,
  post-write verify, no owner-facing success until readback; account/signer
  binding and strict manifest coverage.
- **T1C4:** authenticated provider-specific cloud backend, immutable versioned
  commit and retention; protect existing legacy Creator/Personal clouds.
- **T1D:** single stage-end test matrix across two real/emulated devices,
  crash/network interruption, owner switch, quota and photo-integrity proof.

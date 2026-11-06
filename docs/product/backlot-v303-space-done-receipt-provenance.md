# Backlot V303 — Receipt-attested Space journal recovery

**Draft engineering branch. No new signed APK, no backup restore writer, and no changed cloud coverage.**

## Why this exists

V289's standalone Spaces metadata codec intentionally exports **manual journal entries only**. It correctly refuses confirmed-reminder-Done entries because a free-standing Space note file is not proof a user actually tapped Done. That leaves a completeness gap in the newer V300 encrypted Personal archive: if a Space has a genuine confirmed Done entry, export previously failed rather than silently omitting it.

V303 provides a separate, *Personal-archive-only* extension. The older V289 standalone format and its refusal to import untrusted Done claims are not changed.

## Exact contract

- `V303SpaceReceiptArchiveCodec` packages older checksum-validated manual Spaces metadata alongside separately encoded confirmed Done entries.
- Every confirmed entry MUST have type `CHECK_IN`, source `CONFIRMED_REMINDER_DONE`, an existing Space ID, and a **matching original reminder ID + occurrence timestamp** in the Personal completion journal exported in the SAME archive. The entry's recorded occurrence time must equal that completion's `doneAtMillis`, and the journal-entry creation time cannot predate the real Done receipt.
- Duplicate entry IDs across manual/Done entries, or two Done entries for the same Space/reminder/occurrence, are rejected.
- The complete outer receipt payload is size-bounded and SHA-256 validated; the V300 archive additionally authenticates that payload using the archive's AES-256-GCM encryption and verifies all included photo files.
- If there are no confirmed Done entries, `encode` returns the original V289 manual-only payload unchanged. `decode` explicitly supports both old V289 and new receipt-attested entries.
- V300 archive creation, verification, V301 local read-only inventory and V301 conflict preview now accept this new Spaces format, always checking it against the accompanying real Personal completion history.
- `V303SpaceSnapshotReader` reads all Spaces and journal rows inside a single Room transaction for an explicit owner-selected export. It does not modify any entry, fabricate a receipt or change Creator/reminder/cloud state.
- Read-only previews still cannot authorize any writes. There is **no restore implementation**.

## Tests

- Nine `V303SpaceReceiptArchiveCodecTest` unit tests: legacy compatibility, genuine receipt roundtrip, missing receipt, mismatched timestamp/occurrence, duplicate Done, missing reminder, tampering/version rejection, orphan Space, duplicate manual/Done ID.
- One additional `V300EncryptedPersonalArchiveTest`: a real receipt-attested journal survives full password-protected ZIP encryption, authenticated staging, and decrypted validation with its completion event intact.
- One Android 15 in-memory Room test, `V303SpaceReceiptDeviceTest`: inserts a synthetic confirmed Done entry, exports a snapshot, validates against a real synthetic completion record, and rejects the same archived row when the Done evidence is missing. It cannot access or mutate the owner's Spaces database.
- CI expects ten V300 archive tests, nine V303 codec tests, and 11 synthetic Spaces/metrics/device tests. The V294 signed owner build remains frozen.

## What still blocks complete recovery

The archive must not claim full Backlot coverage: Creator projects/ideas/voice recordings and custom songs are separate. Photo files and Space notes must be reinstalled using an **owner-confirmed, crash-safe restore with collision handling and rollback** before any complete-restoration promise. V303 only improves fidelity and detection of backup contents. Done receipts remain vulnerable to a person intentionally creating and re-encrypting a fabricated owner file; an offline portable archive cannot independently attest who pressed the button. Backlot never creates new Done activity during export or preview.

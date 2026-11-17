package com.framebynavin.app.cloud.ux09

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class Ux09OfflineContractAndJournalTest {
    @get:Rule val tmp = TemporaryFolder()

    private val a = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val b = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val project = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    private val op = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    private val oldHash = Ux09Contract.sha256("local before".toByteArray())
    private val newHash = Ux09Contract.sha256("local after".toByteArray())

    private fun intent(
        accountId: String = a,
        opId: String = op,
        payload: String = """{"title":"Synthetic Project","status":"PLANNED"}""",
        before: String = oldHash,
        after: String = newHash,
        deleted: Boolean = false,
    ) = Ux09Intent(accountId, 3, 7, Ux09Domain.PROJECT, project, opId,
        0L, before, after, Ux09Contract.canonicalPayload(payload), deleted)

    @Test fun disabledForAllBuildVariantsAndNoConnectedWorkers() {
        assertFalse(Ux09LabFeatureGate.ENABLED)
    }

    @Test fun canonicalPayloadStableAcrossObjectKeyOrderAndUnicode() {
        val first = Ux09Contract.canonicalPayload("""{"b":[2,{"z":"✨","a":true}],"a":"x"}""")
        val second = Ux09Contract.canonicalPayload("""{"a":"x","b":[2,{"a":true,"z":"✨"}]}""")
        assertEquals(first, second)
        assertEquals(Ux09Contract.sha256(first.toByteArray()), Ux09Contract.sha256(second.toByteArray()))
    }

    @Test fun invalidUuidAndLegacyIdsFailClosed() {
        for (bad in listOf("", "old-task-id", "../outside", "00000000-0000-0000-0000-000000000000")) {
            assertThrows(IllegalArgumentException::class.java) {
                Ux09Contract.validate(intent().copy(entityId = bad))
            }
        }
    }

    @Test fun nestedDeviceUrisAndOwnerTokensAreRejected() {
        val bad = listOf(
            """{"audioLocalPath":"/data/user/0/audio.aac"}""",
            """{"workspace":{"access_token":"abc"}}""",
            """{"arr":[{"sound":"content://media/file"}]}""",
            """{"owner":{"user_id":"some-other-account"}}""",
            """{"alarmSoundUri":"file://local"}""",
        )
        for (json in bad) assertThrows(IllegalArgumentException::class.java) {
            Ux09Contract.canonicalPayload(json)
        }
    }

    @Test fun oversizeAndMalformedPayloadFailBeforeJournal() {
        val oversize = """{"title":"""" + "x".repeat(17_000) + "\"}"
        assertThrows(IllegalArgumentException::class.java) { Ux09Contract.canonicalPayload(oversize) }
        assertThrows(Exception::class.java) { Ux09Contract.canonicalPayload("[]") }
        assertThrows(Exception::class.java) { Ux09Contract.canonicalPayload("not-json") }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09Contract.validate(intent().copy(expectedRevision = -1))
        }
    }

    @Test fun serverRpcEnvelopeUsesOnlyReviewedArgumentNames() {
        val body = Ux09Contract.rpcApplyBody(intent())
        assertEquals(setOf("p_operation_id", "p_domain", "p_entity_id", "p_expected_revision",
            "p_payload", "p_deleted"), body.keys().asSequence().toSet())
        assertEquals("creator_project", body.getString("p_domain"))
        assertEquals(false, body.getBoolean("p_deleted"))
        assertFalse(body.toString().contains("account_id"))
    }

    @Test fun tombstoneMustContainOnlyEmptyObject() {
        Ux09Contract.validate(intent(payload = "{}", deleted = true))
        assertThrows(IllegalArgumentException::class.java) {
            Ux09Contract.validate(intent(deleted = true))
        }
    }

    @Test fun preparePersistsAndRestartBeforeLocalSaveCannotUpload() {
        val journal = Ux09DiskIntentJournal(tmp.root)
        val entry = journal.prepare(intent())
        assertEquals(Ux09DiskIntentJournal.Phase.PREPARED, entry.phase)
        val afterRestart = Ux09DiskIntentJournal(tmp.root).read(a, op)
        assertEquals(entry, afterRestart)
        assertEquals(Ux09DiskIntentJournal.Recovery.NOT_SAVED,
            journal.recover(afterRestart, oldHash, a, 3, 7))
        assertEquals(Ux09DiskIntentJournal.Recovery.READY_TO_RETRY,
            journal.recover(afterRestart, newHash, a, 3, 7))
    }

    @Test fun durableLocalSaveMustBeProvenBeforeReady() {
        val journal = Ux09DiskIntentJournal(tmp.root)
        journal.prepare(intent())
        assertThrows(IllegalArgumentException::class.java) {
            journal.markLocallySaved(intent(), oldHash)
        }
        assertEquals(Ux09DiskIntentJournal.Phase.PREPARED, journal.read(a, op).phase)
        journal.markLocallySaved(intent(), newHash)
        assertEquals(Ux09DiskIntentJournal.Phase.READY,
            Ux09DiskIntentJournal(tmp.root).read(a, op).phase)
        assertEquals(Ux09DiskIntentJournal.Recovery.READY_TO_RETRY,
            journal.recover(journal.read(a, op), newHash, a, 3, 7))
        assertEquals(Ux09DiskIntentJournal.Recovery.NEEDS_MANUAL_RECONCILIATION,
            journal.recover(journal.read(a, op), oldHash, a, 3, 7))
    }

    @Test fun accountOrRestoreChangeBlocksRecoveryEvenIfPayloadHashMatches() {
        val j = Ux09DiskIntentJournal(tmp.root)
        j.prepare(intent())
        val ready = j.markLocallySaved(intent(), newHash)
        assertEquals(Ux09DiskIntentJournal.Recovery.WRONG_ACCOUNT_OR_RESTORE,
            j.recover(ready, newHash, b, 3, 7))
        assertEquals(Ux09DiskIntentJournal.Recovery.WRONG_ACCOUNT_OR_RESTORE,
            j.recover(ready, newHash, a, 4, 7))
        assertEquals(Ux09DiskIntentJournal.Recovery.WRONG_ACCOUNT_OR_RESTORE,
            j.recover(ready, newHash, a, 3, 8))
    }

    @Test fun unrelatedLocalChangeCannotBeSilentlyOverwrittenOrReplayed() {
        val j = Ux09DiskIntentJournal(tmp.root)
        val stored = j.prepare(intent())
        val unknown = Ux09Contract.sha256("another local edit".toByteArray())
        assertEquals(Ux09DiskIntentJournal.Recovery.NEEDS_MANUAL_RECONCILIATION,
            j.recover(stored, unknown, a, 3, 7))
    }

    @Test fun idempotentPrepareButChangedOperationUuidPayloadIsRejected() {
        val j = Ux09DiskIntentJournal(tmp.root)
        assertEquals(j.prepare(intent()), j.prepare(intent()))
        assertThrows(IllegalArgumentException::class.java) {
            j.prepare(intent(payload = """{"title":"malicious reuse"}"""))
        }
        assertEquals(1, j.pending(a).size)
        assertTrue(j.pending(b).isEmpty())
    }

    @Test fun corruptJournalIsNotDeletedOrUploaded() {
        val j = Ux09DiskIntentJournal(tmp.root)
        j.prepare(intent())
        val path = File(File(tmp.root, a), "$op.intent")
        val bytes = path.readText().replace("Synthetic", "Tampered")
        path.writeText(bytes)
        assertThrows(IllegalArgumentException::class.java) { j.read(a, op) }
        assertTrue(path.exists())
    }

    @Test fun interruptedTempFileStopsEnumerationWithoutDeletingIt() {
        val j = Ux09DiskIntentJournal(tmp.root)
        j.prepare(intent())
        val unfinished = File(File(tmp.root, a), "broken.intent.tmp")
        unfinished.writeText("truncated")
        assertThrows(IllegalArgumentException::class.java) { j.pending(a) }
        assertTrue(unfinished.exists())
    }

    @Test fun noOpMutationRejectedAndNoFileCreated() {
        val j = Ux09DiskIntentJournal(tmp.root)
        assertThrows(IllegalArgumentException::class.java) {
            j.prepare(intent(after = oldHash))
        }
        assertTrue(j.pending(a).isEmpty())
    }

    @Test fun twoAccountsHaveSeparateJournalEntries() {
        val j = Ux09DiskIntentJournal(tmp.root)
        val a1 = intent()
        val b1 = intent(accountId=b)
        j.prepare(a1)
        j.prepare(b1)
        assertEquals(a1, j.pending(a).single().intent)
        assertEquals(b1, j.pending(b).single().intent)
    }
}

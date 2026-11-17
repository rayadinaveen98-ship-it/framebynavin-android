package com.framebynavin.app.cloud.ux09rpc

import com.framebynavin.app.cloud.ux09.Ux09Contract
import com.framebynavin.app.cloud.ux09.Ux09Domain
import com.framebynavin.app.cloud.ux09.Ux09Intent
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic, no credentials, no HTTP, no app runtime, no hosted records. */
class Ux09LabRpcContractTest {
    private val owner = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val entity = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val operation = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    private val digest = Ux09Contract.sha256("local".toByteArray())

    private fun sent(
        domain: Ux09Domain = Ux09Domain.PROJECT,
        deleted: Boolean = false,
    ): Ux09Intent = Ux09Intent(
        owner, 2L, 3L, domain, entity, operation, 0L, digest,
        Ux09Contract.sha256("new".toByteArray()),
        if (deleted) "{}" else Ux09Contract.canonicalPayload("""{"title":"Synthetic Project"}"""),
        deleted,
    )

    private fun applied(
        revision: Long = 1L,
        sequence: Long = 1L,
        domain: String = "creator_project",
        entityId: String = entity,
        deleted: Boolean = false,
    ): String = JSONObject()
        .put("status", "applied")
        .put("sequence", sequence).put("revision", revision)
        .put("domain", domain).put("entity_id", entityId)
        .put("deleted", deleted).toString()

    private fun conflict(current: Long = 5L, deleted: Boolean = true): String =
        JSONObject().put("status", "conflict")
            .put("current_revision", current).put("deleted", deleted)
            .put("domain", "creator_project").put("entity_id", entity).toString()

    private fun change(
        seq: Long = 1L,
        revision: Long = 1L,
        domain: String = "creator_project",
        deleted: Boolean = false,
        payload: JSONObject = JSONObject().put("title", "Synthetic"),
    ): JSONObject = JSONObject()
        .put("sequence", seq).put("domain", domain)
        .put("entity_id", entity).put("revision", revision)
        .put("payload", if (deleted) JSONObject() else payload)
        .put("deleted", deleted)

    private fun page(
        records: JSONArray,
        cursor: Long,
        more: Boolean = false,
    ): String = JSONObject()
        .put("changes", records).put("next_cursor", cursor)
        .put("has_more", more).toString()

    @Test fun originalProductionEndpointIsAlwaysRejected() {
        Ux09LabRpcContract.requireLabEndpoint("https://vasleafrmsovdbdlrcsw.supabase.co")
        Ux09LabRpcContract.requireLabEndpoint("https://vasleafrmsovdbdlrcsw.supabase.co/")
        for (notLab in listOf(
            "", "https://kukkqgpzxnfanynbddiw.supabase.co",
            "https://vasleafrmsovdbdlrcsw.supabase.co.evil.test",
            "http://vasleafrmsovdbdlrcsw.supabase.co",
            "https://localhost:54321",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                Ux09LabRpcContract.requireLabEndpoint(notLab)
            }
        }
    }

    @Test fun validAppliedResponseLinksToExactLocalOperation() {
        val out = Ux09LabRpcContract.parseApply(applied(), sent())
        val applied = out as Ux09LabRpcContract.Apply.Applied
        assertEquals(operation, applied.operationId)
        assertEquals(1L, applied.revision)
        assertEquals(1L, applied.sequence)
        assertEquals(entity, applied.entityId)
        assertFalse(applied.tombstone)
    }

    @Test fun validConflictRetainsRemoteRevisionAndTombstone() {
        val c = Ux09LabRpcContract.parseApply(conflict(), sent()) as
            Ux09LabRpcContract.Apply.Conflict
        assertEquals(operation, c.operationId)
        assertEquals(5L, c.currentRevision)
        assertTrue(c.remoteDeleted)
    }

    @Test fun wrongEntityOrDomainCannotAcknowledgeLocalWrite() {
        for (bad in listOf(
            applied(entityId = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
            applied(domain = "creator_idea"),
        )) assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parseApply(bad, sent())
        }
    }

    @Test fun staleOrIncorrectRevisionAndSequenceAreRejected() {
        for (bad in listOf(
            applied(revision = 2L),
            applied(sequence = 0L),
            applied(deleted = true),
        )) assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parseApply(bad, sent())
        }
    }

    @Test fun deletedApplyMustMatchRequestedTombstone() {
        val r = Ux09LabRpcContract.parseApply(
            applied(deleted = true), sent(deleted = true),
        ) as Ux09LabRpcContract.Apply.Applied
        assertTrue(r.tombstone)
    }

    @Test fun conflictNegativeRevisionAndAdditionalOwnerFieldsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parseApply(conflict(current = -1L), sent())
        }
        val forged = JSONObject(applied()).put("user_id", owner)
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parseApply(forged.toString(), sent())
        }
    }

    @Test fun emptyPullPreservesCursorAndAllowsNewerDataLater() {
        val p = Ux09LabRpcContract.parsePull(page(JSONArray(), 7), 7, 50)
        assertTrue(p.changes.isEmpty())
        assertEquals(7L, p.nextCursor)
        assertFalse(p.hasMore)
    }

    @Test fun realDeltaPageRequiresIncreasingSequencesAndLastCursor() {
        val records = JSONArray().put(change(seq = 3L)).put(change(seq = 5L,
            revision = 2L, domain = "creator_idea"))
        val p = Ux09LabRpcContract.parsePull(page(records, 5, true), 2, 2)
        assertEquals(2, p.changes.size)
        assertEquals(3L, p.changes.first().sequence)
        assertEquals(5L, p.nextCursor)
        assertTrue(p.hasMore)
        assertEquals(Ux09Domain.IDEA, p.changes[1].domain)
    }

    @Test fun repeatedOrNonmonotonicDeltaSequencesAreRejected() {
        for (records in listOf(
            JSONArray().put(change(seq = 2L)).put(change(seq = 2L)),
            JSONArray().put(change(seq = 4L)).put(change(seq = 3L)),
            JSONArray().put(change(seq = 1L)),
        )) assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(page(records, 4), 1L, 50)
        }
    }

    @Test fun invalidCursorAndEmptyMorePagesCannotAdvanceSync() {
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(page(JSONArray().put(change(seq = 2L)), 10), 1L, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(page(JSONArray(), 1, true), 1L, 10)
        }
    }

    @Test fun oversizedPageAndInvalidRequestBoundsFailClosed() {
        val records = JSONArray().put(change(seq = 1L)).put(change(seq = 2L))
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(page(records, 2), 0L, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.pullBody(-1L, 50)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.pullBody(0L, 101)
        }
    }

    @Test fun localFilePathAndReservedOwnerDataAreNotAcceptedFromCloud() {
        for (payload in listOf(
            JSONObject().put("workspace", JSONObject().put("location", "content://media/1")),
            JSONObject().put("access_token", "bad"),
            JSONObject().put("filePath", "/data/user/0/private"),
        )) assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(
                page(JSONArray().put(change(payload = payload)), 1), 0, 10)
        }
    }

    @Test fun unknownExtraResponseFieldsOrDomainAreRejected() {
        val extra = JSONObject(page(JSONArray(), 0)).put("auth_user", owner)
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(extra.toString(), 0, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(
                page(JSONArray().put(change(domain = "personal")), 1), 0, 10)
        }
    }

    @Test fun remoteTombstoneNeedsEmptyPayloadAndNeverRestoresRecord() {
        val result = Ux09LabRpcContract.parsePull(
            page(JSONArray().put(change(deleted = true)), 1), 0, 10)
        assertTrue(result.changes.single().deleted)
        assertEquals("{}", result.changes.single().canonicalPayload)
        val forged = change(deleted = true).put("payload", JSONObject().put("title", "do not restore"))
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRpcContract.parsePull(
                page(JSONArray().put(forged), 1), 0, 10)
        }
    }
}

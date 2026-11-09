package com.framebynavin.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CreatorFocusSessionCodecTest {
    private fun setup() = CreatorFocusSessionPolicy.create(
        projectId = "film-123", stageId = "script",
        targetMinutes = 25, atWallMillis = 1_700_000_000_000L,
        id = "focus-123",
    )

    @Test fun roundTripRetainsRunningAnchorAndActiveHistorySeparately() {
        val active = CreatorFocusSessionPolicy.start(setup(), elapsedMillis = 1000L, bootCount = 9)
        val ledger = CreatorFocusLedger(active = active)
        assertEquals(ledger, CreatorFocusSessionCodec.decode(CreatorFocusSessionCodec.encode(ledger)))
        assertEquals(CreatorFocusState.RUNNING,
            CreatorFocusSessionCodec.decode(CreatorFocusSessionCodec.encode(ledger)).active?.state)
    }

    @Test fun confirmedFinishPreservesActualMinutesWithoutChangingProjectOrStage() {
        val session = CreatorFocusSessionPolicy.start(setup(), elapsedMillis = 4_000L, bootCount = 1)
        val completed = CreatorFocusSessionPolicy.finish(
            session, elapsedMillis = 34_000L, bootCount = 1, atWallMillis = 1_700_000_034_000L)
        val ledger = CreatorFocusLedger(history = listOf(completed))
        val decoded = CreatorFocusSessionCodec.decode(CreatorFocusSessionCodec.encode(ledger))
        assertEquals(30_000L, decoded.history.single().accruedActiveMillis)
        assertEquals("film-123", decoded.history.single().projectId)
        assertEquals("script", decoded.history.single().stageId)
    }

    @Test fun rejectsDuplicateActiveAndHistoryIdentity() {
        val running = CreatorFocusSessionPolicy.start(setup(), 100L, 1)
        val finished = CreatorFocusSessionPolicy.finish(running, 200L, 1, 1_700_000_000_200L)
        try {
            CreatorFocusLedger(active = running, history = listOf(finished)).checked()
            fail("Duplicate session IDs must be rejected")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun corruptJsonDoesNotSilentlyTurnIntoEmptyHistory() {
        val valid = CreatorFocusSessionCodec.encode(
            CreatorFocusLedger(active = CreatorFocusSessionPolicy.start(setup(), 100L, 5)))
        val corrupted = JSONObject(valid).put("version", 999).toString()
        try {
            CreatorFocusSessionCodec.decode(corrupted)
            fail("Unsupported archive version must fail")
        } catch (_: IllegalArgumentException) {}
        try {
            CreatorFocusSessionCodec.decode("not-json")
            fail("Corrupted archive must fail")
        } catch (_: Exception) {}
    }

    @Test fun rejectsMissingOrUnrecognizedSessionFieldsWithoutDataLoss() {
        val valid = CreatorFocusSessionCodec.encode(CreatorFocusLedger(active = setup()))
        val root = JSONObject(valid)
        root.getJSONObject("active").remove("projectId")
        try {
            CreatorFocusSessionCodec.decode(root.toString())
            fail("Missing identity must not become anonymous work")
        } catch (_: Exception) {}
        root.getJSONObject("active").put("projectId", "film-123")
        root.getJSONObject("active").put("unrecognized", "bad")
        try {
            CreatorFocusSessionCodec.decode(root.toString())
            fail("Unknown archive section must not be silently ignored")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun cappedHistoryAndTerminalStatesOnly() {
        val done = CreatorFocusSessionPolicy.finish(
            setup(), 0L, 0, 1_700_000_001_000L)
        val history = (0..99).map { done.copy(id = "session-$it") }
        assertEquals(100, CreatorFocusSessionCodec.decode(
            CreatorFocusSessionCodec.encode(CreatorFocusLedger(history = history))).history.size)
        try {
            CreatorFocusLedger(history = history + done.copy(id = "excess")).checked()
            fail("History count beyond 100 must be bounded before persistence")
        } catch (_: IllegalArgumentException) {}
        try {
            CreatorFocusLedger(history = listOf(setup())).checked()
            fail("Nonterminal record is never history")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun legacyEmptyFocusArchiveIsVersionedButContainsNoFabricatedSession() {
        val empty = CreatorFocusSessionCodec.decode(
            CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
        assertEquals(null, empty.active)
        assertTrue(empty.history.isEmpty())
    }
}

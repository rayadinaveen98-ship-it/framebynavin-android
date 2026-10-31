package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** No production endpoint calls. Parser and launch switch tested in isolation. */
@RunWith(AndroidJUnit4::class)
class V270AccountDeletionSafetyTest {
    private val receipt = "11111111-2222-4333-8444-555555555555"

    private fun rejected(block: () -> Unit) {
        var denied = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            denied = true
        } catch (_: org.json.JSONException) {
            denied = true
        }
        assertTrue("Invalid deletion response must fail closed", denied)
    }

    @Test fun productionDeletionCannotAccidentallyLaunchInPreview() {
        assertFalse(BacklotDeletionReleaseGate.LIVE_REQUESTS_ENABLED)
        assertEquals("DELETE MY BACKLOT ACCOUNT", BacklotDeletionReleaseGate.CONFIRMATION)
    }

    @Test fun onlyExactOwnerReceiptAccepted() {
        val result = BacklotDeletionResponse.request(
            """{"request_id":"$receipt","status":"pending","stage":"fenced"}"""
        )
        assertEquals(receipt, result.requestId)
        assertEquals("pending", result.status)
        assertEquals("fenced", result.stage)
    }

    @Test fun noRequestYetIsNotMistakenForSuccess() {
        assertNull(BacklotDeletionResponse.status("""{"status":"none"}"""))
    }

    @Test fun missingReceiptOrUnapprovedStateRejected() {
        rejected { BacklotDeletionResponse.request("""{"status":"complete","stage":"complete"}""") }
        rejected { BacklotDeletionResponse.request("""{"request_id":"$receipt","status":"deleted","stage":"complete"}""") }
        rejected { BacklotDeletionResponse.request("""{"request_id":"$receipt","status":"complete","stage":"unexpected"}""") }
        rejected { BacklotDeletionResponse.status("""{"status":"none","request_id":"$receipt"}""") }
        rejected { BacklotDeletionResponse.request("""{"request_id":"other","status":"pending","stage":"fenced"}""") }
    }

    @Test fun serverCannotAddUnexpectedFieldsOrImpersonateCompletion() {
        rejected {
            BacklotDeletionResponse.request(
                """{"request_id":"$receipt","status":"complete","stage":"complete","user_id":"other"}"""
            )
        }
        val complete = BacklotDeletionResponse.status(
            """{"request_id":"$receipt","status":"complete","stage":"complete"}"""
        )
        assertEquals("complete", complete?.status)
    }
}

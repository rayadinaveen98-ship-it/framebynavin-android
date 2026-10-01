package com.framebynavin.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorCloudFailurePolicyTest {
    @Test
    fun `records failure only for the account that started the operation`() {
        assertTrue(
            CreatorCloudFailurePolicy.shouldRecord(
                expectedUserId = "user-a",
                currentUserId = "user-a",
                error = RuntimeException("network down"),
            ),
        )
    }

    @Test
    fun `does not attach old operation failure to newly selected account`() {
        assertFalse(
            CreatorCloudFailurePolicy.shouldRecord(
                expectedUserId = "user-a",
                currentUserId = "user-b",
                error = RuntimeException("network down"),
            ),
        )
    }

    @Test
    fun `does not persist account changed fence as a sync error`() {
        assertFalse(
            CreatorCloudFailurePolicy.shouldRecord(
                expectedUserId = "user-a",
                currentUserId = "user-a",
                error = CloudAccountChanged(),
            ),
        )
    }

    @Test
    fun `does not record failure after sign out`() {
        assertFalse(
            CreatorCloudFailurePolicy.shouldRecord(
                expectedUserId = "user-a",
                currentUserId = null,
                error = RuntimeException("request finished late"),
            ),
        )
    }
}

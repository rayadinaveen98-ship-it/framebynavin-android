package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class V179FollowUpClaimInstrumentedTest {
    @Test fun repeatedWorkerForSameTokenCannotPostSecondTime() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val taskId = "v179-claims-${UUID.randomUUID()}"
        val first = V179FollowUpClaimStore(context)
        try {
            assertTrue(first.claim(taskId, "valid-token", 1))
            assertFalse(V179FollowUpClaimStore(context).claim(taskId, "valid-token", 1))
            assertTrue(V179FollowUpClaimStore(context).claim(taskId, "valid-token", 2))
            assertFalse(first.claim(taskId, "valid-token", 2))
            assertFalse(first.claim(taskId, "valid-token", 3))
            // New occurrence starts its own budget without reviving old workers.
            assertTrue(first.claim(taskId, "new-token", 1))
            assertFalse(first.claim(taskId, "", 1))
        } finally {
            first.clearTask(taskId)
        }
    }
}

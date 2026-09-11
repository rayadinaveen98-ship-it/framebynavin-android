package com.framebynavin.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudLoginAlpha11Test {
    @Test
    fun missingLifecycleRpc_isClassifiedAsSafetyServiceUnavailable() {
        assertTrue(isCloudSafetyServiceUnavailable(CloudHttpException(404, "not found")))
        assertTrue(isCloudSafetyServiceUnavailable(CloudHttpException(400, "PGRST202 function missing")))
    }

    @Test
    fun authAndOtherServerErrors_areNotMisclassified() {
        assertFalse(isCloudSafetyServiceUnavailable(CloudHttpException(401, "unauthorized")))
        assertFalse(isCloudSafetyServiceUnavailable(CloudHttpException(500, "server error")))
        assertFalse(isCloudSafetyServiceUnavailable(IllegalStateException("offline")))
    }
}

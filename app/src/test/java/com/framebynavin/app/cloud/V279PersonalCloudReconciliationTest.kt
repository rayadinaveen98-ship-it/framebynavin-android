package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class V279PersonalCloudReconciliationTest {
    private val shaA = "a".repeat(64)
    private val shaB = "b".repeat(64)
    private val shaC = "c".repeat(64)

    @Test fun newDeviceRestoresButFirstDeviceUploads() {
        assertEquals(CreatorCloudReconciliationAction.UPLOAD_LOCAL,
            PersonalCloudReconciliation.decide(shaA, null, null, false))
        assertEquals(CreatorCloudReconciliationAction.NO_CHANGE,
            PersonalCloudReconciliation.decide(shaA, null, null, true))
        assertEquals(CreatorCloudReconciliationAction.RESTORE_CLOUD,
            PersonalCloudReconciliation.decide(shaA, shaB, null, true))
    }
    @Test fun twoPopulatedDevicesRequireExplicitChoice() {
        assertEquals(CreatorCloudReconciliationAction.REQUIRE_CHOICE,
            PersonalCloudReconciliation.decide(shaA, shaB, null, false))
        assertEquals(CreatorCloudReconciliationAction.REQUIRE_CHOICE,
            PersonalCloudReconciliation.decide(shaC, shaB, shaA, false))
    }
    @Test fun lastSyncedRevisionDeterminesSafeDirection() {
        assertEquals(CreatorCloudReconciliationAction.NO_CHANGE,
            PersonalCloudReconciliation.decide(shaA, shaA, null, false))
        assertEquals(CreatorCloudReconciliationAction.UPLOAD_LOCAL,
            PersonalCloudReconciliation.decide(shaB, shaA, shaA, false))
        assertEquals(CreatorCloudReconciliationAction.RESTORE_CLOUD,
            PersonalCloudReconciliation.decide(shaA, shaB, shaA, false))
    }
}
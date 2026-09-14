package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorCloudReconciliationPolicyTest {
    private val local = "a".repeat(64)
    private val cloud = "b".repeat(64)
    private val prior = "c".repeat(64)

    @Test
    fun `first backup uploads local when cloud is empty`() {
        assertEquals(
            CreatorCloudReconciliationAction.UPLOAD_LOCAL,
            CreatorCloudReconciliationPolicy.decide(local, null, null, localIsFresh = false),
        )
    }

    @Test
    fun `matching local and cloud is no change`() {
        assertEquals(
            CreatorCloudReconciliationAction.NO_CHANGE,
            CreatorCloudReconciliationPolicy.decide(local, local, prior, localIsFresh = false),
        )
    }

    @Test
    fun `fresh phone restores existing cloud on first reconciliation`() {
        assertEquals(
            CreatorCloudReconciliationAction.RESTORE_CLOUD,
            CreatorCloudReconciliationPolicy.decide(local, cloud, null, localIsFresh = true),
        )
    }

    @Test
    fun `meaningful local and cloud require choice on first reconciliation`() {
        assertEquals(
            CreatorCloudReconciliationAction.REQUIRE_CHOICE,
            CreatorCloudReconciliationPolicy.decide(local, cloud, null, localIsFresh = false),
        )
    }

    @Test
    fun `local only edit uploads when cloud still equals last synced`() {
        assertEquals(
            CreatorCloudReconciliationAction.UPLOAD_LOCAL,
            CreatorCloudReconciliationPolicy.decide(local, cloud, cloud, localIsFresh = false),
        )
    }

    @Test
    fun `cloud only edit restores when local still equals last synced`() {
        assertEquals(
            CreatorCloudReconciliationAction.RESTORE_CLOUD,
            CreatorCloudReconciliationPolicy.decide(local, cloud, local, localIsFresh = false),
        )
    }

    @Test
    fun `two device divergence never chooses a winner automatically`() {
        assertEquals(
            CreatorCloudReconciliationAction.REQUIRE_CHOICE,
            CreatorCloudReconciliationPolicy.decide(local, cloud, prior, localIsFresh = false),
        )
    }
}

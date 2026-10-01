package com.backlot.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals

class BacklotReconciliationPolicyTest {
    private val local = "a".repeat(64)
    private val cloud = "b".repeat(64)
    private val prior = "c".repeat(64)

    @Test
    fun freshInstallRestoresExistingCloud() {
        assertEquals(
            BacklotReconciliationAction.RESTORE_CLOUD,
            BacklotReconciliationPolicy.decide(local, cloud, null, localIsFresh = true),
        )
    }

    @Test
    fun localOnlyChangeUploadsWhenCloudStillMatchesPriorSync() {
        assertEquals(
            BacklotReconciliationAction.UPLOAD_LOCAL,
            BacklotReconciliationPolicy.decide(local, cloud, cloud, localIsFresh = false),
        )
    }

    @Test
    fun cloudOnlyChangeRestoresWhenLocalStillMatchesPriorSync() {
        assertEquals(
            BacklotReconciliationAction.RESTORE_CLOUD,
            BacklotReconciliationPolicy.decide(local, cloud, local, localIsFresh = false),
        )
    }

    @Test
    fun divergentDevicesRequireHumanChoice() {
        assertEquals(
            BacklotReconciliationAction.REQUIRE_CHOICE,
            BacklotReconciliationPolicy.decide(local, cloud, prior, localIsFresh = false),
        )
    }
}

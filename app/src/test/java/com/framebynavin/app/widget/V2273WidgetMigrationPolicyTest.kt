package com.framebynavin.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2273WidgetMigrationPolicyTest {
    @Test fun oldPinnedWithoutNewWidgetWarnsAgainstPrematureRemoval() {
        val result = V2273WidgetMigrationPolicy.migrationMessage(0, 4)
        assertTrue(result.contains("Add a new widget before removing"))
    }

    @Test fun bothGenerationsRequiresOwnerHomeScreenRemoval() {
        val result = V2273WidgetMigrationPolicy.migrationMessage(2, 4)
        assertTrue(result.contains("long-press each old widget"))
    }

    @Test fun noLegacyWidgetsIsSafeButNeverTriggersComponentDisabling() {
        assertTrue(V2273WidgetMigrationPolicy.migrationMessage(3, 0).contains("No older widgets"))
        assertTrue(V2273WidgetMigrationPolicy.migrationMessage(0, 0).contains("No older widgets"))
    }

    @Test fun pinRequestMeansLauncherApprovalIsStillNeeded() {
        assertTrue(V2273WidgetMigrationPolicy.pinMessage(true).contains("Approve it"))
        assertTrue(V2273WidgetMigrationPolicy.pinMessage(false).contains("Long-press Home"))
    }

    @Test fun inventoryMakesNoUnverifiedClaimsAndTotalsAllPinnedProviders() {
        val counts = V2273WidgetInventory(
            newPinned = mapOf(
                V2273WidgetKind.QUICK_CAPTURE to 1,
                V2273WidgetKind.TODAY_FOCUS to 2,
                V2273WidgetKind.MY_DAY to 0,
            ),
            legacyPinned = mapOf("Compact" to 2, "Current Project" to 1, "Insights" to 0),
        )
        assertEquals(3, counts.newTotal)
        assertEquals(3, counts.oldTotal)
        assertTrue(counts.hasLegacyPinned)
        assertFalse(counts.legacyPinned["Insights"]!! > 0)
    }
}

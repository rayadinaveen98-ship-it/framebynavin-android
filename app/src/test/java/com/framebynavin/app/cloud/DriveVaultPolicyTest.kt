package com.framebynavin.app.cloud

import org.junit.Assert.*
import org.junit.Test

class DriveVaultPolicyTest {
    private fun point(id: String, at: Long, projects: Int, ideas: Int) = DriveVaultRestorePoint(
        fileId=id, name="$id.fbnbackup", capturedAtMillis=at, appVersion="test",
        projectCount=projects, ideaCount=ideas, weeklySlotCount=0, activeReminderCount=0,
        sha256="abc", sizeBytes=100,
    )

    @Test fun `recommended recovery skips newer empty snapshot`() {
        val result = DriveVaultPolicy.recommended(listOf(point("empty", 20, 0, 0), point("work", 10, 4, 2)))
        assertEquals("work", result?.fileId)
    }

    @Test fun `empty local snapshot is blocked when vault has creator work`() {
        assertTrue(DriveVaultPolicy.shouldBlockEmptySnapshot(0, 0, listOf(point("work", 10, 1, 0))))
        assertFalse(DriveVaultPolicy.shouldBlockEmptySnapshot(1, 0, listOf(point("work", 10, 1, 0))))
    }

    @Test fun `empty vault can accept initial creator setup snapshot`() {
        assertFalse(DriveVaultPolicy.shouldBlockEmptySnapshot(0, 0, emptyList()))
    }
}

package com.framebynavin.app.cloud

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DriveVaultTokenMemoryTest {
    @Before fun setUp() = DriveVaultTokenMemory.clear()
    @After fun tearDown() = DriveVaultTokenMemory.clear()

    @Test fun `token is available only to the account that authorized Drive`() {
        DriveVaultTokenMemory.put("Creator@Example.com", "drive-token-a")

        assertEquals("drive-token-a", DriveVaultTokenMemory.get("creator@example.com"))
        assertNull(DriveVaultTokenMemory.get("other@example.com"))
    }

    @Test fun `clear removes process Drive authorization`() {
        DriveVaultTokenMemory.put("creator@example.com", "drive-token-a")
        DriveVaultTokenMemory.clear()

        assertNull(DriveVaultTokenMemory.get("creator@example.com"))
    }

    @Test fun `new account authorization replaces previous account token`() {
        DriveVaultTokenMemory.put("first@example.com", "first-token")
        DriveVaultTokenMemory.put("second@example.com", "second-token")

        assertNull(DriveVaultTokenMemory.get("first@example.com"))
        assertEquals("second-token", DriveVaultTokenMemory.get("second@example.com"))
    }
}

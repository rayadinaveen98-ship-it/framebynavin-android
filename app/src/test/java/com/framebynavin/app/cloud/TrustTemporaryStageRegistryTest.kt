package com.framebynavin.app.cloud

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Pure JVM proof that a sweep cannot confuse active work with crash debris. */
class TrustTemporaryStageRegistryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun activeOwnerPlaintextDirectoryMustBePreservedUntilClosed() {
        val directory = temporary.newFolder("trust-t1c2-owner-bytes-active")
        TrustTemporaryStageRegistry.activate(directory)
        try {
            assertTrue(TrustTemporaryStageRegistry.isActive(directory))
            assertThrows(IllegalStateException::class.java) {
                TrustTemporaryStageRegistry.activate(directory)
            }
            assertTrue(directory.isDirectory)
        } finally {
            TrustTemporaryStageRegistry.deactivate(directory)
        }
        assertFalse(TrustTemporaryStageRegistry.isActive(directory))
    }

    @Test fun canonicalAliasesCannotBypassActiveOwnerDirectoryIdentity() {
        val directory = temporary.newFolder("backlot-trust-check-active")
        val alias = File(directory.parentFile, "./" + directory.name)
        TrustTemporaryStageRegistry.activate(directory)
        try {
            assertTrue(TrustTemporaryStageRegistry.isActive(alias))
        } finally {
            TrustTemporaryStageRegistry.deactivate(alias)
        }
        assertFalse(TrustTemporaryStageRegistry.isActive(directory))
    }
}

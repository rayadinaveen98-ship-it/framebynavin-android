package com.framebynavin.app.personal

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V300PrivatePhotoInventoryPolicyTest {
    @Test fun completePrivatePhotoInventoryIsAllowed() {
        V300PrivatePhotoInventoryPolicy.requireExactFiles(
            setOf("/private/space/photo-1.jpg"),
            setOf("/private/space/photo-1.jpg"),
        )
    }

    @Test fun missingPrivatePhotoBlocksExportWithoutLeakingItsPath() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            V300PrivatePhotoInventoryPolicy.requireExactFiles(
                setOf("/very-private/space/secret-photo.jpg"), emptySet(),
            )
        }
        assertTrue(failure.message.orEmpty().contains("inventory"))
        assertTrue(!failure.message.orEmpty().contains("secret-photo"))
    }

    @Test fun untrackedInterruptedPhotoBlocksExport() {
        assertThrows(IllegalArgumentException::class.java) {
            V300PrivatePhotoInventoryPolicy.requireExactFiles(
                setOf("/private/space/known.jpg"),
                setOf("/private/space/known.jpg", "/private/space/abandoned.partial"),
            )
        }
    }
}

package com.framebynavin.app.data

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PortableVoiceMediaIntegrityTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `identical installed voice copy passes integrity verification`() {
        val staged = temporaryFolder.newFile("staged.m4a").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }
        val installed = temporaryFolder.newFile("installed.m4a").apply {
            writeBytes(staged.readBytes())
        }

        PortableVoiceMediaIntegrity.verifyInstalledCopy(staged, installed)
    }

    @Test
    fun `same-size corrupted installed voice copy is rejected`() {
        val staged = temporaryFolder.newFile("staged-corrupt.m4a").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }
        val installed = temporaryFolder.newFile("installed-corrupt.m4a").apply {
            writeBytes(byteArrayOf(6, 5, 4, 3, 2, 1))
        }

        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceMediaIntegrity.verifyInstalledCopy(staged, installed)
        }
    }

    @Test
    fun `truncated installed voice copy is rejected`() {
        val staged = temporaryFolder.newFile("staged-truncated.m4a").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }
        val installed = temporaryFolder.newFile("installed-truncated.m4a").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }

        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceMediaIntegrity.verifyInstalledCopy(staged, installed)
        }
    }
}

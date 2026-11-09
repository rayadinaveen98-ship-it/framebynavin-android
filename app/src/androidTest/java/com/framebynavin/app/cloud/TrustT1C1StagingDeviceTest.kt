package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable CI Android 15 owner-independent fixture: all IO is under cacheDir.
 * No real Creator/Personal store reads, network, permission requests or restore.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1C1StagingDeviceTest {
    @Test fun encryptAndVerifyFakeDataOnDeviceWithoutTouchingLiveOwnerState() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val directory = File(context.cacheDir,
            "ci-trust-t1c1-staging-" + System.nanoTime()).apply { mkdirs() }
        val passphrase = "ci-device-strong-staging-passphrase".toCharArray()
        try {
            val owner = "disposable-android-ci-user"
            val personal = """
                {"completedAt":"2026-11-09T07:00:00Z","title":"మార్నింగ్ వాక్"}
            """.trimIndent().toByteArray(Charsets.UTF_8)
            val fakePhoto = ByteArray(90_024) { (it % 197).toByte() }
            val parts = listOf(
                fakeSource("personal/challenge-receipts", personal, 1),
                fakeSource("personal/space-jpeg-fixture", fakePhoto, 1),
            )
            val stagedZip = File(directory, "temporary-owner-staging.bundle")
            stagedZip.outputStream().use {
                TrustEncryptedStagingCodec.write(owner, passphrase, parts, it)
            }
            assertTrue(stagedZip.length() > 1000L)
            val raw = stagedZip.readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(raw.contains(owner))
            assertFalse(raw.contains("completedAt"))
            assertFalse(raw.contains("personal/challenge-receipts"))

            stagedZip.inputStream().use { encrypted ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    owner, passphrase, encrypted,
                    File(directory, "verified-owner-disposable"),
                ).use { result ->
                    assertEquals(2, result.parts.size)
                    assertArrayEquals(personal,
                        result.parts.single { p ->
                            p.id == "personal/challenge-receipts"
                        }.file.readBytes())
                    assertArrayEquals(fakePhoto,
                        result.parts.single { p ->
                            p.id == "personal/space-jpeg-fixture"
                        }.file.readBytes())
                    assertFalse(result.canUploadAsUniversalBackup)
                    assertFalse(result.canClaimCompleteBackup)
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                stagedZip.inputStream().use { encrypted ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        "other-disposable-owner", passphrase, encrypted,
                        File(directory, "wrong-account-temp"),
                    )
                }
            }
        } finally {
            passphrase.fill('\u0000')
            check(directory.deleteRecursively() || !directory.exists())
        }
    }

    private fun fakeSource(id: String, bytes: ByteArray, count: Int):
        TrustStagingSource {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        return TrustStagingSource(id, count, bytes.size.toLong(), digest) {
            ByteArrayInputStream(bytes)
        }
    }
}

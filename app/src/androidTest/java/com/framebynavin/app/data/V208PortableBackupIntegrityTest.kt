package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V208PortableBackupIntegrityTest {
    @Test fun freshPortableExportPreviewsAllMediaBeforeClaimingValid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backup = PortableCreatorBackupManager(context)
        val bytes = ByteArrayOutputStream()
        backup.writeBackup(bytes)
        val preview = backup.validate(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(CreatorBackupManager.SCHEMA_VERSION, preview.schemaVersion)
        assertTrue(bytes.size() > 80)
    }

    @Test fun orphanVoiceArchiveDoesNotPassPreviewWhenJsonIsValid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = CreatorBackupManager(context).createBackup()
        val extra = File.createTempFile("voice-idea-orphan", ".m4a", context.cacheDir)
        try {
            extra.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            val bytes = ByteArrayOutputStream().also {
                PortableBackupArchive.write(raw, listOf(PortableBackupMedia("not-a-real-idea-id", extra)), it)
            }.toByteArray()
            // Pre-V208 preview checked only backlot.json, silently ignoring
            // the unexpected media. Full-archive validation must fail closed.
            assertThrows(IllegalArgumentException::class.java) {
                PortableCreatorBackupManager(context).validate(ByteArrayInputStream(bytes))
            }
        } finally {
            extra.delete()
        }
    }

    @Test fun tamperedMediaChecksumFailsBeforeAnyRestore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = CreatorBackupManager(context).createBackup()
        val extra = File.createTempFile("voice-idea-tamper", ".m4a", context.cacheDir)
        try {
            extra.writeBytes(byteArrayOf(10, 20, 30, 40, 50))
            val original = ByteArrayOutputStream().also {
                PortableBackupArchive.write(raw, listOf(PortableBackupMedia("not-a-real-idea-id", extra)), it)
            }.toByteArray()
            val corrupted = ByteArrayOutputStream()
            ZipInputStream(ByteArrayInputStream(original)).use { input ->
                ZipOutputStream(corrupted).use { output ->
                    while (true) {
                        val entry = input.nextEntry ?: break
                        val payload = input.readBytes()
                        output.putNextEntry(ZipEntry(entry.name))
                        if (entry.name.startsWith("voice_ideas/") && payload.isNotEmpty())
                            payload[0] = (payload[0].toInt() xor 0xff).toByte()
                        output.write(payload)
                        output.closeEntry()
                        input.closeEntry()
                    }
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                PortableCreatorBackupManager(context).validate(ByteArrayInputStream(corrupted.toByteArray()))
            }
        } finally {
            extra.delete()
        }
    }
}

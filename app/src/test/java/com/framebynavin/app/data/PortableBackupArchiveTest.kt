package com.framebynavin.app.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PortableBackupArchiveTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `portable archive round trips json and voice bytes`() {
        val recording = temporaryFolder.newFile("voice.m4a").apply {
            writeBytes(byteArrayOf(1, 4, 9, 16, 25, 36))
        }
        val output = ByteArrayOutputStream()

        PortableBackupArchive.write(
            backupJson = "{\"format\":\"FrameByNavinBackup\"}",
            media = listOf(PortableBackupMedia("idea-voice-1", recording)),
            output = output,
        )

        val staged = PortableBackupArchive.stage(
            ByteArrayInputStream(output.toByteArray()),
            temporaryFolder.newFolder("stage-root"),
        )
        try {
            assertTrue(staged.isArchive)
            assertEquals("{\"format\":\"FrameByNavinBackup\"}", staged.backupJson)
            assertEquals(setOf("idea-voice-1"), staged.mediaByIdeaId.keys)
            assertArrayEquals(recording.readBytes(), staged.mediaByIdeaId.getValue("idea-voice-1").readBytes())
        } finally {
            val stagedFile = staged.mediaByIdeaId.getValue("idea-voice-1")
            staged.cleanup()
            assertFalse(stagedFile.exists())
        }
    }

    @Test
    fun `legacy raw json remains readable`() {
        val raw = "{\"format\":\"FrameByNavinBackup\",\"schemaVersion\":8}"

        assertEquals(raw, PortableBackupArchive.readBackupJson(ByteArrayInputStream(raw.toByteArray())))

        val staged = PortableBackupArchive.stage(
            ByteArrayInputStream(raw.toByteArray()),
            temporaryFolder.newFolder("legacy-stage"),
        )
        assertFalse(staged.isArchive)
        assertEquals(raw, staged.backupJson)
        assertTrue(staged.mediaByIdeaId.isEmpty())
    }

    @Test
    fun `archive with no voice recordings is valid`() {
        val output = ByteArrayOutputStream()
        PortableBackupArchive.write("{}", emptyList(), output)

        val staged = PortableBackupArchive.stage(
            ByteArrayInputStream(output.toByteArray()),
            temporaryFolder.newFolder("empty-media-stage"),
        )
        try {
            assertTrue(staged.isArchive)
            assertTrue(staged.mediaByIdeaId.isEmpty())
        } finally {
            staged.cleanup()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsafe zip entry is rejected without writing traversal path`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backlot.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("../escape.m4a"))
            zip.write(byteArrayOf(7, 8, 9))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("voice_media.tsv"))
            zip.write("BacklotVoiceMedia\t1".toByteArray())
            zip.closeEntry()
        }
        val stageRoot = temporaryFolder.newFolder("unsafe-stage")

        try {
            PortableBackupArchive.stage(ByteArrayInputStream(output.toByteArray()), stageRoot)
        } finally {
            assertFalse(File(stageRoot.parentFile, "escape.m4a").exists())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `manifest cannot reference missing voice media`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backlot.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("voice_media.tsv"))
            zip.write(
                ("BacklotVoiceMedia\t1\n" +
                    "aWRlYS0x\tvoice_ideas/media-0-0123456789abcdef.m4a\t3\t" + "0".repeat(64))
                    .toByteArray(),
            )
            zip.closeEntry()
        }

        PortableBackupArchive.stage(
            ByteArrayInputStream(output.toByteArray()),
            temporaryFolder.newFolder("missing-stage"),
        )
    }
}

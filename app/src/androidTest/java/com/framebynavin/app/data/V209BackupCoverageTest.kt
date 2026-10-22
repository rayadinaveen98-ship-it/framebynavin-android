package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V209BackupCoverageTest {
    @Test fun portablePreviewCountsRealFramesAndLinkOnlyProjectAssets(): Unit = runBlocking {
        // All mutations run on the isolated CI emulator and are rolled back.
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val original = core.createBackup()
        val id = "v209-coverage-${UUID.randomUUID()}"
        val externalUri = "content://com.example.v209.invalid/owner-media-${UUID.randomUUID()}.mp4"
        try {
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val imageBytes = ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
            bitmap.recycle()
            val sha = MessageDigest.getInstance("SHA-256")
                .digest(imageBytes).joinToString("") { "%02x".format(it) }
            val oneFrame = JSONArray().put(JSONObject()
                .put("name", "v209-original.png")
                .put("sha256", sha)
                .put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP)))
            CreatorHeroArchive.import(context, oneFrame.toString())

            val task = CreatorTask(
                id = id,
                title = "Portable coverage check",
                platform = "YouTube",
                contentType = "Long-form",
                dueLabel = "Tomorrow",
                workspace = CreatorContentWorkspace(
                    assets = listOf(CreatorProjectAsset(
                        label = "Source footage",
                        location = externalUri,
                        kind = CreatorAssetKind.VIDEO,
                    )),
                ),
            )
            TaskStore(context).mutate { it + task }

            val manager = PortableCreatorBackupManager(context)
            val bytes = ByteArrayOutputStream().also { manager.writeBackup(it) }.toByteArray()
            val preview = manager.validate(ByteArrayInputStream(bytes))
            assertTrue(preview.personalFramesIncluded)
            assertEquals(1, preview.personalFrameCount)
            assertTrue(preview.externalProjectAssetCount >= 1)

            val entries = mutableListOf<String>()
            var backedUpJson = ""
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries += entry.name
                    if (entry.name == "backlot.json") backedUpJson = zip.readBytes().toString(Charsets.UTF_8)
                    zip.closeEntry()
                }
            }
            // A URI is stored as a reference but no project attachment file is embedded.
            assertTrue(entries.contains("backlot.json"))
            assertFalse(entries.any { it.startsWith("project_assets/") })
            // Core JSON nests serialized project JSON as a string. Its escaped
            // representation is not the URI itself; compare the decoded value.
            val backupRoot = JSONObject(backedUpJson)
            val tasksInBackup = JSONArray(backupRoot.getString("tasks"))
            val restoredProjectJson = (0 until tasksInBackup.length())
                .map { tasksInBackup.getJSONObject(it) }
                .single { it.getString("id") == id }
            val savedAssetLocation = restoredProjectJson.getJSONObject("workspace")
                .getJSONArray("assets").getJSONObject(0).getString("location")
            assertEquals(externalUri, savedAssetLocation)
            assertTrue(entries.contains("best_frames/frame-0.img"))
            assertTrue(entries.contains("best_frames.tsv"))
            val packedFrames = JSONObject(backupRoot.getString("personalFrames"))
            assertEquals("BacklotPortableBestFrames", packedFrames.getString("format"))
            assertEquals(1, packedFrames.getInt("count"))

            // The V208/V209 archive shape remains importable: no media split
            // marker, just the old JSON with a properly verified inline frame.
            val legacyBytes = ByteArrayOutputStream().also {
                PortableBackupArchive.write(core.createBackup(), emptyList(), it)
            }.toByteArray()
            assertEquals(1, manager.validate(ByteArrayInputStream(legacyBytes)).personalFrameCount)

            // Corrupt the image itself while leaving the original manifest in place.
            // Preview must validate ALL bytes and refuse the archive before restore.
            val damaged = rewriteArchive(bytes) { name, data ->
                if (name.startsWith("best_frames/") && data.isNotEmpty()) {
                    data[0] = (data[0].toInt() xor 0xff).toByte()
                }
                data
            }
            val checksumError = assertThrows(IllegalArgumentException::class.java) {
                manager.validate(ByteArrayInputStream(damaged))
            }
            assertTrue(checksumError.message.orEmpty().contains("integrity", ignoreCase = true))

            // A missing manifest must never silently import zero Best Frames.
            val missingManifest = rewriteArchive(bytes, omit = "best_frames.tsv")
            assertThrows(IllegalArgumentException::class.java) {
                manager.validate(ByteArrayInputStream(missingManifest))
            }
        } finally {
            // Never exercise restore on real installed data; this test is CI-emulator gated.
            CreatorDataGate.nonCancellable { core.restore(original) }
        }
    }

    private fun rewriteArchive(
        bytes: ByteArray,
        omit: String? = null,
        modify: (String, ByteArray) -> ByteArray = { _, payload -> payload },
    ): ByteArray = ByteArrayOutputStream().also { output ->
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            ZipOutputStream(output).use { zip ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val body = input.readBytes()
                    if (entry.name != omit) {
                        zip.putNextEntry(ZipEntry(entry.name))
                        zip.write(modify(entry.name, body))
                        zip.closeEntry()
                    }
                    input.closeEntry()
                }
            }
        }
    }.toByteArray()

    @Test fun validatedCurrentSnapshotReportsZeroAbsentAssetReferences(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val current = manager.createBackup()
        val preview = manager.validate(current)
        assertTrue(preview.personalFramesIncluded)
        val tasks = JSONArray(JSONObject(current).getString("tasks"))
        val expected = (0 until tasks.length()).sumOf { index ->
            val items = tasks.getJSONObject(index).optJSONObject("workspace")?.optJSONArray("assets")
            if (items == null) 0 else (0 until items.length()).count { n ->
                items.optJSONObject(n)?.optString("location", "").orEmpty().isNotBlank()
            }
        }
        assertEquals(expected, preview.externalProjectAssetCount)
    }
}

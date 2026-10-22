package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            assertTrue(backedUpJson.contains(externalUri))
            assertEquals(1, JSONArray(JSONObject(backedUpJson).getString("personalFrames")).length())
        } finally {
            // Never exercise restore on real installed data; this test is CI-emulator gated.
            CreatorDataGate.nonCancellable { core.restore(original) }
        }
    }

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

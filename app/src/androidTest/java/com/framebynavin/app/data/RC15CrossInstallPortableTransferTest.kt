package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalRepeat
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CI-EMULATOR-ONLY transfer between *independent app installs*.
 *
 * phase1 runs on Android 11, exports files to this app's internal filesDir.
 * GitHub Actions uses adb exec-out/run-as to copy those exact bytes to its
 * ephemeral runner, uninstalls no owner's application, then creates a fresh
 * Android 16 emulator and copies the files into its isolated app filesDir.
 * phase2 imports them on Android 16. Not a Play-signing migration test.
 *
 * Never run on a personal device: these fixtures and restore operations
 * deliberately modify the installed app's data, and the gate rejects phones.
 */
@RunWith(AndroidJUnit4::class)
class RC15CrossInstallPortableTransferTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val transferDir get() = File(app.filesDir, "rc15_cross_install")
    private val creatorFile get() = File(transferDir, "creator.fbnbackup")
    private val personalFile get() = File(transferDir, "personal.json")
    private val manifestFile get() = File(transferDir, "manifest.json")
    private val projectId = "rc15-portable-project"
    private val privateId = "personal:rc15-portable-reminder"
    private val privateNotes = "Synthetic offline note for RC15 only"
    private val sourceUri = "content://com.example.rc15.synthetic/original-camera-file.mov"
    private val script = "A filmmaker studies the silence between cuts."
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun digest(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                sha.update(buffer, 0, count)
            }
        }
        return sha.digest().joinToString("") { "%02x".format(it) }
    }

    @Test fun phase1ExportFromAndroid11(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("Sender must be a disposable Android 11 emulator", 30, android.os.Build.VERSION.SDK_INT)
        val tasks = TaskStore(app)
        val reminders = PersonalReminderStore(app)
        assertFalse("Sender already has fixture", tasks.load().any { it.id == projectId })
        assertFalse("Sender already has private fixture", reminders.load().any { it.id == privateId })
        require(!transferDir.exists()) { "Refusing to replace existing transfer files" }

        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { output ->
            try { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() }
            finally { bitmap.recycle() }
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val frame = JSONArray().put(JSONObject()
            .put("name", "rc15-still.png")
            .put("sha256", hash)
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        CreatorHeroArchive.import(app, frame.toString())

        tasks.mutate { existing ->
            existing + CreatorTask(
                id = projectId,
                title = "Cross-device camera study",
                platform = "YouTube",
                contentType = "Long-form",
                dueLabel = "Upcoming",
                notes = "Keep scene notes and source footage reference",
                status = TaskStatus.WORKING,
                progress = 42,
                workspace = CreatorContentWorkspace(
                    script = script,
                    assets = listOf(CreatorProjectAsset(
                        label = "Original footage - not bundled",
                        location = sourceUri,
                        kind = CreatorAssetKind.VIDEO,
                    )),
                ),
            )
        }

        reminders.create(PersonalReminder(
            id = privateId,
            title = "Synthetic private reminder",
            notes = privateNotes,
            startDate = LocalDate.of(2036, 12, 12),
            time = LocalTime.of(9, 15),
            zoneId = zone,
            repeat = PersonalRepeat.ONCE,
            alert = PersonalAlert.NOTIFICATION,
            paused = true, // no alarm or OS permission request on either emulator
        ))

        check(transferDir.mkdirs())
        val portable = PortableCreatorBackupManager(app)
        creatorFile.outputStream().buffered().use { portable.writeBackup(it) }
        personalFile.writeText(reminders.exportPersonalJson(), Charsets.UTF_8)

        assertTrue(creatorFile.length() > 100L)
        assertTrue(personalFile.length() > 100L)
        val preview = creatorFile.inputStream().use(portable::validate)
        assertTrue(preview.projectCount >= 1)
        assertTrue(preview.personalFramesIncluded)
        assertTrue(preview.personalFrameCount >= 1)
        assertTrue(preview.externalProjectAssetCount >= 1)
        assertEquals(1, PersonalBackupTransfer(app).preview(personalFile.readText()).reminderCount)
        // A creator-only export must not be used as a Personal reminder archive.
        assertTrue(runCatching {
            PersonalBackupTransfer(app).preview(creatorFile.readText())
        }.isFailure)

        manifestFile.writeText(JSONObject()
            .put("schema", "RC15CrossInstallSyntheticV1")
            .put("creatorSha256", digest(creatorFile))
            .put("personalSha256", digest(personalFile))
            .put("projectId", projectId)
            .put("privateId", privateId)
            .put("frameSha256", hash)
            .toString(), Charsets.UTF_8)
        assertEquals(hash, JSONObject(manifestFile.readText()).getString("frameSha256"))
        println("RC15 sender: 2 verified exports and original synthetic frame, project, Personal reminder")
    }

    @Test fun phase2ImportIntoFreshAndroid16(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("Receiver must be a separate disposable Android 16 emulator", 36, android.os.Build.VERSION.SDK_INT)
        val tasks = TaskStore(app)
        val reminders = PersonalReminderStore(app)
        assertTrue("Fresh install already has sender project", tasks.load().none { it.id == projectId })
        assertTrue("Fresh install already has sender Personal reminder", reminders.load().none { it.id == privateId })
        assertTrue("Receiver has no valid Creator archive", creatorFile.isFile && creatorFile.length() > 100L)
        assertTrue("Receiver has no valid Personal archive", personalFile.isFile && personalFile.length() > 100L)
        assertTrue(manifestFile.isFile)
        val manifest = JSONObject(manifestFile.readText())
        assertEquals("RC15CrossInstallSyntheticV1", manifest.getString("schema"))
        assertEquals(digest(creatorFile), manifest.getString("creatorSha256"))
        assertEquals(digest(personalFile), manifest.getString("personalSha256"))

        val portable = PortableCreatorBackupManager(app)
        val preview = creatorFile.inputStream().use(portable::validate)
        assertTrue(preview.projectCount >= 1)
        assertTrue(preview.personalFrameCount >= 1)
        assertTrue(preview.externalProjectAssetCount >= 1)

        // Creator import cannot import Personal reminders, even when run first.
        creatorFile.inputStream().use { portable.restore(it) }
        assertTrue(reminders.load().none { it.id == privateId })
        val restored = tasks.load().single { it.id == projectId }
        assertEquals("Cross-device camera study", restored.title)
        assertEquals(42, restored.progress)
        assertEquals(script, restored.workspace.script)
        assertEquals(sourceUri, restored.workspace.assets.single().location)
        assertTrue(CreatorHeroArchive.validate(CreatorHeroArchive.export(app)) >= 1)

        // Personal backup remains a separately approved, isolated import.
        val personal = PersonalBackupTransfer(app)
        assertEquals(1, personal.preview(personalFile.readText()).reminderCount)
        val receipt = personal.restoreApproved(personalFile.readText())
        assertEquals(1, receipt.importedCount)
        val imported = reminders.load().single { it.id == privateId }
        assertEquals(privateNotes, imported.notes)
        assertEquals(zone, imported.zoneId)
        assertTrue(imported.paused)
        assertEquals("", imported.pendingToken)
        assertEquals("", imported.presentedToken)
        assertEquals(script, tasks.load().single { it.id == projectId }.workspace.script)
        println("RC15 receiver: Creator workspace, original frame, URI-only asset, and isolated Personal records verified")
    }
}

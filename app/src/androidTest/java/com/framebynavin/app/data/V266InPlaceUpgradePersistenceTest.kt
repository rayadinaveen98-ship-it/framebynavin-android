package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderCodec
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalRepeat
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CI-only, three PHASE in-place Android APK upgrade test.
 *
 * Prepare is executed against the INSTALLED signed-debug Build265 using the
 * Build266 test APK; Gradle keeps the app package and test interfaces stable.
 * CI force-stops the old installation and adb installs Build266 with -r,
 * NEVER -d and NEVER uninstall/pm clear. Subsequent instrumentation processes
 * verify persistence before and after a normal Activity startup.
 *
 * Every method must be invoked SEPARATELY in the workflow and in order.
 * Test refuses non-emulator environments and unexpected app version codes.
 */
@RunWith(AndroidJUnit4::class)
class V266InPlaceUpgradePersistenceTest {
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val fixture: File get() = File(app.filesDir, "rc4-build266-upgrade-fixture.json")
    private val projectId = "rc4-upgrade-creator-proof"
    private val editedLegacyId = "starter-frame-breakdown"
    private val privateId = "personal:rc4-upgrade-proof"
    private val privateSecret = "RC4 private offline diary proof - never in creator backup"

    private fun versionCode(): Long = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode

    private fun editorProject(): CreatorTask = CreatorTask(
        id = projectId,
        title = "RC4 film project survives upgrade",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Tomorrow",
        status = TaskStatus.WORKING,
        progress = 33,
        notes = "Shot list and timeline remain saved",
        workspace = CreatorContentWorkspace(script = "A genuine script, not demo data"),
    )

    private fun editedLegacy(): CreatorTask = CreatorTask(
        id = editedLegacyId,
        title = "My edited Frame Breakdown",
        platform = "YouTube",
        contentType = "Video Essay",
        dueLabel = "Next week",
        status = TaskStatus.WORKING,
        progress = 72,
        notes = "Real creator work under the old demo ID",
        workspace = CreatorContentWorkspace(script = "Own narration"),
    )

    private fun privateRecord(): PersonalReminder = PersonalReminder(
        id = privateId,
        title = "RC4 private reminder survives upgrade",
        notes = privateSecret,
        startDate = LocalDate.of(2036, 12, 12),
        time = LocalTime.of(9, 15),
        zoneId = ZoneId.of("Asia/Kolkata"),
        repeat = PersonalRepeat.ONCE,
        alert = PersonalAlert.NOTIFICATION,
        paused = true, // no delivery, alarms or background permission prompts
        createdAtMillis = 1_767_000_000_000L,
        updatedAtMillis = 1_767_000_000_000L,
    )

    @Test fun phase1SeedOlderAppAndCaptureVerifiedBackup(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("CI must first install the exact prior Build265", 265L, versionCode())
        assertFalse("Never overwrite an earlier failed upgrade fixture", fixture.exists())

        val store = TaskStore(app)
        val privateStore = PersonalReminderStore(app)
        assertTrue(store.load().none { it.id in setOf(projectId, editedLegacyId) })
        assertTrue(privateStore.load().none { it.id == privateId })

        store.mutate { current -> current + editorProject() + editedLegacy() }
        privateStore.create(privateRecord())

        val canonical = store.exportJson()
        val savedTasks = store.load()
        val privateBackup = privateStore.exportPersonalJson()
        val creatorBackup = CreatorBackupManager(app).createBackup()
        assertTrue(CreatorBackupManager(app).validate(creatorBackup).projectCount >= 2)
        assertFalse("Private reminder content must not leak to Creator backup", creatorBackup.contains(privateSecret))
        assertEquals(1, PersonalReminderCodec.decode(privateBackup).count { it.id == privateId })

        val snapshot = JSONObject()
            .put("tasks", canonical)
            .put("expectedProject", store.encodeRecoveryTask(savedTasks.first { it.id == projectId }))
            .put("expectedEditedLegacy", store.encodeRecoveryTask(savedTasks.first { it.id == editedLegacyId }))
            .put("personal", privateBackup)
            .put("privateId", privateId)
            .put("projectId", projectId)
        val atomic = AtomicFile(fixture)
        val handle = atomic.startWrite()
        try {
            handle.write(snapshot.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(handle)
        } catch (failure: Throwable) {
            atomic.failWrite(handle)
            throw failure
        }
        assertTrue("Upgrade fixture was not committed to app-owned internal storage", fixture.isFile)
    }

    @Test fun phase2VerifyUpgradeWithoutUninstall(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("Actual Build266 must replace Build265 via adb install -r", 266L, versionCode())
        assertTrue("App private data was wiped or old package uninstalled", fixture.isFile)
        verifyCurrentSavedData()
    }

    @Test fun phase3VerifyAfterNewAppLaunchAndCleanUp(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(266L, versionCode())
        assertTrue("Activity launch lost the prior installation data", fixture.isFile)
        verifyCurrentSavedData()

        // Cleanup ONLY our own ephemeral emulator QA records. Never wipe app data.
        val taskStore = TaskStore(app)
        val personalStore = PersonalReminderStore(app)
        taskStore.mutate { current -> current.filterNot { it.id in setOf(projectId, editedLegacyId) } }
        personalStore.delete(privateId)
        assertTrue(fixture.delete())
    }

    private suspend fun verifyCurrentSavedData() {
        val snapshot = JSONObject(AtomicFile(fixture).openRead().bufferedReader().use { it.readText() })
        val projectStore = TaskStore(app)
        val personalStore = PersonalReminderStore(app)

        val currentProjects = projectStore.load()
        val expected = listOf(
            projectStore.decodeRecoveryTask(snapshot.getString("expectedProject")),
            projectStore.decodeRecoveryTask(snapshot.getString("expectedEditedLegacy")),
        )
        assertEquals("Two seeded creator projects must survive in-place upgrade", 2, expected.size)
        assertEquals(expected, currentProjects.filter { it.id in setOf(projectId, editedLegacyId) })
        assertEquals("Creator project must still have its saved script",
            "A genuine script, not demo data",
            currentProjects.first { it.id == projectId }.workspace.script)
        assertEquals("Customized historical demo ID must never be deleted",
            editedLegacy().title, currentProjects.first { it.id == editedLegacyId }.title)

        val expectedPrivate = PersonalReminderCodec.decode(snapshot.getString("personal"))
            .first { it.id == privateId }
        val restoredPrivate = personalStore.load().first { it.id == privateId }
        assertEquals(expectedPrivate.title, restoredPrivate.title)
        assertEquals(expectedPrivate.notes, restoredPrivate.notes)
        assertEquals(expectedPrivate.startDate, restoredPrivate.startDate)
        assertEquals(expectedPrivate.paused, restoredPrivate.paused)
        assertEquals(expectedPrivate.zoneId, restoredPrivate.zoneId)
        assertFalse(CreatorBackupManager(app).createBackup().contains(privateSecret))
    }
}

package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual QA only: synthetic Creator and Personal evidence deliberately seeded
 * on a disposable, cleared emulator. No phone, account, cloud, live reminder
 * delivery, owner journal or user photograph is accessed.
 */
@RunWith(AndroidJUnit4::class)
class V309PopulatedActivityVisualQaTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun screenshot(name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.waitForIdle()
        val dir = File(requireNotNull(app.getExternalFilesDir(null)), "v309-populated-activity-audit")
        check(dir.isDirectory || dir.mkdirs())
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(image.width >= 200 && image.height >= 200)
            val file = File(dir, name)
            file.outputStream().use { stream ->
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            check(file.length() > 10_000L)
        } finally { image.recycle() }
    }

    @Test fun actualPopulatedActivityLayoutFitsSyntheticCreatorAndPersonalEvents() {
        HardeningTestEnvironment.requireCiEmulator()
        val now = System.currentTimeMillis()
        val creatorTasks = listOf(
            CreatorTask(
                id = "v309-visual-creator-missed",
                title = "Friday Filmmaker POV review",
                platform = "YouTube",
                contentType = "Long-form",
                dueLabel = "Today",
                reminderEnabled = true,
                reminderMode = ReminderMode.SIMPLE,
                reminderAtMillis = now - 30_000L,
            ),
            CreatorTask(
                id = "v309-visual-creator-delivery",
                title = "Edit cinematic sound breakdown",
                platform = "YouTube",
                contentType = "Video",
                dueLabel = "Today",
                reminderEnabled = true,
                reminderMode = ReminderMode.SIMPLE,
                reminderAtMillis = now - 60_000L,
            )
        )

        val occurrences = ReminderOccurrenceStore(app)
        val acknowledgements = ReminderAcknowledgementStore(app)
        val missed = occurrences.issue(creatorTasks[0])
        val failed = occurrences.issue(creatorTasks[1])
        assertTrue(acknowledgements.begin(creatorTasks[0], missed, "REGULAR", now - 35_000L))
        assertTrue(acknowledgements.markUnanswered(
            creatorTasks[0].id, missed, atMillis = now - 20_000L
        ))
        assertTrue(acknowledgements.begin(creatorTasks[1], failed, "REGULAR", now - 40_000L))
        assertTrue(acknowledgements.markDeliveryFailed(
            creatorTasks[1].id, failed, atMillis = now - 15_000L
        ))

        val store = PersonalReminderStore(app)
        val token = UUID.randomUUID().toString()
        val occurrence = now - 18_000L
        val personalPending = PersonalReminder(
            id = "v309-visual-personal-pending",
            title = "Evening skincare routine",
            startDate = LocalDate.now(),
            time = LocalTime.of(20, 0),
            pendingOccurrenceMillis = occurrence,
            pendingFireAtMillis = occurrence,
            pendingToken = token,
            presentedToken = token,
            presentedAtMillis = now - 10_000L,
        )
        val personalCompleted = PersonalReminder(
            id = "v309-visual-personal-completed",
            title = "Daily journal",
            startDate = LocalDate.now(),
            time = LocalTime.of(9, 0),
        )
        val receipt = PersonalCompletionEvent(
            reminderId = personalCompleted.id,
            occurrenceMillis = now - 86_000L,
            doneAtMillis = now - 40_000L,
        )
        runBlocking {
            assertTrue(store.load().isEmpty())
            store.create(personalPending)
            store.create(personalCompleted)
            store.mutate(replacementHistory = listOf(receipt)) { it }
            assertEquals(2, store.load().size)
            assertEquals(listOf(receipt), store.loadCompletionHistory())
        }
        rule.setContent {
            FrameByNavinTheme {
                V306ActivityCenterScreen(
                    tasks = creatorTasks,
                    nowMillis = now,
                    onClose = {},
                    onOpenCreator = {},
                    onOpenPersonal = {},
                )
            }
        }
        rule.waitUntil(20_000) {
            rule.onAllNodesWithTag("v306-item-creator").fetchSemanticsNodes().size == 2 &&
                rule.onAllNodesWithTag("v306-item-personal").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("Edit cinematic sound breakdown").assertExists()
        rule.onNodeWithText("Friday Filmmaker POV review").assertExists()
        rule.onNodeWithText("Evening skincare routine").assertExists()
        screenshot("01-activity-realistic-needs-action.png")

        rule.onNodeWithTag("v306-filter-all").performClick()
        rule.onNodeWithText("Daily journal").assertExists()
        rule.onNodeWithText("Marked Done by you").assertExists()
        screenshot("02-activity-realistic-all.png")

        rule.onNodeWithTag("v306-filter-history").performClick()
        rule.onNodeWithText("Daily journal").assertExists()
        rule.onNodeWithText("Evening skincare routine").assertDoesNotExist()
        screenshot("03-activity-realistic-history.png")

        // Viewing and switching filters never completes/removes the pending item.
        assertEquals(2, runBlocking { store.load() }.size)
        assertEquals(receipt, runBlocking { store.loadCompletionHistory() }.single())
    }
}

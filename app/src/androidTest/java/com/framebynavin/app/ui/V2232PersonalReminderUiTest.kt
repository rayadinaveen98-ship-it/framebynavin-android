package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalDeliveryCoordinator
import com.framebynavin.app.personal.PersonalReminderNotifications
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Personal creation form + DataStore, no creator task or alarm impersonation. */
@RunWith(AndroidJUnit4::class)
class V2232PersonalReminderUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun personalSpaceCreatesAndPersistsWithoutModifyingCreatorProjects() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val before = runBlocking { TaskStore(app).load() }
        runBlocking { store.mutate { emptyList() } }
        try {
            rule.setContent {
                FrameByNavinTheme {
                    V2232PersonalReminderHub(onClose = {}, onCreator = {})
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v2232-personal-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v2232-personal-empty-add").performClick()
            rule.onNodeWithTag("v2232-personal-editor").assertExists()
            rule.onNodeWithTag("v2232-personal-title").performTextInput("Pay electricity bill")
            rule.onNodeWithTag("v2232-personal-save").performClick()
            rule.waitUntil(12_000) {
                runBlocking { store.load().any { it.title == "Pay electricity bill" } }
            }
            rule.onNodeWithText("Pay electricity bill").assertExists()
            val personal = runBlocking { store.load() }
            assertEquals(1, personal.size)
            assertTrue(personal.single().pendingToken.isNotBlank())
            assertEquals(before, runBlocking { TaskStore(app).load() })
        } finally {
            runBlocking {
                store.load().forEach {
                    PersonalDeliveryCoordinator(app).cancelPending(it.id)
                    PersonalReminderNotifications.cancel(app, it.id)
                }
                store.mutate { emptyList() }
            }
        }
    }

    @Test fun editorRejectsEmptyTitleWithoutTouchingStore() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        runBlocking { store.mutate { emptyList() } }
        try {
            rule.setContent {
                FrameByNavinTheme {
                    V2232PersonalEditor(
                        existing = null,
                        saving = false,
                        onDismiss = {},
                        onSave = { error("Should not save blank personal reminder") },
                    )
                }
            }
            rule.onNodeWithTag("v2232-personal-editor").assertExists()
            rule.onNodeWithTag("v2232-personal-save").performClick()
            rule.onNodeWithTag("v2232-editor-error").assertExists()
            assertTrue(runBlocking { store.load().isEmpty() })
        } finally {
            runBlocking { store.mutate { emptyList() } }
        }
    }
}

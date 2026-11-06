package com.framebynavin.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesRepository
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated on emulator: opt-in metric UI saves just one manually logged Room entry. */
@RunWith(AndroidJUnit4::class)
class V292SpaceMetricUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun workoutMinutesAreExplicitAndDoNotModifyPersonalReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.FITNESS,
            System.currentTimeMillis(),
            "10000000-0000-4000-8000-000000000091",
        )
        runBlocking { V289SpacesRepository(app, db.spaces()).createSpace(space) }
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-space-" + space.id)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-space-" + space.id).performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-log-open").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-log-open").performClick()
            rule.onNodeWithTag("v292-metric-toggle").assertExists().performClick()
            rule.onNodeWithTag("v289-checkin-save").assertIsNotEnabled()
            rule.onNodeWithTag("v292-metric-value").performTextInput("0")
            rule.onNodeWithTag("v289-checkin-save").assertIsNotEnabled()
            rule.onNodeWithTag("v292-metric-value").performTextReplacement("40")
            rule.onNodeWithTag("v289-checkin-save").assertIsEnabled()
            rule.onNodeWithTag("v289-checkin-note").performTextInput("Morning walk")
            rule.onNodeWithTag("v289-checkin-save").assertIsEnabled().performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v292-recorded-metric").fetchSemanticsNodes().isNotEmpty()
            }
            val entries = runBlocking { db.spaces().entriesForSpace(space.id) }
            assertEquals(1, entries.size)
            assertEquals(V289SpaceEntryKind.METRIC.name, entries.single().kind)
            assertEquals("Workout time", entries.single().metricName)
            assertEquals(40.0, entries.single().metricValue!!, 0.0)
            assertEquals("minutes", entries.single().metricUnit)
            assertEquals("Morning walk", entries.single().note)
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
            assertTrue(runBlocking { db.spaces().photosForSpace(space.id).isEmpty() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }
}

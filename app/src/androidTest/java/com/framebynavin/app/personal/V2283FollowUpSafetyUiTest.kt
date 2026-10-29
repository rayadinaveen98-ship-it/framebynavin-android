package com.framebynavin.app.personal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.V2232PersonalReminderHub
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2283FollowUpSafetyUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun followUpsRemainOffUntilUserOptInAndCanBeTurnedOff() {
        HardeningTestEnvironment.requireCiEmulator()
        val prefs = PersonalFollowUpSettingsStore(app)
        runBlocking { prefs.setEnabled(false) }
        try {
            rule.setContent { FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = {}, onCreator = {})
            } }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag("v2283-follow-up-toggle").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v2283-follow-up-toggle").assertIsOff().performClick()
            rule.waitUntil(15_000) { runBlocking { prefs.current().enabled } }
            rule.onNodeWithTag("v2283-follow-up-toggle").assertIsOn()
            rule.onNodeWithTag("v2283-follow-up-toggle").performClick()
            rule.waitUntil(15_000) { runBlocking { !prefs.current().enabled } }
            rule.onNodeWithTag("v2283-follow-up-toggle").assertIsOff()
        } finally {
            runBlocking { prefs.setEnabled(false) }
        }
    }

    @Test fun claimedFollowUpIsAtMostOnceAndStaleLeaseCannotRepeat() {
        HardeningTestEnvironment.requireCiEmulator()
        val prefs = PersonalFollowUpSettingsStore(app)
        val data = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 10, 29)
        val due = day.atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
        val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val token = UUID.randomUUID().toString()
        val id = "v2283-one-shot-test"
        runBlocking {
            prefs.setEnabled(false)
            data.delete(id)
            data.create(PersonalReminder(
                id = id, title = "Synthetic follow-up",
                startDate = day, time = LocalTime.of(11, 0), zoneId = zone,
                repeat = PersonalRepeat.DAILY,
                pendingOccurrenceMillis = due, pendingFireAtMillis = due,
                pendingToken = token, presentedToken = token,
                presentedAtMillis = due,
            ))
        }
        try {
            assertFalse(runBlocking { coordinator.deliverFollowUp(id, due, token, now) })
            assertEquals("", runBlocking { data.load().first { it.id == id }.followUpSentToken })
            runBlocking { prefs.setEnabled(true) }
            // Android notification permission can be denied in a test emulator;
            // the durable one-shot claim, not a claimed visual delivery, is tested.
            runBlocking { coordinator.deliverFollowUp(id, due, token, now) }
            assertEquals(token, runBlocking { data.load().first { it.id == id }.followUpSentToken })
            assertEquals(due, runBlocking {
                data.load().first { it.id == id }.followUpSentOccurrenceMillis
            })
            assertFalse(runBlocking { coordinator.deliverFollowUp(id, due, token, now) })
            runBlocking {
                data.update(id) { it.copy(
                    pendingToken = UUID.randomUUID().toString(),
                    presentedToken = "",
                ) }
            }
            assertFalse(runBlocking { coordinator.deliverFollowUp(id, due, token, now) })
        } finally {
            runBlocking {
                prefs.setEnabled(false)
                data.delete(id)
            }
            coordinator.cancelPending(id)
            PersonalReminderNotifications.cancel(app, id)
        }
    }
}

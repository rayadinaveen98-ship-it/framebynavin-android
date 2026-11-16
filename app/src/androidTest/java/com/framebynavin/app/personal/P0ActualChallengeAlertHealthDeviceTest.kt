package com.framebynavin.app.personal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.Phase1HabitAlertHealthPanel
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 QA ONLY. Run AFTER P0ActualChallengeAlarmClockDeviceTest on the same
 * cleared disposable Android 15 emulator, in a NEW instrumentation process
 * without clearing synthetic DataStore or delivery evidence.
 *
 * Verifies the REAL Alert Health Compose panel classifies a real OS-fired,
 * notification-posted challenge occurrence. No manual broadcast, owner data,
 * cloud access, forced-stop, fake completion or production release.
 */
@RunWith(AndroidJUnit4::class)
class P0ActualChallengeAlertHealthDeviceTest {
    @get:Rule val rule = createComposeRule()

    @Test fun actualOsFireAppearsAsPostedWithoutClaimingOwnerSawIt() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val challenge = runBlocking {
            val records = Phase1HabitStore(app).load()
            assertEquals("Only the synthetic CI challenge is allowed", 1, records.size)
            records.single()
        }
        assertEquals("ci-p0-os-timed-actual-v337", challenge.id)
        assertEquals(Phase1HabitAlertKind.ALARM, challenge.alertKind)
        assertTrue("OS fire must not auto-complete an activity",
            challenge.confirmations.isEmpty())

        val dueMillis = challenge.startDate
            .atTime(requireNotNull(challenge.alertTime))
            .atZone(ZoneId.of(challenge.zoneId))
            .toInstant().toEpochMilli()
        val events = Phase1HabitDeliveryLedger(app).recent()
        val matching = events.filter {
            it.challengeId == challenge.id && it.scheduledAtMillis == dueMillis
        }
        assertTrue("Previous test must have recorded actual OS-fire evidence",
            matching.any { it.stage == Phase1HabitDeliveryStage.OS_FIRED })
        assertTrue("Previous test must have recorded a posted notification",
            matching.any { it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED })
        assertEquals(
            "A posted notification is not evidence that the owner saw or heard it",
            Phase1HabitDeliveryHealth.NOTIFICATION_POSTED,
            Phase1HabitDeliveryEvidencePolicy.health(events, challenge.id,
                dueMillis, System.currentTimeMillis()),
        )

        rule.setContent {
            FrameByNavinTheme {
                Phase1HabitAlertHealthPanel(challenge, Instant.now(), onRepair = {})
            }
        }
        rule.onNodeWithText("ALERT HEALTH").assertIsDisplayed()
        rule.onNodeWithText("DETAILS").performClick()
        rule.onNodeWithText(
            "Notification posted. This does not prove you saw it.",
        ).assertIsDisplayed()
        rule.onNodeWithText(
            "RECHECK & RE-ARM",
        ).assertIsDisplayed()
        assertTrue("Alert Health must not mutate challenge Done state",
            runBlocking { Phase1HabitStore(app).load().single().confirmations.isEmpty() })
    }
}

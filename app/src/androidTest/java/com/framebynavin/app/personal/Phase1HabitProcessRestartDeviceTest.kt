package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This class MUST NOT be run as a whole. The CI script invokes writeSynthetic
 * in one instrumentation process, force-stops the app, and invokes
 * verifyAfterProcessRestart in a NEW instrumentation process without pm clear.
 *
 * Only disposable Android CI emulator, synthetic isolated DataStore entries.
 */
@RunWith(AndroidJUnit4::class)
class Phase1HabitProcessRestartDeviceTest {
    private val expectedDay = LocalDate.of(2026, 11, 7)
    private val receipt = Instant.parse("2026-11-07T04:00:00Z")
    private val id = "ci-restart-single-habit"

    private val context get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun expected(): Phase1HabitChallenge = Phase1HabitChallenge(
        id = id,
        activity = "Practice guitar",
        startDate = expectedDay,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.QUIET,
    ).confirmToday(receipt)

    @Test fun writeSynthetic() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = context
        check(!StabilizationPersonalMutationGate.journalRootFor(app).exists()) {
            "Refuse to mutate while recovery is active"
        }
        val store = Phase1HabitStore(app)
        check(store.load().isEmpty() &&
            V297ChallengeStore(app).load().isEmpty() &&
            PersonalReminderStore(app).load().isEmpty()) {
            "Refuse non-empty device data: CI must pm clear before writer"
        }
        val original = expected().copy(confirmations = emptyMap())
        assertEquals(listOf(original), store.create(original))
        assertEquals(listOf(expected()), store.confirmToday(id, receipt))
        assertEquals(listOf(expected()), store.load())
        assertEquals(expected(), Phase1HabitCodec.decode(store.exportJson()).single())
    }

    @Test fun verifyAfterProcessRestart() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = context
        check(!StabilizationPersonalMutationGate.journalRootFor(app).exists()) {
            "Refuse to inspect incomplete restore as valid progress"
        }
        val saved = Phase1HabitStore(app).load()
        assertEquals("Persistent challenge must survive app process restart",
            listOf(expected()), saved)
        assertEquals(receipt, saved.single().confirmations[expectedDay])
        assertTrue("Receipt must not be duplicated", saved.single().confirmations.size == 1)
        assertEquals(saved, Phase1HabitCodec.decode(Phase1HabitStore(app).exportJson()))
        assertTrue("No legacy challenge records were touched",
            V297ChallengeStore(app).load().isEmpty())
        assertTrue("No Personal reminder records were touched",
            PersonalReminderStore(app).load().isEmpty())
        assertFalse("Only explicit Done counts", saved.single().paused)
    }
}

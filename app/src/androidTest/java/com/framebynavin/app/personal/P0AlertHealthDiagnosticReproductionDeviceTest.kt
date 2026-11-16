package com.framebynavin.app.personal

import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QA DIAGNOSTIC, NOT A PRODUCT ACCEPTANCE TEST.
 *
 * A PASS here means the current frozen V337 implementation reproduced a
 * suspected reporting discrepancy; it DOES NOT mean the reporting is correct.
 * Must run only on a disposable, explicitly marked Android CI emulator.
 * No main-app source changes, real account/data, cloud access or APK release.
 */
@RunWith(AndroidJUnit4::class)
class P0AlertHealthDiagnosticReproductionDeviceTest {
    private val context get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun reproducesEarlierVoiceStartedMaskingLaterSpeechFailure() {
        HardeningTestEnvironment.requireCiEmulator()
        val id = "p0-voice-diagnostic"
        val scheduledAt = 1_900_000_000_000L
        val events = listOf(
            Phase1HabitDeliveryEvidence(
                id, scheduledAt, Phase1HabitDeliveryStage.VOICE_REQUESTED,
                scheduledAt - 1L,
            ),
            Phase1HabitDeliveryEvidence(
                id, scheduledAt, Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED,
                scheduledAt + 100L,
            ),
            Phase1HabitDeliveryEvidence(
                id, scheduledAt, Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED,
                scheduledAt + 500L,
            ),
        )
        val observed = Phase1HabitDeliveryEvidencePolicy.health(
            events, id, scheduledAt, scheduledAt + 1000L,
        )
        // Known V337 characterization; a product fix should make this fail.
        assertEquals("REPRODUCED: speech start currently masks a later failure",
            Phase1HabitDeliveryHealth.VOICE_SPEAKING, observed)
        assertNotEquals("Desired behavior is to surface failed voice playback",
            Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE, observed)
        println("P0_DIAGNOSTIC_REPRODUCED=VOICE_FAILURE_MASKED")
        println("P0_DESIRED_AFTER_APPROVAL=VOICE_UNAVAILABLE")
    }

    @Test fun reproducesMissingNotificationChannelReportedAsAllowed() {
        HardeningTestEnvironment.requireCiEmulator()
        assertTrue("Requires Android 8 or newer for notification channels",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val app = context
        val kind = Phase1HabitAlertKind.ALARM
        val manager = app.getSystemService(NotificationManager::class.java)
        val channel = Phase1HabitNotification.channel(kind)

        // Test-only deletion on newly cleared/disposable CI app. NEVER run
        // this on the founder's phone or any non-disposable installation.
        manager.deleteNotificationChannel(channel)
        assertNull("The diagnostic requires a genuinely missing channel",
            manager.getNotificationChannel(channel))
        val snapshot = Phase1HabitAlertReadinessInspector.inspect(app, kind)

        // Known V337 characterization; this is NOT the desired behavior.
        assertTrue("REPRODUCED: null channel is incorrectly called allowed",
            snapshot.channelAllowed)
        println("P0_DIAGNOSTIC_REPRODUCED=MISSING_CHANNEL_REPORTED_ALLOWED")
        println("P0_DESIRED_AFTER_APPROVAL=CHANNEL_NEEDS_ATTENTION")
    }
}

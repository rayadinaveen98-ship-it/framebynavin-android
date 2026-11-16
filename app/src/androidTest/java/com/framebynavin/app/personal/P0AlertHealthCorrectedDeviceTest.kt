package com.framebynavin.app.personal

import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 corrected-contract Android test. Only on a disposable, cleared CI emulator.
 * Never delete a notification channel or app data on a real owner device.
 */
@RunWith(AndroidJUnit4::class)
class P0AlertHealthCorrectedDeviceTest {
    @Test fun absentAlarmChannelIsNotMisreportedAsAllowed() {
        HardeningTestEnvironment.requireCiEmulator()
        assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = Phase1HabitNotification.channel(Phase1HabitAlertKind.ALARM)
        // This modifies the disposable CI package only, never owner settings.
        manager.deleteNotificationChannel(id)
        assertNull("Diagnostic requires a genuinely absent 21-day channel",
            manager.getNotificationChannel(id))
        val readiness = Phase1HabitAlertReadinessInspector.inspect(
            context, Phase1HabitAlertKind.ALARM)
        assertFalse("Missing notification channel must not be reported allowed",
            readiness.channelAllowed)
        assertTrue("Missing channel must appear in attention state",
            readiness.needsAttention)
    }
}

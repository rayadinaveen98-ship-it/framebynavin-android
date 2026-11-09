package com.framebynavin.app.cloud

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable CI emulator only. Real SharedPreferences are restored in finally;
 * no account access, reminders, alarms, cloud uploads or backup writes.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1B3VoiceSparkDeviceTest {
    private fun restore(prefs: SharedPreferences, before: Map<String, *>): Unit {
        val edit = prefs.edit().clear()
        before.forEach { (key, value) ->
            when (value) {
                is String -> edit.putString(key, value)
                is Boolean -> edit.putBoolean(key, value)
                is Int -> edit.putInt(key, value)
                is Long -> edit.putLong(key, value)
                is Float -> edit.putFloat(key, value)
                is Set<*> -> {
                    require(value.all { it is String })
                    edit.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
                else -> throw IllegalStateException("Unrecognized preference type")
            }
        }
        check(edit.commit()) { "Could not restore disposable test settings" }
    }

    @Test fun savedSparkIdentityAndDismissalPreferencesRemainReadOnlyAndFailClosed() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val identity = app.getSharedPreferences(
            "backlot_character_identity_v151", Context.MODE_PRIVATE)
        val tips = app.getSharedPreferences(
            "backlot_companion_suggestions_v2212", Context.MODE_PRIVATE)
        val oldIdentity = identity.all.toMap()
        val oldTips = tips.all.toMap()
        try {
            check(identity.edit().clear()
                .putString("selected_character", "FUNNY").commit())
            check(tips.edit().clear()
                .putBoolean("suggestions_enabled", true)
                .putString("dismissed", "[{\"id\":\"spark-receipt-1\",\"at\":1000,\"until\":2000}]")
                .commit())

            val first = requireNotNull(TrustSparkPreferenceInventory.inspect(app))
            val again = requireNotNull(TrustSparkPreferenceInventory.inspect(app))
            assertEquals(TrustBackupDomain.SPARK_IDENTITY_AND_TIPS, first.domain)
            assertEquals(3, first.records) // identity + enabled + one dismissal
            assertEquals(first.sha256, again.sha256)
            assertFalse(first.toString().contains("spark-receipt-1"))
            assertFalse(first.toString().contains("FUNNY"))

            check(tips.edit().putString("dismissed",
                "[{\"id\":\"spark-receipt-1\",\"at\":1000,\"until\":2100}]").commit())
            assertNotEquals(first.sha256,
                requireNotNull(TrustSparkPreferenceInventory.inspect(app)).sha256)

            check(tips.edit().putString("dismissed", "not-json").commit())
            assertThrows(Exception::class.java) {
                TrustSparkPreferenceInventory.inspect(app)
            }
            check(tips.edit().putString("dismissed",
                "[{\"id\":\"invalid\",\"at\":2000,\"until\":1000}]").commit())
            assertThrows(IllegalArgumentException::class.java) {
                TrustSparkPreferenceInventory.inspect(app)
            }
            // Never change the source just because an inventory failed.
            assertEquals("FUNNY", identity.getString("selected_character", null))
            assertTrue(tips.getString("dismissed", "")!!.contains("invalid"))
        } finally {
            restore(identity, oldIdentity)
            restore(tips, oldTips)
        }
        assertEquals(oldIdentity, identity.all.toMap())
        assertEquals(oldTips, tips.all.toMap())
    }

    @Test fun smartReminderEscalationEvidenceNeverRunsLegacyMigration() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val prefs = app.getSharedPreferences(
            "smart_escalation_config_v102", Context.MODE_PRIVATE,
        )
        val before = prefs.all.toMap()
        val key = "task|ci-escalation-verify"
        try {
            check(prefs.edit().clear().putString(key,
                """{"notificationToVoiceMinutes":10,"voiceToAlarmMinutes":15,"alarmToCriticalMinutes":30}"""
            ).commit())
            val first = requireNotNull(TrustCreatorEscalationInventory.inspect(app))
            assertEquals(TrustBackupDomain.CREATOR_ESCALATION_TIMINGS, first.domain)
            assertEquals(1, first.records)
            assertEquals(first.sha256,
                requireNotNull(TrustCreatorEscalationInventory.inspect(app)).sha256)
            assertEquals(1, prefs.all.size)
            assertTrue(prefs.contains(key))
            check(prefs.edit().putString(key,
                """{"notificationToVoiceMinutes":"bad","voiceToAlarmMinutes":15,"alarmToCriticalMinutes":30}"""
            ).commit())
            assertThrows(IllegalArgumentException::class.java) {
                TrustCreatorEscalationInventory.inspect(app)
            }
            assertTrue("Rejected inventory must not rewrite the rule",
                prefs.getString(key, "")!!.contains("bad"))
        } finally {
            restore(prefs, before)
        }
        assertEquals(before, prefs.all.toMap())
    }
}

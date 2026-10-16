package com.framebynavin.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V158QuietHoursSettingsInstrumentedTest {
    @Test fun existingBackupsDefaultQuietHoursOffAndNewSettingsRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CreatorOsSettingsStore(context)
        val original = store.exportJson()
        try {
            // New fields are optional so older valid Backlot backups keep their meaning.
            val oldBackup = JSONObject(original)
                .remove("quietHoursEnabled")
                .remove("quietHoursStartMinute")
                .remove("quietHoursEndMinute").toString()
            val upgraded = store.importJson(oldBackup)
            assertFalse(upgraded.quietHoursEnabled)
            assertEquals(22 * 60, upgraded.quietHoursStartMinute)
            assertEquals(8 * 60, upgraded.quietHoursEndMinute)

            store.setQuietHoursWindow(21 * 60, 7 * 60)
            store.setQuietHoursEnabled(true)
            val snapshot = store.snapshot()
            assertTrue(snapshot.quietHoursEnabled)
            assertEquals(21 * 60, snapshot.quietHoursStartMinute)
            assertEquals(7 * 60, snapshot.quietHoursEndMinute)

            val persisted = JSONObject(store.exportJson())
            assertTrue(persisted.getBoolean("quietHoursEnabled"))
            assertEquals(21 * 60, persisted.getInt("quietHoursStartMinute"))
            assertEquals(7 * 60, persisted.getInt("quietHoursEndMinute"))

            val reimported = store.importJson(persisted.toString())
            assertEquals(snapshot.quietHoursEnabled, reimported.quietHoursEnabled)
            assertEquals(snapshot.quietHoursStartMinute, reimported.quietHoursStartMinute)
            assertEquals(snapshot.quietHoursEndMinute, reimported.quietHoursEndMinute)
        } finally {
            store.importJson(original)
        }
    }
}

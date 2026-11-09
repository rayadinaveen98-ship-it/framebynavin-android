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
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator fixture only: no migration, cloud or reminder changes. */
@RunWith(AndroidJUnit4::class)
class TrustT1B4SavedPreferencesDeviceTest {
    private fun restore(prefs: SharedPreferences, old: Map<String, *>): Unit {
        val editor = prefs.edit().clear()
        old.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> {
                    require(value.all { it is String })
                    editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
                else -> error("Unknown disposable preference type")
            }
        }
        check(editor.commit())
    }

    @Test fun savedRoutineAndDirectorThemeYieldExactReadOnlyEvidence() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val routine = app.getSharedPreferences(
            "creator_automation_preferences_v17", Context.MODE_PRIVATE,
        )
        val appearance = app.getSharedPreferences(
            "framebynavin_visual_experience", Context.MODE_PRIVATE,
        )
        val oldRoutine = routine.all.toMap()
        val oldAppearance = appearance.all.toMap()
        try {
            check(routine.edit().clear()
                .putBoolean("daily_brief", true)
                .putBoolean("weekly_review", false).commit())
            check(appearance.edit().clear()
                .putString("theme", "DIRECTORS_CUT")
                .putBoolean("launch_sound", true).commit())

            val routineBefore = routine.all.toMap()
            val appearanceBefore = appearance.all.toMap()
            val routineEvidence = requireNotNull(TrustRoutinePreferencesInventory.inspect(app))
            val themeEvidence = requireNotNull(TrustAppearancePreferencesInventory.inspect(app))
            assertEquals(2, routineEvidence.records)
            assertEquals(2, themeEvidence.records)
            assertEquals(routineBefore, routine.all.toMap())
            assertEquals(appearanceBefore, appearance.all.toMap())
            assertFalse(themeEvidence.toString().contains("DIRECTORS_CUT"))
            assertFalse(routineEvidence.toString().contains("daily_brief"))

            check(appearance.edit().putBoolean("launch_sound", false).commit())
            assertNotEquals(themeEvidence.sha256,
                requireNotNull(TrustAppearancePreferencesInventory.inspect(app)).sha256)
            check(appearance.edit().putString("theme", "unrecognized").commit())
            assertThrows(IllegalArgumentException::class.java) {
                TrustAppearancePreferencesInventory.inspect(app)
            }
            check(routine.edit().putString("daily_brief", "corrupt").commit())
            assertThrows(IllegalStateException::class.java) {
                TrustRoutinePreferencesInventory.inspect(app)
            }
        } finally {
            restore(routine, oldRoutine)
            restore(appearance, oldAppearance)
        }
        assertEquals(oldRoutine, routine.all.toMap())
        assertEquals(oldAppearance, appearance.all.toMap())
    }
}

package com.framebynavin.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only. No task rows, cloud accounts or Personal reminders modified. */
@RunWith(AndroidJUnit4::class)
class V276CreatorDefaultMusicDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "backlot_creator_music_device_only_v1"

    @Before fun prepare() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun defaultAppliesToExistingAndNewProjectsWithoutPerProjectOverrides() {
        val store = CreatorMusicLocalStore(context)
        assertEquals(ReminderMusicMode.PIANO, store.get("project:a").mode)
        store.setDefaultForAll(ReminderMusicChoice(ReminderMusicMode.CUSTOM, "content://test/picked_music"))
        assertEquals(ReminderMusicMode.CUSTOM, store.get("project:a").mode)
        assertEquals(ReminderMusicMode.CUSTOM, CreatorMusicLocalStore(context).get("project:new").mode)
        assertFalse(store.hasOverride("project:a"))
    }

    @Test fun applyAllReplacesPreviousSoundtrackOverridesOnly() {
        val store = CreatorMusicLocalStore(context)
        store.put("project:a", ReminderMusicChoice(ReminderMusicMode.OFF))
        assertTrue(store.hasOverride("project:a"))
        store.setDefaultForAll(ReminderMusicChoice(ReminderMusicMode.PIANO))
        assertFalse(store.hasOverride("project:a"))
        assertEquals(ReminderMusicMode.PIANO, store.get("project:a").mode)
        store.put("project:a", ReminderMusicChoice(ReminderMusicMode.OFF))
        assertTrue(store.hasOverride("project:a"))
        assertEquals(ReminderMusicMode.PIANO, store.get("project:b").mode)
    }
}

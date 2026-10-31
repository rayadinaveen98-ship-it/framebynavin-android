package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic project only. Never accesses a real user cloud account. */
@RunWith(AndroidJUnit4::class)
class V273CreatorMusicLocalTest {
    @Test fun creatorMusicPreferencesRemainDeviceLocalAndReopenCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = "v273:qa-creator-synthetic"
        val store = CreatorMusicLocalStore(context)
        try {
            store.remove(id)
            assertEquals(ReminderMusicMode.PIANO, store.get(id).mode)
            val song = "content://com.backlot.synthetic/audio/1"
            store.put(id, ReminderMusicChoice(ReminderMusicMode.CUSTOM, song))
            assertEquals(ReminderMusicMode.CUSTOM, CreatorMusicLocalStore(context).get(id).mode)
            assertEquals(song, CreatorMusicLocalStore(context).get(id).uri)
            store.put(id, ReminderMusicChoice(ReminderMusicMode.OFF))
            assertEquals(ReminderMusicMode.OFF, store.get(id).mode)
            assertEquals("", store.get(id).uri)
        } finally {
            store.remove(id)
        }
    }

    @Test fun unsafeCreatorIdentifierCannotWriteLocalPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThrows(IllegalArgumentException::class.java) {
            CreatorMusicLocalStore(context).put("../abuse", ReminderMusicChoice())
        }
    }
}

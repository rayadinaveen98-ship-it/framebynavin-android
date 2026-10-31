package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** SAF URI requirements enforced before MediaPlayer sees a source. */
class V273MusicChoiceTest {
    @Test fun pianoAndOffRequireNoDocument() {
        assertEquals(ReminderMusicMode.PIANO, ReminderMusicChoice().checked().mode)
        assertEquals(ReminderMusicMode.OFF, ReminderMusicChoice(ReminderMusicMode.OFF).checked().mode)
    }

    @Test fun customRequiresPersistableLocalContentUri() {
        assertEquals("content://com.android.providers.media.documents/document/audio%3A42",
            ReminderMusicChoice(ReminderMusicMode.CUSTOM,
                "content://com.android.providers.media.documents/document/audio%3A42").checked().uri)
        for (invalid in listOf("", "/sdcard/music.mp3", "file:///sdcard/Music/song.mp3",
            "https://example.com/song.mp3", "content://provider/\nsecret", "content://" + "x".repeat(4096))) {
            assertThrows(IllegalArgumentException::class.java) {
                ReminderMusicChoice(ReminderMusicMode.CUSTOM, invalid).checked()
            }
        }
    }
}

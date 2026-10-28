package com.framebynavin.app.personal

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V226PersonalBackupPolicyTest {
    private val oldToken = "b6a4e3af-7a2b-4b8d-b039-76b91e3406a0"

    private fun sample() = PersonalReminder(
        id = "personal:v226",
        title = "Do not expose my title on Creator backups",
        notes = "Private details",
        startDate = LocalDate.of(2027, 2, 1),
        time = LocalTime.of(9, 30),
        pendingOccurrenceMillis = 1_810_000_000_000L,
        pendingFireAtMillis = 1_810_000_050_000L,
        pendingToken = oldToken,
        presentedToken = oldToken,
        snoozedUntilMillis = 1_810_000_050_000L,
        lastAcknowledgedOccurrenceMillis = 1_700_000_000_000L,
    )

    @Test fun importedPersonalBackupNeverCarriesDeviceLeaseOrPostedNotification() {
        val cleaned = PersonalBackupPolicy.resetDeviceLeases(listOf(sample())).single()
        assertEquals("", cleaned.pendingToken)
        assertEquals("", cleaned.presentedToken)
        assertEquals(0L, cleaned.pendingOccurrenceMillis)
        assertEquals(0L, cleaned.pendingFireAtMillis)
        assertEquals(0L, cleaned.snoozedUntilMillis)
        assertEquals(sample().lastAcknowledgedOccurrenceMillis,
            cleaned.lastAcknowledgedOccurrenceMillis)
        assertEquals(sample().title, cleaned.title)
        assertEquals(sample().notes, cleaned.notes)
        assertEquals(sample().id, cleaned.id)
    }

    @Test fun restoringAlreadyCleanRemindersIsDeterministic() {
        val result = PersonalBackupPolicy.resetDeviceLeases(listOf(sample()))
        assertEquals(result, PersonalBackupPolicy.resetDeviceLeases(result))
    }

    @Test fun streamingReaderRejectsOversizedUntrustedSafSource() {
        val data = object : InputStream() {
            private var bytes = 0
            override fun read(): Int = if (bytes++ <= 8_000_000) 'A'.code else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (bytes > 8_000_000) return -1
                val n = minOf(len, 8192)
                java.util.Arrays.fill(b, off, off + n, 'A'.code.toByte())
                bytes += n
                return n
            }
        }
        val failure = runCatching { PersonalBackupIo.readUtf8(data) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun streamingReaderKeepsUnicodeExact() {
        val original = "telugu తెలుగు reminders • 👋"
        val parsed = PersonalBackupIo.readUtf8(
            ByteArrayInputStream(original.toByteArray(Charsets.UTF_8))
        )
        assertEquals(original, parsed)
        assertFalse(parsed.contains("�"))
    }
}

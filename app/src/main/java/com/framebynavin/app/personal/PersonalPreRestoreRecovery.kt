package com.framebynavin.app.personal

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID

/**
 * Separate, offline recovery snapshot of the Personal records immediately
 * before an approved replacement. DataStore's one-revision undo is insufficient:
 * post-import recurrence re-arming writes again and rotates that revision away.
 *
 * Files remain app-private, distinct from Creator restore and cloud vaults.
 */
internal class PersonalPreRestoreRecovery(context: Context) {
    private val root = File(
        context.applicationContext.filesDir, "personal_backup_recovery_v226",
    )

    fun retain(records: List<PersonalReminder>): File {
        val raw = PersonalReminderCodec.encode(records)
        require(root.isDirectory || root.mkdirs()) {
            "Could not prepare Personal recovery directory; nothing was restored"
        }
        val file = File(root, "pre-personal-${UUID.randomUUID()}.json")
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(raw.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
        // Do not remove the immediately previous records to save space. Keep
        // five recovery revisions; cleanup failure is non-fatal after commit.
        runCatching {
            copies().drop(5).forEach { old -> AtomicFile(old).delete() }
        }
        return file
    }

    fun latestVerified(): String? {
        for (file in copies()) {
            val raw = runCatching {
                AtomicFile(file).openRead().use(PersonalBackupIo::readUtf8)
            }.getOrNull() ?: continue
            if (runCatching { PersonalReminderCodec.decode(raw) }.isSuccess) return raw
        }
        return null
    }

    fun count(): Int = copies().size

    private fun copies(): List<File> =
        root.listFiles().orEmpty()
            .filter { it.isFile && it.name.matches(Regex("pre-personal-[a-f0-9-]{36}\\.json")) }
            .sortedByDescending { it.lastModified() }
}

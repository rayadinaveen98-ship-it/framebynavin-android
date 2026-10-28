package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.widget.V224WidgetUpdater
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.CancellationException

/** Never add Personal records to CreatorBackupManager or DriveVault. */
internal data class PersonalBackupPreview(
    val reminderCount: Int,
    val activeCount: Int,
)

internal data class PersonalBackupRestoreReceipt(
    val importedCount: Int,
    val recoveryAttentionNeeded: Boolean,
)

/**
 * Validate the entire Personal payload before changing anything. Replacement
 * is one atomic DataStore edit, with the previous revision retained by
 * PersonalReminderStore. Imported delivery leases are device-local and cannot
 * be replayed on this or another phone.
 */
internal class PersonalBackupTransfer(context: Context) {
    private val app = context.applicationContext
    private val store = PersonalReminderStore(app)
    private val delivery = PersonalDeliveryCoordinator(app)
    private val retained = PersonalPreRestoreRecovery(app)

    suspend fun export(): String = store.exportPersonalJson()

    fun preview(raw: String): PersonalBackupPreview {
        val imported = PersonalReminderCodec.decode(raw)
        return PersonalBackupPreview(imported.size, imported.count { !it.paused })
    }

    fun previousRecoveryPreview(): PersonalBackupPreview? =
        retained.latestVerified()?.let(::preview)

    suspend fun restorePreviousApproved(): PersonalBackupRestoreReceipt {
        val previous = requireNotNull(retained.latestVerified()) {
            "No verified Personal pre-restore copy is available"
        }
        return restoreApproved(previous)
    }

    suspend fun restoreApproved(raw: String): PersonalBackupRestoreReceipt {
        val imported = PersonalReminderCodec.decode(raw)
        val clean = PersonalBackupPolicy.resetDeviceLeases(imported)
        var previous = emptyList<PersonalReminder>()
        // Capture the actual previous revision inside the same serialized
        // DataStore mutation. A racing edit cannot escape the recovery copy.
        store.mutate { existing ->
            // Persist a verified, independent undo image BEFORE changing the
            // atomic DataStore primary. Failure aborts the entire replacement.
            retained.retain(existing)
            previous = existing
            clean
        }
        // At this point the data is already committed. Cleanup and scheduling
        // failures are reported separately; never claim the whole restore
        // failed (which might encourage the user to import a second time).
        var attention = false
        (previous.map { it.id } + clean.map { it.id }).toSet().forEach { id ->
            try {
                delivery.cancelPending(id)
                PersonalPlaybackService.stopForReminder(id)
                PersonalReminderNotifications.cancel(app, id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                attention = true
                android.util.Log.w("BacklotPersonal", "Post-restore reminder cleanup deferred", error)
            }
        }
        try {
            delivery.reconcile()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            attention = true
            android.util.Log.w("BacklotPersonal", "Post-restore reschedule deferred", error)
        }
        V224WidgetUpdater.refresh(app)
        return PersonalBackupRestoreReceipt(clean.size, attention)
    }
}

internal object PersonalBackupPolicy {
    fun resetDeviceLeases(records: List<PersonalReminder>): List<PersonalReminder> =
        records.map { item ->
            item.copy(
                pendingOccurrenceMillis = 0L,
                pendingFireAtMillis = 0L,
                pendingToken = "",
                presentedToken = "",
                snoozedUntilMillis = 0L,
            ).checked()
        }
}

/**
 * Bounded streaming read for untrusted Android document-provider files.
 * A malformed multi-GB file cannot exhaust Backlot's memory before checksum
 * and schema validation are even attempted.
 */
internal object PersonalBackupIo {
    private const val MAX_BYTES = 8_000_000

    fun readUtf8(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            require(output.size() + count <= MAX_BYTES) {
                "The Personal backup is larger than the allowed safety limit"
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }
}

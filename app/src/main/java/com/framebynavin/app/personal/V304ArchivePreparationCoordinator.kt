package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only V304 orchestration path: authenticated selected ZIP, verified
 * existing Personal snapshot, zero-ID-conflict plan, copied private PREP
 * images, and a *fresh* destination revision check.
 *
 * Live DataStore, Room, Android alarms and photos remain untouched.
 * No UI currently invokes this; explicit owner request must be wired later.
 */
internal class V304ArchivePreparationCoordinator(context: Context) {
    private val app = context.applicationContext
    private val reader = V301LocalRecoveryReader(app)
    private val preparations = V304RestorePreparationStore(
        File(app.filesDir, "backlot-personal-restore-preparation-v1"),
    )

    suspend fun prepareOwnerRequested(
        encryptedArchive: InputStream,
        password: CharArray,
        ownerRequested: Boolean,
    ): V304PreparedTransfer = withContext(Dispatchers.IO) {
        require(ownerRequested) {
            "A preparation must be initiated by an explicit owner action"
        }
        val local = reader.read()
        V300EncryptedPersonalArchive.stageAndVerify(
            encryptedArchive, password, app.cacheDir,
        ).use { authenticated ->
            val plan = V304RestorePlanPolicy.propose(
                local, authenticated.components,
                authenticated.photos.map { it.photo },
            )
            // Do not write a preparation based on a stale preview.
            V304RestorePlanPolicy.checkStillSame(plan, reader.read())
            val result = preparations.prepare(
                plan, authenticated.photos, requestedByOwner = true,
            )
            try {
                // The archived, staged bytes are valid. Check the ACTIVE phone
                // once more so changes made while copying are not accepted.
                V304RestorePlanPolicy.checkStillSame(plan, reader.read())
                // Reopen and SHA-256 check every private preparation file.
                check(preparations.inspect(result.transactionId) == result) {
                    "Prepared image inventory does not match its journal"
                }
                result
            } catch (cancel: CancellationException) {
                preparations.discard(result.transactionId)
                throw cancel
            } catch (failure: Throwable) {
                preparations.discard(result.transactionId)
                throw failure
            }
        }
    }

    /** User-selected cleanup only; no live owner data is ever removed. */
    suspend fun discardPreparation(transactionId: String) = withContext(Dispatchers.IO) {
        preparations.discard(transactionId)
    }

    suspend fun verifyPreparation(transactionId: String): V304PreparedTransfer =
        withContext(Dispatchers.IO) {
            preparations.inspect(transactionId)
        }
}

package com.framebynavin.app.personal

import android.content.Context
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The first end-to-end *read-only* preview path: decrypt/authenticate entire
 * selected archive -> compare all eight domains to verified device state ->
 * close and wipe staged photo files. Does not modify the backup source file.
 *
 * The caller must own/close its InputStream and erase its password buffer.
 * The result cannot authorize restore; the verified stage is destroyed on exit.
 */
internal class V301ArchiveConflictPreview(context: Context) {
    private val app = context.applicationContext
    private val reader = V301LocalRecoveryReader(app)

    suspend fun check(
        input: InputStream,
        password: CharArray,
    ): V301RestoreDryRunReport = withContext(Dispatchers.IO) {
        // The local data is read without writes; process or user actions could
        // still change it later. Never treat these counts as a restore lock.
        val local = reader.read()
        V300EncryptedPersonalArchive.stageAndVerify(
            input, password, app.cacheDir,
        ).use { verified ->
            V301RestoreDryRun.compare(
                local, verified.components,
                verified.photos.map { it.photo },
            )
        }
    }
}

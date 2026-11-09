package com.framebynavin.app.cloud

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One process-wide AND one OS file lock serialize T1C3 preview/recovery.
 * The file lock keeps another Backlot process from deleting active owner bytes.
 * Only T1C3 code participates; it is NOT a global Creator/Personal data lock.
 */
internal object TrustT1C3OperationGate {
    private val processLock = Mutex()
    private const val ROOT = "trust-t1c3-local-review-v1"

    suspend fun <T> withExclusive(context: Context, action: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            processLock.withLock {
                val root = File(context.applicationContext.noBackupFilesDir, ROOT)
                check(root.isDirectory || root.mkdirs()) {
                    "Private review lock directory cannot be prepared"
                }
                check(!java.nio.file.Files.isSymbolicLink(root.toPath())) {
                    "Private review root cannot be a symlink"
                }
                val lockFile = File(root, "operation.lock")
                FileOutputStream(lockFile, true).channel.use { channel ->
                    val lock = channel.tryLock()
                        ?: error("An owner preview/recovery is already active in another process")
                    lock.use { action() }
                }
            }
        }
}

package com.framebynavin.app.cloud

import java.io.File

/**
 * Same-process registry of currently open temporary owner plaintext directories.
 * Crash clears this in-memory registry, allowing the NEXT EXPLICIT preview
 * to sweep abandoned directories. It is not a multi-process recovery lock.
 */
internal object TrustTemporaryStageRegistry {
    private val activePaths = HashSet<String>()

    @Synchronized fun activate(directory: File) {
        check(activePaths.add(directory.canonicalPath)) {
            "Temporary owner staging directory is already active"
        }
    }

    @Synchronized fun deactivate(directory: File) {
        activePaths.remove(directory.canonicalPath)
    }

    @Synchronized fun isActive(directory: File): Boolean =
        directory.canonicalPath in activePaths
}

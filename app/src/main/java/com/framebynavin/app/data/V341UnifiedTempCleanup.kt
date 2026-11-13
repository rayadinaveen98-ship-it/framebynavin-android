package com.framebynavin.app.data

import java.io.File
import java.nio.file.Files

/**
 * Post-crash cleanup for ONLY V341-owned private staging directories.
 * Never touches Creator's real media, Personal journals, recovery escrows,
 * existing backups, or in-flight temporary work less than 24 hours old.
 */
internal object V341UnifiedTempCleanup {
    private const val MIN_AGE_MILLIS = 24L * 60L * 60L * 1000L
    private val OWN_PREFIXES = listOf("backlot-unified-export-", "backlot-unified-stage-")

    fun prune(cacheDir: File, nowMillis: Long = System.currentTimeMillis()): Int {
        if (!cacheDir.isDirectory || nowMillis <= 0L) return 0
        var cleared = 0
        cacheDir.listFiles().orEmpty().forEach { child ->
            if (OWN_PREFIXES.none { child.name.startsWith(it) } ||
                !child.isDirectory ||
                Files.isSymbolicLink(child.toPath()) ||
                child.canonicalFile.parentFile != cacheDir.canonicalFile) return@forEach
            val timestamp = child.lastModified()
            if (timestamp <= 0L || timestamp > nowMillis ||
                nowMillis - timestamp < MIN_AGE_MILLIS) return@forEach
            if (child.deleteRecursively()) cleared++
        }
        return cleared
    }
}

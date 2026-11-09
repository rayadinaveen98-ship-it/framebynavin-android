package com.framebynavin.app.cloud

import android.content.Context
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Read-only wrt ALL Creator/Personal records, reminders, alarms, photos,
 * cloud accounts and older encrypted reviews.
 *
 * Runs on MainActivity startup under TrustT1C3OperationGate. Removes ONLY
 * known abandoned T1C1/T1C2 plaintext staging and T1C3 pending encrypted
 * candidate names. It is deliberately NOT a user data reset/restore.
 */
internal object TrustT1C3StartupRecovery {
    internal data class Result(
        val abandonedPlaintextDirectoriesCleaned: Int,
        val pendingEncryptedAttemptsCleaned: Int,
        val hadInterruptedJournal: Boolean,
    )

    private val frozenName = Regex("trust-t1c2-owner-bytes-[A-Za-z0-9_-]{1,60}")
    private val decryptedName = Regex("backlot-trust-check-[A-Za-z0-9_-]{1,60}")
    private val pendingName = Regex(
        "pending-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.enc(\\.new|\\.bak)?",
    )
    private const val MAX_ABANDONED = 256
    private const val MAX_NODES_PER_DIRECTORY = 1100

    suspend fun recoverOnMainActivityStartup(context: Context): Result =
        TrustT1C3OperationGate.withExclusive(context) {
            sweepWithinExclusive(context.applicationContext)
        }

    /** Also used before a fresh partial preview, with gate already held. */
    internal fun sweepWithinExclusive(context: Context): Result {
        val app = context.applicationContext
        val root = File(app.noBackupFilesDir, "trust-t1c3-local-review-v1")
        val pending = File(root, "pending")
        if (root.exists()) check(!Files.isSymbolicLink(root.toPath())) {
            "Private review root cannot be a symlink"
        }
        val marker = TrustT1C3RecoveryJournal.inspect(root)
        if (pending.exists()) {
            require(pending.isDirectory && !Files.isSymbolicLink(pending.toPath())) {
                "Unrecognized private pending backup directory"
            }
        }

        val candidates = app.cacheDir.listFiles().orEmpty().filter {
            frozenName.matches(it.name) || decryptedName.matches(it.name)
        }
        val pendingCandidates = pending.listFiles().orEmpty()
        require(candidates.size + pendingCandidates.size <= MAX_ABANDONED) {
            "Too many abandoned backup artifacts; preserve for review"
        }

        // Preflight EVERY candidate before removing ANY. Reject links, paths
        // outside the owned cache, unsupported types or untracked pending
        // filenames. No entry under the verified/ directory is considered.
        val cacheRoot = app.cacheDir.canonicalFile.toPath()
        candidates.forEach { file ->
            require(file.parentFile.canonicalFile.toPath() == cacheRoot &&
                !TrustTemporaryStageRegistry.isActive(file)) {
                "An owner source stage is still active; cleanup deferred"
            }
            require(file.isDirectory && !Files.isSymbolicLink(file.toPath())) {
                "Unrecognized private staging directory"
            }
            preflightTree(file.toPath())
        }
        pendingCandidates.forEach { file ->
            require(pendingName.matches(file.name) &&
                file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.parentFile.canonicalFile == pending.canonicalFile) {
                "Unexpected pending encrypted preview; no deletion performed"
            }
        }

        candidates.forEach { safeDeleteTree(it.toPath()) }
        pendingCandidates.forEach { file ->
            require(file.delete() || !file.exists()) {
                "Unable to remove abandoned pending encrypted preview"
            }
        }
        // Journal is a privacy-preserving breadcrumb only. If it was
        // corrupted, inspect() failed above and no candidates were deleted.
        if (marker != null) TrustT1C3RecoveryJournal.clear(root)
        return Result(candidates.size, pendingCandidates.size, marker != null)
    }

    private fun preflightTree(root: Path) {
        var visited = 0
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                directory: Path, attributes: BasicFileAttributes,
            ): FileVisitResult {
                require(!attributes.isSymbolicLink && attributes.isDirectory &&
                    ++visited <= MAX_NODES_PER_DIRECTORY) {
                    "Unsafe or oversized temporary backup directory"
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(
                file: Path, attributes: BasicFileAttributes,
            ): FileVisitResult {
                require(!attributes.isSymbolicLink && attributes.isRegularFile &&
                    ++visited <= MAX_NODES_PER_DIRECTORY) {
                    "Unexpected temporary backup file"
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun safeDeleteTree(root: Path) {
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                check(!attributes.isSymbolicLink && attributes.isRegularFile)
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(
                directory: Path, failure: java.io.IOException?,
            ): FileVisitResult {
                if (failure != null) throw failure
                Files.delete(directory)
                return FileVisitResult.CONTINUE
            }
        })
    }
}

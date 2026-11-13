package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest

/**
 * Read-only byte inventory of the live Space photo folder after a crashed
 * combined restore. This is run ONLY inside V353's exclusive Personal+Creator
 * forensic inspection scopes. It does not open Room, remove orphan files,
 * re-encode JPEGs, or rename interrupted *.pending files.
 *
 * Matching live rows and their current JPEG bytes is still NOT proof that
 * original/target cross-domain media recovery is safe. Those encrypted owner
 * snapshots need a separate inverse/forward media plan.
 */
internal class V354UnifiedLiveSpacePhotoAudit(
    context: Context,
    private val folder: File = File(
        context.applicationContext.filesDir, "backlot-personal-space-photos",
    ),
) {
    fun inspect(photos: List<V290SpacePhotoEntity>): V354SpacePhotoAuditResult {
        require(photos.size <= 500) { "Space photo count exceeds bounded forensic inventory" }
        val expected = HashMap<String, V290SpacePhotoEntity>()
        for (row in photos) {
            V290PhotoTimelinePolicy.validate(row)
            val key = "${row.spaceId}/${row.fileName}"
            require(expected.put(key, row) == null) {
                "Duplicate Space photo reference during live recovery inspection"
            }
        }
        if (!Files.exists(folder.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return V354SpacePhotoAuditResult(
                rows = photos.size,
                matchedBytes = 0,
                missing = photos.size,
                modified = 0,
                unreferenced = 0,
                unsafeOrUnverifiable = 0,
            )
        }
        if (Files.isSymbolicLink(folder.toPath()) ||
            !Files.isDirectory(folder.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return V354SpacePhotoAuditResult(
                photos.size, 0, 0, 0, 0, 1,
            )
        }

        val discovered = HashSet<String>()
        val stack = ArrayDeque<Pair<File, String>>()
        stack.addLast(folder to "")
        var unreferenced = 0
        var unsafe = 0
        var inspected = 0
        while (stack.isNotEmpty()) {
            val (next, relative) = stack.removeLast()
            val children = next.listFiles()
            if (children == null) {
                unsafe++
                continue
            }
            for (child in children) {
                inspected++
                check(inspected <= 2_000) {
                    "Unexpectedly large live Space photo directory; refuse forensic conclusions"
                }
                val rel = if (relative.isEmpty()) child.name
                    else "$relative/${child.name}"
                when {
                    Files.isSymbolicLink(child.toPath()) -> unsafe++
                    Files.isDirectory(child.toPath(), LinkOption.NOFOLLOW_LINKS) ->
                        stack.addLast(child to rel)
                    Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) -> {
                        if (!expected.containsKey(rel)) unreferenced++
                        else discovered.add(rel)
                    }
                    else -> unsafe++
                }
            }
        }
        var matched = 0
        var modified = 0
        for ((path, row) in expected) {
            if (path !in discovered) continue
            val file = File(folder, path)
            if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                Files.isSymbolicLink(file.toPath()) ||
                file.length() !in 1..(12L * 1024L * 1024L)) {
                modified++
                continue
            }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { source ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val size = source.read(buffer)
                    if (size < 0) break
                    if (size > 0) digest.update(buffer, 0, size)
                }
            }
            val calculated = digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
            if (calculated == row.sha256Hex) matched++ else modified++
        }
        return V354SpacePhotoAuditResult(
            rows = photos.size,
            matchedBytes = matched,
            missing = photos.size - discovered.size,
            modified = modified,
            unreferenced = unreferenced,
            unsafeOrUnverifiable = unsafe,
        )
    }
}

internal data class V354SpacePhotoAuditResult(
    val rows: Int,
    val matchedBytes: Int,
    val missing: Int,
    val modified: Int,
    val unreferenced: Int,
    val unsafeOrUnverifiable: Int,
) {
    init {
        require(listOf(
            rows, matchedBytes, missing, modified, unreferenced, unsafeOrUnverifiable,
        ).all { it >= 0 } && matchedBytes + missing + modified <= rows) {
            "Inconsistent read-only Space photo forensic summary"
        }
    }

    val currentPhotoFilesMatchTheirLiveRows: Boolean get() =
        matchedBytes == rows && missing == 0 && modified == 0 &&
            unreferenced == 0 && unsafeOrUnverifiable == 0

    /** Original-vs-target media transaction recovery is NOT established here. */
    val authorizesCombinedMediaRollback: Boolean get() = false
}

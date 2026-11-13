package com.framebynavin.app.ui

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Package I: read-only safety inspection BEFORE asking Android to clear all
 * Backlot app-private data. An unfinished restore is not an ordinary backup.
 * Never delete or "repair" its encrypted original, media, or journals here.
 *
 * This class does not run a reset, alter alarm schedules, or create files.
 * It deliberately fails closed for unreadable private storage.
 */
internal data class V383DeviceResetInspection(
    val storageReadable: Boolean,
    val blockingRecoveryMarkers: List<String>,
) {
    val mayOfferReset: Boolean get() = storageReadable && blockingRecoveryMarkers.isEmpty()
}

internal object V383UnifiedDeviceResetPreflight {
    // Only ACTIVE owner recovery / rollback evidence. Archived history is NOT
    // counted: the owner may still intentionally clear completed old history.
    private val ACTIVE_ROOTS = listOf(
        "backlot-unified-cross-domain-restore-journal-v1",
        "backlot-unified-owner-recovery-decision-v1",
        "backlot-unified-owner-recovery-escrow-v1",
        "backlot-unified-personal-v2-handoff-v1",
        "backlot-unified-personal-v2-reverse-actions-v1",
        "backlot-unified-verified-outcome-v1",
        "backlot-unified-final-release-v1",
        "backlot-personal-active-restore-transaction",
        "backlot-personal-active-restore-transaction-v2",
    )
    private val CREATOR_PENDING = listOf("pending.json", "pending.json.new", "pending.json.bak")

    fun inspect(filesDir: File): V383DeviceResetInspection = try {
        val root = filesDir.toPath()
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) ||
            Files.isSymbolicLink(root) || filesDir.list() == null
        ) {
            V383DeviceResetInspection(false, emptyList())
        } else {
            val blocking = ACTIVE_ROOTS.filter { name ->
                Files.exists(File(filesDir, name).toPath(), LinkOption.NOFOLLOW_LINKS)
            }.toMutableList()
            if (CREATOR_PENDING.any { name ->
                    Files.exists(
                        File(File(filesDir, "creator_restore_recovery_v181"), name).toPath(),
                        LinkOption.NOFOLLOW_LINKS,
                    )
                }
            ) {
                blocking.add("creator_restore_recovery_v181/pending")
            }
            V383DeviceResetInspection(true, blocking)
        }
    } catch (_: Exception) {
        V383DeviceResetInspection(false, emptyList())
    }
}

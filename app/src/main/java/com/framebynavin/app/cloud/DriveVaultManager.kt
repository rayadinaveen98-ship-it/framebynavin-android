package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.CreatorBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class DriveVaultManager(context: Context) {
    data class PreparedSnapshot(
        val payload: String,
        val preview: CreatorBackupManager.BackupPreview,
        val sha256: String,
    )

    private val backup = CreatorBackupManager(context.applicationContext)
    private val api = DriveVaultApi()

    suspend fun list(accessToken: String): List<DriveVaultRestorePoint> = withContext(Dispatchers.IO) {
        api.list(accessToken)
    }

    suspend fun prepare(): PreparedSnapshot = withContext(Dispatchers.IO) {
        val payload = backup.createBackup()
        val preview = backup.validate(payload)
        PreparedSnapshot(payload, preview, sha256(payload))
    }

    suspend fun backupNow(accessToken: String, allowEmptyAgainstMeaningfulHistory: Boolean = false): DriveVaultRestorePoint =
        withContext(Dispatchers.IO) {
            val prepared = prepare()
            val existing = api.list(accessToken)
            if (!allowEmptyAgainstMeaningfulHistory && DriveVaultPolicy.shouldBlockEmptySnapshot(
                    prepared.preview.projectCount,
                    prepared.preview.ideaCount,
                    existing,
                )) {
                throw DriveVaultEmptySnapshotBlocked(
                    "This phone has no projects or ideas, while your Drive vault has creator work. Restore or explicitly review the existing history before creating an empty snapshot."
                )
            }
            api.upload(
                accessToken = accessToken,
                payload = prepared.payload,
                capturedAtMillis = prepared.preview.createdAtMillis,
                appVersion = BuildConfig.VERSION_NAME,
                projectCount = prepared.preview.projectCount,
                ideaCount = prepared.preview.ideaCount,
                weeklySlotCount = prepared.preview.weeklySlotCount,
                activeReminderCount = prepared.preview.activeReminderCount,
                sha256 = prepared.sha256,
            )
        }

    suspend fun restore(accessToken: String, point: DriveVaultRestorePoint): CreatorBackupManager.BackupPreview =
        withContext(Dispatchers.IO) {
            require(point.sha256.isNotBlank()) { "Drive snapshot is missing integrity metadata" }
            val payload = api.download(accessToken, point.fileId)
            require(sha256(payload).equals(point.sha256, ignoreCase = true)) { "Drive snapshot integrity check failed" }
            backup.validate(payload)
            backup.restore(payload)
        }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

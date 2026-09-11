package com.framebynavin.app.cloud

data class DriveVaultRestorePoint(
    val fileId: String,
    val name: String,
    val capturedAtMillis: Long,
    val appVersion: String,
    val projectCount: Int,
    val ideaCount: Int,
    val weeklySlotCount: Int,
    val activeReminderCount: Int,
    val sha256: String,
    val sizeBytes: Long,
) {
    val hasCreatorWork: Boolean get() = projectCount > 0 || ideaCount > 0
}

class DriveVaultHttpException(val statusCode: Int, override val message: String) : Exception(message)
class DriveVaultEmptySnapshotBlocked(message: String) : IllegalStateException(message)

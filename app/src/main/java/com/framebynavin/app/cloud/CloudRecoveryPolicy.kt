package com.framebynavin.app.cloud

object CloudRecoveryPolicy {
    fun hasCreatorWork(point: CloudRestorePoint): Boolean =
        point.projectCount > 0 || point.ideaCount > 0 || point.weeklySlotCount > 0 || point.activeReminderCount > 0

    fun recommended(points: List<CloudRestorePoint>): CloudRestorePoint? {
        val newestFirst = points.sortedByDescending { it.capturedAtMillis }
        return newestFirst.firstOrNull(::hasCreatorWork) ?: newestFirst.firstOrNull()
    }
}

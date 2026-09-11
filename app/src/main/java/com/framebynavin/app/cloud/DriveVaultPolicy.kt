package com.framebynavin.app.cloud

object DriveVaultPolicy {
    fun recommended(points: List<DriveVaultRestorePoint>): DriveVaultRestorePoint? {
        val ordered = points.sortedByDescending { it.capturedAtMillis }
        return ordered.firstOrNull { it.hasCreatorWork } ?: ordered.firstOrNull()
    }

    fun shouldBlockEmptySnapshot(
        projectCount: Int,
        ideaCount: Int,
        existing: List<DriveVaultRestorePoint>,
    ): Boolean = projectCount == 0 && ideaCount == 0 && existing.any { it.hasCreatorWork }
}

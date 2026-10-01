package com.framebynavin.app.data

/** Keeps account identity and creator setup separate while repairing blank legacy display names. */
object CreatorIdentityPolicy {
    fun resolvedDisplayName(
        localCreatorName: String,
        cachedAccountName: String,
        googleAccountName: String,
    ): String {
        val local = localCreatorName.trim()
        if (local.isNotBlank()) return local.take(40)
        return cachedAccountName.trim().ifBlank { googleAccountName.trim() }.take(40)
    }
}

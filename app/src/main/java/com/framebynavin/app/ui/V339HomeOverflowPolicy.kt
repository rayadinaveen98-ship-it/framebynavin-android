package com.framebynavin.app.ui

/**
 * Home header has one disclosure affordance. These are navigation actions,
 * not separate reminder schedulers, notification readers or photo stores.
 */
internal enum class V339HomeOverflowAction {
    NOTIFICATION_CENTER,
    SPARK,
    GALLERY,
}

internal object V339HomeOverflowPolicy {
    val actions: List<V339HomeOverflowAction> = listOf(
        V339HomeOverflowAction.NOTIFICATION_CENTER,
        V339HomeOverflowAction.SPARK,
        V339HomeOverflowAction.GALLERY,
    )

    /** Spark's existing owner preference remains authoritative. */
    fun enabled(action: V339HomeOverflowAction, sparkEnabled: Boolean): Boolean =
        action != V339HomeOverflowAction.SPARK || sparkEnabled

    /** Badge conveys an outstanding action or suggestion, never "unread". */
    fun hasIndicator(
        action: V339HomeOverflowAction,
        needsReminderAction: Boolean,
        hasSparkSuggestion: Boolean,
        sparkEnabled: Boolean,
    ): Boolean = when (action) {
        V339HomeOverflowAction.NOTIFICATION_CENTER -> needsReminderAction
        V339HomeOverflowAction.SPARK -> sparkEnabled && hasSparkSuggestion
        V339HomeOverflowAction.GALLERY -> false
    }
}

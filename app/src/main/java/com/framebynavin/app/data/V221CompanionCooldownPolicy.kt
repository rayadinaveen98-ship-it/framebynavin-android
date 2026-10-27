package com.framebynavin.app.data

/**
 * V221.2: a bounded, local-only way to avoid repeating a dismissed suggestion.
 * Dismissal is presentation state. It never changes a task, reminder or idea.
 */
internal data class V221CompanionDismissal(
    val decisionId: String,
    val dismissedAtMillis: Long,
    val expiresAtMillis: Long,
)

internal object V221CompanionCooldownPolicy {
    private const val HOUR = 60L * 60 * 1000
    internal const val DEFAULT_COOLDOWN_MILLIS = 12L * HOUR
    internal const val MAX_RECORDS = 80
    private const val CLOCK_ROLLBACK_TOLERANCE = 10L * 60 * 1000

    /** Invalid/expired values cannot suppress a suggestion indefinitely. */
    fun active(records: List<V221CompanionDismissal>, nowMillis: Long): Set<String> {
        if (nowMillis <= 0L) return emptySet()
        return records.asSequence()
            .filter { it.decisionId.isNotBlank() && it.decisionId.length <= 160 }
            .filter { it.dismissedAtMillis > 0L &&
                it.expiresAtMillis > it.dismissedAtMillis &&
                it.expiresAtMillis - it.dismissedAtMillis <= 24L * HOUR }
            .filter { nowMillis < it.expiresAtMillis &&
                nowMillis + CLOCK_ROLLBACK_TOLERANCE >= it.dismissedAtMillis }
            .map { it.decisionId }
            .toSet()
    }

    fun dismiss(
        records: List<V221CompanionDismissal>,
        decisionId: String,
        nowMillis: Long,
        durationMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    ): List<V221CompanionDismissal> {
        if (decisionId.isBlank() || decisionId.length > 160 || nowMillis <= 0L ||
            durationMillis !in (15L * 60 * 1000)..(24L * HOUR) ||
            nowMillis > Long.MAX_VALUE - durationMillis
        ) return records
        val current = records.filter {
            it.decisionId != decisionId &&
                it.dismissedAtMillis > 0L &&
                it.expiresAtMillis > nowMillis &&
                it.expiresAtMillis > it.dismissedAtMillis
        }
        return (current + V221CompanionDismissal(
            decisionId = decisionId,
            dismissedAtMillis = nowMillis,
            expiresAtMillis = nowMillis + durationMillis,
        )).takeLast(MAX_RECORDS)
    }
}

/**
 * One read-only bridge between V221.1's grounded evidence engine and V221.2's
 * quiet presentation rules. No mutation happens when selecting guidance.
 */
internal object V221CompanionBehaviorPolicy {
    fun select(
        context: V221CompanionContext,
        enabled: Boolean,
        dismissals: List<V221CompanionDismissal>,
    ): V221CompanionDecision? {
        if (!enabled) return null
        return V221CompanionDecisionEngine.choose(
            context.copy(
                suppressedDecisionIds = context.suppressedDecisionIds +
                    V221CompanionCooldownPolicy.active(dismissals, context.nowMillis),
            )
        )
    }
}

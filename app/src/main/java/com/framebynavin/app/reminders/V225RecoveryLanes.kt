package com.framebynavin.app.reminders

import kotlinx.coroutines.CancellationException

/**
 * Treat Creator, Personal, and Idea alarm recovery as independent lanes.
 * One store failing must never prevent later lanes from running. A failed
 * lane requests WorkManager retry; cancellation must propagate normally.
 */
internal data class V225RecoveryOutcome(
    val creatorRecovered: Boolean,
    val personalRecovered: Boolean,
    val ideasRecovered: Boolean,
) {
    val allSucceeded: Boolean get() = creatorRecovered && personalRecovered && ideasRecovered
}

internal object V225RecoveryLanes {
    suspend fun run(
        creator: suspend () -> Unit,
        personal: suspend () -> Unit,
        ideas: suspend () -> Unit,
    ): V225RecoveryOutcome {
        suspend fun attempt(block: suspend () -> Unit): Boolean =
            try {
                block()
                true
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                false
            }

        return V225RecoveryOutcome(
            creatorRecovered = attempt(creator),
            personalRecovered = attempt(personal),
            ideasRecovered = attempt(ideas),
        )
    }
}

package com.framebynavin.app.data

import java.util.UUID

/**
 * Phase 3C.1 pure clock/domain policy. NO project completion, reward, publication or reminder writes.
 *
 * Time accrues from Android's monotonic elapsed-realtime clock within one boot.
 * A reboot/monotonic reset pauses conservatively at the last persisted checkpoint;
 * it never treats wall-clock time away from the device as actual focused work.
 */
internal enum class CreatorFocusState { READY, RUNNING, PAUSED, FINISHED, DISCARDED }

internal data class CreatorFocusSession(
    val id: String,
    val projectId: String,
    val stageId: String,
    val targetMillis: Long,
    val createdAtWallMillis: Long,
    val state: CreatorFocusState = CreatorFocusState.READY,
    val accruedActiveMillis: Long = 0L,
    val segmentStartedElapsedMillis: Long = -1L,
    val segmentBootCount: Int = -1,
    val endedAtWallMillis: Long = 0L,
    val recoveredAfterReboot: Boolean = false,
) {
    fun checked(): CreatorFocusSession {
        require(id.isNotBlank() && projectId.isNotBlank() && stageId.isNotBlank()) {
            "Focus session needs distinct project, stage and session identity"
        }
        require(targetMillis in 60_000L..7_200_000L && targetMillis % 60_000L == 0L) {
            "Focus duration must be 1-120 whole minutes"
        }
        require(createdAtWallMillis > 0L && accruedActiveMillis in 0L..targetMillis) {
            "Invalid focus timestamp or accumulated duration"
        }
        if (state == CreatorFocusState.RUNNING) {
            require(segmentStartedElapsedMillis >= 0L && segmentBootCount >= 0)
        } else {
            require(segmentStartedElapsedMillis == -1L && segmentBootCount == -1)
        }
        if (state == CreatorFocusState.FINISHED || state == CreatorFocusState.DISCARDED) {
            require(endedAtWallMillis > 0L)
        } else {
            require(endedAtWallMillis == 0L)
        }
        return this
    }
}

internal object CreatorFocusSessionPolicy {
    val durationOptionsMinutes = listOf(15, 25, 45, 60)

    fun create(
        projectId: String,
        stageId: String,
        targetMinutes: Int,
        atWallMillis: Long,
        id: String = UUID.randomUUID().toString(),
    ): CreatorFocusSession {
        require(targetMinutes in durationOptionsMinutes)
        return CreatorFocusSession(
            id = id, projectId = projectId, stageId = stageId,
            targetMillis = targetMinutes * 60_000L,
            createdAtWallMillis = atWallMillis,
        ).checked()
    }

    /** Display time is derived rather than decrementing an in-memory 1-second Compose counter. */
    fun activeMillis(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): Long {
        session.checked()
        if (session.state != CreatorFocusState.RUNNING) return session.accruedActiveMillis
        if (bootCount != session.segmentBootCount ||
            elapsedMillis < session.segmentStartedElapsedMillis
        ) return session.accruedActiveMillis
        val runningSegment = (elapsedMillis - session.segmentStartedElapsedMillis)
            .coerceAtLeast(0L)
        return (session.accruedActiveMillis + runningSegment).coerceAtMost(session.targetMillis)
    }

    /**
     * On a different boot, do NOT extrapolate from wall time. The returned PAUSED session
     * retains verified work up to the last checkpoint and signals that some time may be missing.
     */
    fun recover(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): CreatorFocusSession {
        session.checked()
        if (session.state != CreatorFocusState.RUNNING) return session
        if (bootCount == session.segmentBootCount &&
            elapsedMillis >= session.segmentStartedElapsedMillis
        ) return session
        return session.copy(
            state = CreatorFocusState.PAUSED,
            segmentStartedElapsedMillis = -1L,
            segmentBootCount = -1,
            recoveredAfterReboot = true,
        ).checked()
    }

    /** User chooses Start/Resume explicitly; opening the Focus screen never starts work. */
    fun start(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): CreatorFocusSession {
        session.checked()
        require(session.state == CreatorFocusState.READY || session.state == CreatorFocusState.PAUSED)
        require(elapsedMillis >= 0L && bootCount >= 0)
        require(session.accruedActiveMillis < session.targetMillis) {
            "Duration reached; finish or discard instead"
        }
        return session.copy(
            state = CreatorFocusState.RUNNING,
            segmentStartedElapsedMillis = elapsedMillis,
            segmentBootCount = bootCount,
            recoveredAfterReboot = false,
        ).checked()
    }

    /** Writes a durable checkpoint for process recreation without claiming extra time. */
    fun checkpoint(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): CreatorFocusSession {
        val valid = recover(session, elapsedMillis, bootCount)
        if (valid.state != CreatorFocusState.RUNNING) return valid
        return valid.copy(
            accruedActiveMillis = activeMillis(valid, elapsedMillis, bootCount),
            segmentStartedElapsedMillis = elapsedMillis,
        ).checked()
    }

    fun pause(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): CreatorFocusSession {
        val current = recover(session, elapsedMillis, bootCount)
        if (current.state == CreatorFocusState.PAUSED) return current
        require(current.state == CreatorFocusState.RUNNING)
        return current.copy(
            state = CreatorFocusState.PAUSED,
            accruedActiveMillis = activeMillis(current, elapsedMillis, bootCount),
            segmentStartedElapsedMillis = -1L,
            segmentBootCount = -1,
        ).checked()
    }

    /** Finishing records minutes of work only. It never advances the project's workflow. */
    fun finish(
        session: CreatorFocusSession,
        elapsedMillis: Long,
        bootCount: Int,
        atWallMillis: Long,
    ): CreatorFocusSession {
        session.checked()
        require(session.state in setOf(
            CreatorFocusState.READY, CreatorFocusState.PAUSED, CreatorFocusState.RUNNING
        ))
        require(atWallMillis > 0L)
        val current = recover(session, elapsedMillis, bootCount)
        return current.copy(
            state = CreatorFocusState.FINISHED,
            accruedActiveMillis = activeMillis(current, elapsedMillis, bootCount),
            segmentStartedElapsedMillis = -1L,
            segmentBootCount = -1,
            endedAtWallMillis = atWallMillis,
        ).checked()
    }

    fun discard(session: CreatorFocusSession, atWallMillis: Long): CreatorFocusSession {
        session.checked()
        require(session.state in setOf(
            CreatorFocusState.READY, CreatorFocusState.PAUSED, CreatorFocusState.RUNNING
        ))
        require(atWallMillis > 0L)
        return session.copy(
            state = CreatorFocusState.DISCARDED,
            accruedActiveMillis = 0L,
            segmentStartedElapsedMillis = -1L,
            segmentBootCount = -1,
            endedAtWallMillis = atWallMillis,
        ).checked()
    }

    fun remainingMillis(session: CreatorFocusSession, elapsedMillis: Long, bootCount: Int): Long =
        (session.targetMillis - activeMillis(session, elapsedMillis, bootCount)).coerceAtLeast(0L)
}

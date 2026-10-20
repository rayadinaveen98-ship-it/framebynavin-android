package com.framebynavin.app.data

import java.util.concurrent.atomic.AtomicBoolean

/**
 * One save in flight per Content Workspace activity. Double taps on Save / Save & Next should
 * never issue competing stale-revision transactions or display a false conflict.
 *
 * Activity scope is intentional: saves for unrelated projects in other screens remain unaffected.
 */
class CreatorWorkspaceSaveGate {
    private val inFlight = AtomicBoolean(false)

    fun tryBegin(): Boolean = inFlight.compareAndSet(false, true)

    fun end() {
        inFlight.set(false)
    }
}

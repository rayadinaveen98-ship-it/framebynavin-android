package com.framebynavin.app

import android.app.Application
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.V380CombinedAlarmRearmSafetyNet
import android.util.Log

/**
 * Installs the passive cross-domain recovery interlock before any Activity,
 * alarm receiver, background worker or Creator DataStore transaction starts.
 *
 * It NEVER runs an automatic restore. A combined recovery marker causes
 * normal gated Creator edits to fail closed until a separately authenticated,
 * owner-reviewed dual-domain recovery executor exists.
 */
class BacklotRecoveryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CreatorDataGate.installUnifiedRecoveryFence(this)
        // No restoration runs here. When the OWNER has already finished
        // the H3 release and H4 is durably archived, retry the existing
        // Creator/Personal/21-day alarm reconcilers after a process death.
        runCatching {
            V380CombinedAlarmRearmSafetyNet.enqueueIfReleased(this)
        }.onFailure {
            Log.w("BacklotUnifiedRelease", "Post-release alarm work will be retried", it)
        }
    }
}

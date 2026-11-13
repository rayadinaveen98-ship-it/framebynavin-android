package com.framebynavin.app

import android.app.Application
import com.framebynavin.app.data.CreatorDataGate

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
    }
}

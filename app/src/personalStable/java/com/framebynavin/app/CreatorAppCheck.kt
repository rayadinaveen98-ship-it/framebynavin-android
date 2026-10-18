package com.framebynavin.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * PERSONAL SIDeload variant ONLY. Match the existing Backlot debug preview's Firebase
 * App Check provider so local creator data/cloud authentication is not silently changed
 * during an in-place upgrade. This is NOT production attestation. The debug token
 * must already be allow-listed by the Firebase owner as for previous previews.
 *
 * Production / Google Play builds MUST use the separate release source set's
 * Play Integrity App Check provider and privately controlled signing credentials.
 */
object CreatorAppCheck {
    fun install(context: Context) {
        FirebaseApp.initializeApp(context.applicationContext)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
    }
}

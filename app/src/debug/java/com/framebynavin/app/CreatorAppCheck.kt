package com.framebynavin.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug-only App Check. Firebase prints a one-time debug secret to Logcat for allow-listing. */
object CreatorAppCheck {
    fun install(context: Context) {
        FirebaseApp.initializeApp(context.applicationContext)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
    }
}

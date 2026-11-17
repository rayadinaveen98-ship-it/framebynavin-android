package com.framebynavin.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug-only App Check. Firebase prints a one-time debug secret to Logcat for allow-listing. */
object CreatorAppCheck {
    fun install(context: Context) {
        // The isolated Lab APK intentionally omits google-services.json because
        // the original Firebase app registration belongs to com.framebynavin.app.
        // FirebaseApp.initializeApp() returns null without Lab-specific options,
        // and FirebaseAppCheck.getInstance() would crash before onboarding.
        // Lab cloud authentication uses Supabase; Firebase AI is separately
        // disabled in this test-only variant. Preserve the original app path.
        if (BuildConfig.BACKLOT_LAB) return

        FirebaseApp.initializeApp(context.applicationContext)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
    }
}

package com.framebynavin.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Production App Check uses Play Integrity; debug builds use a separate source-set implementation. */
object CreatorAppCheck {
    fun install(context: Context) {
        FirebaseApp.initializeApp(context.applicationContext)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }
}

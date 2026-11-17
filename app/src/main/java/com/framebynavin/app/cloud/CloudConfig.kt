package com.framebynavin.app.cloud

import com.framebynavin.app.BuildConfig

object CloudConfig {
    // Opt-in lab APK has its own package and empty isolated cloud environment.
    // Standard builds keep the exact original backend.
    val SUPABASE_URL: String = if (BuildConfig.BACKLOT_LAB) {
        "https://vasleafrmsovdbdlrcsw.supabase.co"
    } else {
        "https://kukkqgpzxnfanynbddiw.supabase.co"
    }
    // Supabase PUBLISHABLE keys are safe for public clients; never embed secret/service-role keys.
    val SUPABASE_PUBLISHABLE_KEY: String = if (BuildConfig.BACKLOT_LAB) {
        "sb_publishable_HwXU7UMwp6a0Z5CJQRA6eA_Tq_wIO_b"
    } else {
        "sb_publishable_83Fd0Bh6QMe16iOAk0OR0w_f-W-7uTG"
    }

    /**
     * Public OAuth client id used only to request a Google ID token on Android.
     */
    const val GOOGLE_WEB_CLIENT_ID = "792920886199-l5ukj5btvhf00fmc7lohe440lkbonjfl.apps.googleusercontent.com"

    const val CLOUD_FORMAT = "FrameByNavinCloudBackup"
    const val CLOUD_SCHEMA_VERSION = 1
}

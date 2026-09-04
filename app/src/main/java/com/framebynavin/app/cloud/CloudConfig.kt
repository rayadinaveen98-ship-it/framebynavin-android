package com.framebynavin.app.cloud

object CloudConfig {
    const val SUPABASE_URL = "https://kukkqgpzxnfanynbddiw.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_83Fd0Bh6QMe16iOAk0OR0w_f-W-7uTG"

    /**
     * Public OAuth client id used only to request a Google ID token on Android.
     * Filled once the Google Web OAuth client is created for FrameByNavin Cloud Sync.
     */
    const val GOOGLE_WEB_CLIENT_ID = ""

    const val CLOUD_FORMAT = "FrameByNavinCloudBackup"
    const val CLOUD_SCHEMA_VERSION = 1
}

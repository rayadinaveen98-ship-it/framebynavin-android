package com.framebynavin.app.data

import android.content.Context

/** Device-local AI consent. Intentionally excluded from cloud backup so restore never re-enables sharing. */
class CreatorAiSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): CreatorAiConsent = CreatorAiConsent(
        enabled = prefs.getBoolean("enabled", false),
        sharePublicVideo = prefs.getBoolean("sharePublicVideo", false),
        shareProjectContext = prefs.getBoolean("shareProjectContext", false),
        sharePerformance = prefs.getBoolean("sharePerformance", false),
        shareCreatorLearning = prefs.getBoolean("shareCreatorLearning", false),
    )

    fun save(value: CreatorAiConsent) {
        check(prefs.edit()
            .putBoolean("enabled", value.enabled)
            .putBoolean("sharePublicVideo", value.sharePublicVideo)
            .putBoolean("shareProjectContext", value.shareProjectContext)
            .putBoolean("sharePerformance", value.sharePerformance)
            .putBoolean("shareCreatorLearning", value.shareCreatorLearning)
            .commit()) { "Could not save AI consent" }
    }

    fun clear() {
        check(prefs.edit().clear().commit()) { "Could not clear AI consent" }
    }

    companion object { private const val PREFS = "creator_ai_consent_v104" }
}

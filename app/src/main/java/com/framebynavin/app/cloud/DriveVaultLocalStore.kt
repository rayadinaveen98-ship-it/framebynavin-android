package com.framebynavin.app.cloud

import android.content.Context

/** Device-local routing state only; never creator content. */
class DriveVaultLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recoveryReviewed(email: String): Boolean =
        prefs.getString(KEY_REVIEWED_EMAIL, "").equals(email.trim(), ignoreCase = true)

    fun markRecoveryReviewed(email: String) {
        prefs.edit().putString(KEY_REVIEWED_EMAIL, email.trim().lowercase()).apply()
    }

    fun clearRecoveryReview() = prefs.edit().remove(KEY_REVIEWED_EMAIL).apply()

    companion object {
        private const val PREFS = "creator_drive_vault_v1"
        private const val KEY_REVIEWED_EMAIL = "recovery_reviewed_email"
    }
}

package com.framebynavin.app.cloud

/** Short-lived process memory only. Drive access tokens are never written to disk. */
object DriveVaultTokenMemory {
    @Volatile private var email: String = ""
    @Volatile private var token: String = ""

    fun put(accountEmail: String, accessToken: String) {
        email = accountEmail.trim().lowercase()
        token = accessToken
    }

    fun get(accountEmail: String): String? = token.takeIf {
        it.isNotBlank() && email == accountEmail.trim().lowercase()
    }

    fun clear() {
        email = ""
        token = ""
    }
}

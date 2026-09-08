package com.framebynavin.app.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CloudLocalStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadSession(): CloudSession? = synchronized(accountLock) {
        val encrypted = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            val o = JSONObject(decrypt(encrypted))
            CloudSession(
                userId = o.getString("userId"),
                email = o.optString("email"),
                displayName = o.optString("displayName"),
                avatarUrl = o.optString("avatarUrl"),
                accessToken = o.getString("accessToken"),
                refreshToken = o.getString("refreshToken"),
                expiresAtMillis = o.optLong("expiresAtMillis"),
            )
        }.getOrElse {
            if (prefs.getString(KEY_SESSION, null) == encrypted) prefs.edit().remove(KEY_SESSION).commit()
            null
        }
    }

    fun saveSession(session: CloudSession) = synchronized(accountLock) {
        val raw = JSONObject()
            .put("userId", session.userId)
            .put("email", session.email)
            .put("displayName", session.displayName)
            .put("avatarUrl", session.avatarUrl)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("expiresAtMillis", session.expiresAtMillis)
            .toString()
        check(prefs.edit().putString(KEY_SESSION, encrypt(raw)).commit()) { "Could not save cloud session" }
    }

    fun clearSession() = synchronized(accountLock) {
        check(prefs.edit().remove(KEY_SESSION).commit()) { "Could not clear cloud session" }
    }

    fun loadCreatorProfile(): CloudCreatorProfile? {
        val raw = prefs.getString(KEY_CREATOR_PROFILE, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            CloudCreatorProfile(
                userId = o.optString("userId"),
                displayName = o.optString("displayName"),
                username = o.optString("username"),
                avatarUrl = o.optString("avatarUrl"),
                createdAtMillis = o.optLong("createdAtMillis"),
                updatedAtMillis = o.optLong("updatedAtMillis"),
            )
        }.getOrNull()
    }

    fun saveCreatorProfile(profile: CloudCreatorProfile) {
        val raw = JSONObject()
            .put("userId", profile.userId)
            .put("displayName", profile.displayName)
            .put("username", profile.username)
            .put("avatarUrl", profile.avatarUrl)
            .put("createdAtMillis", profile.createdAtMillis)
            .put("updatedAtMillis", profile.updatedAtMillis)
            .toString()
        prefs.edit().putString(KEY_CREATOR_PROFILE, raw).apply()
    }

    fun clearCreatorProfile() = prefs.edit().remove(KEY_CREATOR_PROFILE).apply()

    fun settings(): CloudSyncSettings = CloudSyncSettings(
        enabled = false, // v1.8.1 is explicit, append-only backup; no background uploads.

        wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
        lastSyncAtMillis = prefs.getLong(KEY_LAST_SYNC, 0L),
        lastError = prefs.getString(KEY_LAST_ERROR, "").orEmpty(),
        deviceKey = deviceKey(),
        reconciliationRequired = needsReconciliation(),
    )

    fun setEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, false).apply()

    /** A durable generation invalidates queued operations after sign-out/delete/account switch. */
    fun generation(): Long = synchronized(accountLock) { prefs.getLong(KEY_GENERATION, 0L) }

    /** Durable account epoch shared by all manager instances. */
    fun invalidateOperations(): Long = synchronized(accountLock) {
        val next = prefs.getLong(KEY_GENERATION, 0L) + 1L
        check(prefs.edit().putLong(KEY_GENERATION, next).putBoolean(KEY_ENABLED, false).commit()) {
            "Could not invalidate pending cloud operations"
        }
        next
    }

    /** A late refresh cannot recreate a signed-out session or cross an account switch. */
    fun saveRefreshedSession(session: CloudSession, expectedGeneration: Long, expectedUserId: String): Boolean =
        synchronized(accountLock) {
            if (prefs.getLong(KEY_GENERATION, 0L) != expectedGeneration) return@synchronized false
            val current = loadSession() ?: return@synchronized false
            if (current.userId != expectedUserId || session.userId != expectedUserId) return@synchronized false
            saveSession(session)
            true
        }
    fun reconciledUser(): String = prefs.getString(KEY_RECONCILED_USER, "").orEmpty()
    fun approveUser(userId: String) = prefs.edit().putString(KEY_RECONCILED_USER, userId).commit()
    fun clearApproval() = prefs.edit().remove(KEY_RECONCILED_USER).commit()
    fun needsReconciliation(): Boolean {
        val session = loadSession() ?: return false
        return reconciledUser() != session.userId
    }

    fun setWifiOnly(value: Boolean) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
    fun markSyncSuccess(now: Long) = prefs.edit().putLong(KEY_LAST_SYNC, now).putString(KEY_LAST_ERROR, "").apply()
    fun markError(message: String) = prefs.edit().putString(KEY_LAST_ERROR, message.take(300)).apply()

    fun deviceKey(): String {
        val existing = prefs.getString(KEY_DEVICE, null)
        if (!existing.isNullOrBlank()) return existing
        val value = "device-${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE, value).apply()
        return value
    }

    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        val blob = ByteArray(cipher.iv.size + encrypted.size)
        System.arraycopy(cipher.iv, 0, blob, 0, cipher.iv.size)
        System.arraycopy(encrypted, 0, blob, cipher.iv.size, encrypted.size)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val blob = Base64.decode(value, Base64.NO_WRAP)
        require(blob.size > IV_BYTES)
        val iv = blob.copyOfRange(0, IV_BYTES)
        val encrypted = blob.copyOfRange(IV_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private val accountLock = Any()
        private const val PREFS = "creator_cloud_v13"
        private const val KEY_SESSION = "session"
        private const val KEY_CREATOR_PROFILE = "creator_profile_v23"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_DEVICE = "device_key"
        private const val KEY_GENERATION = "backup_generation_v181"
        private const val KEY_RECONCILED_USER = "backup_reconciled_user_v181"
        private const val KEY_ALIAS = "framebynavin_cloud_session_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }
}

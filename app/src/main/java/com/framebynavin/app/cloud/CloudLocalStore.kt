package com.framebynavin.app.cloud

import android.content.Context
import android.provider.Settings
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

    fun loadSession(): CloudSession? {
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
            prefs.edit().remove(KEY_SESSION).apply()
            null
        }
    }

    fun saveSession(session: CloudSession) {
        val raw = JSONObject()
            .put("userId", session.userId)
            .put("email", session.email)
            .put("displayName", session.displayName)
            .put("avatarUrl", session.avatarUrl)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("expiresAtMillis", session.expiresAtMillis)
            .toString()
        prefs.edit().putString(KEY_SESSION, encrypt(raw)).apply()
    }

    fun clearSession() = prefs.edit().remove(KEY_SESSION).apply()

    fun settings(): CloudSyncSettings = CloudSyncSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
        lastSyncAtMillis = prefs.getLong(KEY_LAST_SYNC, 0L),
        lastError = prefs.getString(KEY_LAST_ERROR, "").orEmpty(),
        deviceKey = deviceKey(),
    )

    fun setEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    fun setWifiOnly(value: Boolean) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
    fun markSyncSuccess(now: Long) = prefs.edit().putLong(KEY_LAST_SYNC, now).putString(KEY_LAST_ERROR, "").apply()
    fun markError(message: String) = prefs.edit().putString(KEY_LAST_ERROR, message.take(300)).apply()

    fun deviceKey(): String {
        val existing = prefs.getString(KEY_DEVICE, null)
        if (!existing.isNullOrBlank()) return existing
        val androidId = runCatching { Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull().orEmpty()
        val value = if (androidId.isNotBlank()) "android-$androidId" else "device-${UUID.randomUUID()}"
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
        private const val PREFS = "creator_cloud_v13"
        private const val KEY_SESSION = "session"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_DEVICE = "device_key"
        private const val KEY_ALIAS = "framebynavin_cloud_session_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }
}

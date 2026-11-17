package com.framebynavin.app.cloud.ux09

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Device-local, account-scoped Android Keystore key provider for UX09 experiments.
 *
 * Not connected to app runtime. Keys are NOT portable across devices/backups,
 * so restored ciphertext must never be silently treated as a valid cloud backup.
 * A future integration must exclude these blobs from Auto Backup and define
 * an explicit key-loss/quarantine policy before storing real user content.
 */
internal object Ux09AndroidKeyProvider {
    @Synchronized
    fun keyForAccount(accountId: String): SecretKey {
        Ux09Contract.requireUuid(accountId)
        val alias = aliasFor(accountId)
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    internal fun aliasFor(accountId: String): String {
        Ux09Contract.requireUuid(accountId)
        return "backlot_ux09_lab_chain_v1_" +
            Ux09Contract.sha256(accountId.toByteArray(Charsets.UTF_8))
    }
}

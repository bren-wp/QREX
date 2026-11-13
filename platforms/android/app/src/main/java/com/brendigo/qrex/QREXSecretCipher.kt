package com.brendigo.qrex

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts all local QR history with the original non-exportable Keystore key. */
internal object QREXSecretCipher {
    // Historical Wi-Fi alias MUST stay unchanged to decrypt existing installs.
    private const val ALIAS = "com.brendigo.qrex.history.wifi.v1"
    private const val PREFIX = "qrex-gcm:v1:"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    // Bound attacker-controlled pref values and Keystore allocations. Every
    // QR payload is limited to 10,000 UTF-16 chars by QREXStore.
    private const val MAX_PLAINTEXT_BYTES = 48_000
    private const val MAX_ENCODED_CHARS = 65_536

    fun isEncrypted(raw: String): Boolean = raw.startsWith(PREFIX)

    // Read the Keystore handle once per process. This prevents hundreds of
    // repeated KeyStore.load/getKey calls during one-time 250-record migration.
    // Failed key acquisition is retried by Kotlin lazy on a subsequent call.
    private val cachedKey: SecretKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(ALIAS, null) as? SecretKey
        if (existing != null) existing else {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generator.generateKey()
        }
    }

    private fun key(): SecretKey = cachedKey

    fun available(): Boolean = runCatching { key(); true }.getOrDefault(false)

    fun encrypt(raw: String): String? = runCatching {
        val utf8 = raw.toByteArray(Charsets.UTF_8)
        if (utf8.size > MAX_PLAINTEXT_BYTES) return null
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(
            cipher.doFinal(utf8), Base64.NO_WRAP)
        "$PREFIX$iv:$encrypted"
    }.getOrNull()

    fun decrypt(encoded: String): String? = runCatching {
        if (!isEncrypted(encoded) || encoded.length > MAX_ENCODED_CHARS) return null
        val fields = encoded.removePrefix(PREFIX).split(':')
        if (fields.size != 2) return null
        val iv = Base64.decode(fields[0], Base64.NO_WRAP)
        if (iv.size != 12) return null
        val ciphertext = Base64.decode(fields[1], Base64.NO_WRAP)
        if (ciphertext.size > MAX_PLAINTEXT_BYTES + 16) return null
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()
}

package com.example.fishing.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts the auth session blob that is persisted on disk.
 *
 * Implementations must return a self-describing payload, so the encrypted file
 * does not need any metadata outside of it.
 */
internal interface SessionCipher {

    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(payload: ByteArray): ByteArray
}

/**
 * AES-256-GCM cipher whose key never leaves the Android Keystore.
 *
 * The key is not backup-friendly by design: if the encrypted file is restored
 * to another device (or the key is invalidated), decryption fails and the app
 * simply asks the user to sign in again instead of exposing a usable token.
 */
internal class KeystoreSessionCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : SessionCipher {

    private val keyLock = Any()

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        require(iv.size <= MAX_IV_SIZE) { "Unexpected GCM IV size: ${iv.size}" }
        val ciphertext = cipher.doFinal(plaintext)
        return byteArrayOf(PAYLOAD_VERSION, iv.size.toByte()) + iv + ciphertext
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > HEADER_SIZE) { "Session payload is too short" }
        require(payload[0] == PAYLOAD_VERSION) { "Unsupported session payload version" }
        val ivSize = payload[1].toInt()
        require(ivSize in 1..MAX_IV_SIZE && payload.size > HEADER_SIZE + ivSize) {
            "Malformed session payload"
        }
        val iv = payload.copyOfRange(HEADER_SIZE, HEADER_SIZE + ivSize)
        val ciphertext = payload.copyOfRange(HEADER_SIZE + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        generator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "fishing.auth.session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_SIZE_BITS = 128
        const val MAX_IV_SIZE = 32
        const val PAYLOAD_VERSION: Byte = 1
        const val HEADER_SIZE = 2
    }
}

package com.vaitri.ghostmode.security

import android.content.Context
import android.util.Base64
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

    /* ================= VAULT ================= */

    private const val PREFS = "ghostmode_vault"
    private const val KEY = "vault_key"

    private fun getSecretKey(context: Context): SecretKey {

        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val savedKey = prefs.getString(KEY, null)

        if (savedKey != null) {

            val decoded = Base64.decode(
                savedKey,
                Base64.DEFAULT
            )

            return SecretKeySpec(
                decoded,
                "AES"
            )
        }

        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)

        val secretKey = keyGenerator.generateKey()

        prefs.edit()
            .putString(
                KEY,
                Base64.encodeToString(
                    secretKey.encoded,
                    Base64.DEFAULT
                )
            )
            .apply()

        return secretKey
    }

    fun encryptFile(
        context: Context,
        inputFile: File,
        outputFile: File
    ) {

        val key = getSecretKey(context)

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(128, iv)
        )

        val encrypted = cipher.doFinal(
            inputFile.readBytes()
        )

        outputFile.outputStream().use { output ->

            output.write(iv)
            output.write(encrypted)
        }
    }

    fun decryptFile(
        context: Context,
        encryptedFile: File
    ): ByteArray {

        val key = getSecretKey(context)

        val data = encryptedFile.readBytes()

        require(data.size > 12) {
            "Invalid encrypted file"
        }

        val iv = data.copyOfRange(0, 12)

        val encrypted = data.copyOfRange(
            12,
            data.size
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, iv)
        )

        return cipher.doFinal(encrypted)
    }


    /* ================= SECURE NOTES ================= */

    private const val NOTE_KEY_ALIAS =
        "GhostModeNotesKey"

    private fun getNotesKey(): SecretKey {

        val keyStore = KeyStore.getInstance(
            "AndroidKeyStore"
        )

        keyStore.load(null)

        val existingKey =
            keyStore.getKey(
                NOTE_KEY_ALIAS,
                null
            )

        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                "AES",
                "AndroidKeyStore"
            )

        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                NOTE_KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )

        return keyGenerator.generateKey()
    }

    fun encryptNote(note: String): String {

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getNotesKey(),
            GCMParameterSpec(128, iv)
        )

        val encrypted = cipher.doFinal(
            note.toByteArray(StandardCharsets.UTF_8)
        )

        val combined = iv + encrypted

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    fun decryptNote(encryptedNote: String): String {

        return try {

            val combined = Base64.decode(
                encryptedNote,
                Base64.NO_WRAP
            )

            require(combined.size > 12)

            val iv = combined.copyOfRange(
                0,
                12
            )

            val encrypted = combined.copyOfRange(
                12,
                combined.size
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getNotesKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(encrypted),
                StandardCharsets.UTF_8
            )

        } catch (e: Exception) {

            ""
        }
    }
}
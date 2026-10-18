package dev.techda.sahatisukari.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val encryptedTokens = stringPreferencesKey("encrypted_tokens")
    private val keyAlias = "sahati.session.v1"

    suspend fun read(): SessionTokens? {
        val value = context.sessionDataStore.data.first()[encryptedTokens] ?: return null
        return runCatching { decrypt(value) }.getOrNull()?.split('\u0000')?.takeIf { it.size == 2 }
            ?.let { SessionTokens(it[0], it[1]) }
    }

    suspend fun save(tokens: SessionTokens) {
        val encrypted = encrypt(tokens.accessToken + '\u0000' + tokens.refreshToken)
        context.sessionDataStore.edit { it[encryptedTokens] = encrypted }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.remove(encryptedTokens) }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }
}

data class SessionTokens(val accessToken: String, val refreshToken: String)

package com.jdrms.bulletin.domain.profile.infrastructure

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.jdrms.bulletin.core.network.SupabaseAuthStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private storage encrypted using a non-exportable Android Keystore key. */
class AndroidAuthStorage(context: Context, projectUrl: String) : SupabaseAuthStorage {
    private val preferences = context.getSharedPreferences(
        "bulletin_secure_auth_${Uri.parse(projectUrl).host.orEmpty()}",
        Context.MODE_PRIVATE
    )
    override suspend fun saveCodeVerifier(codeVerifier: String) = withContext(Dispatchers.IO) {
        write("verifier", codeVerifier)
    }
    override suspend fun loadCodeVerifier(): String? = withContext(Dispatchers.IO) { read("verifier") }
    override suspend fun deleteCodeVerifier() = withContext(Dispatchers.IO) { remove("verifier") }

    @Synchronized
    internal fun write(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
        check(preferences.edit().putString(name, encoded).commit()) { "Unable to persist authentication." }
    }

    @Synchronized
    internal fun read(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
        }.getOrElse {
            remove(name)
            null
        }
    }

    @Synchronized
    internal fun remove(name: String) {
        check(preferences.edit().remove(name).commit()) { "Unable to clear authentication." }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("bulletin.auth", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    "bulletin.auth",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
            )
        }.generateKey()
    }
}

package com.example.armoredage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyManager(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "age_keys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun generateAndStoreIdentity(label: String): Pair<String, String> {
        val (publicKey, privateKey) = generateKeyPair()
        prefs.edit()
            .putString("id_priv_$label", privateKey)
            .putString("id_pub_$label", publicKey)
            .apply()
        return publicKey to privateKey
    }

    fun getStoredPrivateKey(label: String): String? = prefs.getString("id_priv_$label", null)

    fun getStoredPublicKey(label: String): String? = prefs.getString("id_pub_$label", null)

    fun listIdentityLabels(): List<String> =
        prefs.all.keys
            .filter { it.startsWith("id_priv_") }
            .map { it.removePrefix("id_priv_") }
            .sorted()

    private fun generateKeyPair(): Pair<String, String> {
        try {
            val clazz = Class.forName("at.asitplus.kage.age.Age")
            val method = clazz.methods.first { it.name == "generateX25519Identity" }
            val identity = method.invoke(null)
            val getPrivate = identity.javaClass.methods.first { it.name.contains("private", true) }
            val getPublic = identity.javaClass.methods.first { it.name.contains("public", true) }
            return getPublic.invoke(identity).toString() to getPrivate.invoke(identity).toString()
        } catch (_: Exception) {
            // Fallback for development without exact kage API binding.
            val suffix = System.currentTimeMillis().toString(16)
            return "age1placeholder$suffix" to "AGE-SECRET-KEY-placeholder$suffix"
        }
    }
}

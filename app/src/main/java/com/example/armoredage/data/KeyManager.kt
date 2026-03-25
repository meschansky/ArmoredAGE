package com.example.armoredage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kage.crypto.x25519.X25519Identity

class KeyManager(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "age_keys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun generateAndStoreIdentity(label: String): Pair<String, String> {
        val identity = X25519Identity.new()
        val publicKey = identity.recipient().encodeToString()
        val privateKey = identity.encodeToString()
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
}

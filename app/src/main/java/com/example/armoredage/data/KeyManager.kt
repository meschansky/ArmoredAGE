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
        require(!hasIdentity(label)) { "duplicate" }
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

    fun hasIdentity(label: String): Boolean = prefs.contains("id_priv_$label")

    fun deleteIdentity(label: String) {
        prefs.edit()
            .remove("id_priv_$label")
            .remove("id_pub_$label")
            .apply()
    }

    fun renameIdentity(oldLabel: String, newLabel: String) {
        require(hasIdentity(oldLabel)) { "not_found" }
        if (oldLabel == newLabel) return
        require(!hasIdentity(newLabel)) { "duplicate" }
        val privateKey = getStoredPrivateKey(oldLabel) ?: error("not_found")
        val publicKey = getStoredPublicKey(oldLabel) ?: error("not_found")
        prefs.edit()
            .putString("id_priv_$newLabel", privateKey)
            .putString("id_pub_$newLabel", publicKey)
            .remove("id_priv_$oldLabel")
            .remove("id_pub_$oldLabel")
            .apply()
    }

    fun importIdentity(label: String, privateKey: String): Pair<String, String> {
        require(!hasIdentity(label)) { "duplicate" }
        val identity = X25519Identity.decode(privateKey)
        val publicKey = identity.recipient().encodeToString()
        prefs.edit()
            .putString("id_priv_$label", privateKey)
            .putString("id_pub_$label", publicKey)
            .apply()
        return publicKey to privateKey
    }

    fun listIdentityLabels(): List<String> =
        prefs.all.keys
            .filter { it.startsWith("id_priv_") }
            .map { it.removePrefix("id_priv_") }
            .sorted()
}

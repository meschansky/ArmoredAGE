package com.example.armoredage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KnownRecipientManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "age_recipients",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun addRecipient(name: String, publicKey: String) {
        require(publicKey.startsWith("age1")) { "Recipient must be an AGE X25519 public key." }
        prefs.edit().putString("recipient_$name", publicKey).apply()
    }

    fun getRecipient(name: String): String? = prefs.getString("recipient_$name", null)

    fun deleteRecipient(name: String) {
        prefs.edit().remove("recipient_$name").apply()
    }

    fun listRecipients(): List<Pair<String, String>> =
        prefs.all
            .filter { it.key.startsWith("recipient_") }
            .mapNotNull { (k, v) ->
                val key = v as? String ?: return@mapNotNull null
                k.removePrefix("recipient_") to key
            }
            .sortedBy { it.first.lowercase() }
}

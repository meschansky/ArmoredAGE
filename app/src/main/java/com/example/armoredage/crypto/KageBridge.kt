package com.example.armoredage.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kage.Age
import kage.crypto.x25519.X25519Identity
import kage.crypto.x25519.X25519Recipient

class KageBridge {

    fun encryptArmored(plainText: String, recipientPublicKey: String): String {
        val input = ByteArrayInputStream(plainText.toByteArray(Charsets.UTF_8))
        val output = ByteArrayOutputStream()
        Age.encryptStream(
            recipients = listOf(X25519Recipient.decode(recipientPublicKey)),
            inputStream = input,
            outputStream = output,
            generateArmor = true
        )
        return output.toString(Charsets.UTF_8.name())
    }

    fun decryptArmored(armoredPayload: String, privateKey: String): String {
        val input = ByteArrayInputStream(armoredPayload.toByteArray(Charsets.UTF_8))
        val output = ByteArrayOutputStream()
        Age.decryptStream(
            identities = listOf(X25519Identity.decode(privateKey)),
            srcStream = input,
            dstStream = output
        )
        return output.toString(Charsets.UTF_8.name())
    }
}

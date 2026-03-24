package com.example.armoredage.crypto

import java.lang.reflect.Method

/**
 * Thin reflection-based adapter so app compiles even if kage API names evolve.
 */
class KageBridge {

    fun encryptArmored(plainText: String, recipientPublicKey: String): String {
        try {
            // Expected in current kage API:
            // at.asitplus.kage.age.Age.encryptArmored(plain: String, recipients: List<String>)
            val clazz = Class.forName("at.asitplus.kage.age.Age")
            val method: Method = clazz.methods.first {
                it.name == "encryptArmored" && it.parameterTypes.size == 2
            }
            val result = method.invoke(null, plainText, listOf(recipientPublicKey))
            return result.toString()
        } catch (ex: Exception) {
            throw IllegalStateException(
                "kage encryptArmored invocation failed. Verify the kage version/API.",
                ex
            )
        }
    }

    fun decryptArmored(armoredPayload: String, privateKey: String): String {
        try {
            // Expected in current kage API:
            // at.asitplus.kage.age.Age.decryptArmored(armored: String, identities: List<String>)
            val clazz = Class.forName("at.asitplus.kage.age.Age")
            val method: Method = clazz.methods.first {
                it.name == "decryptArmored" && it.parameterTypes.size == 2
            }
            val result = method.invoke(null, armoredPayload, listOf(privateKey))
            return result.toString()
        } catch (ex: Exception) {
            throw IllegalStateException(
                "kage decryptArmored invocation failed. Verify the kage version/API.",
                ex
            )
        }
    }
}

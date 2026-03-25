package com.example.armoredage.crypto

object AgeArmor {
    private const val HEADER = "-----BEGIN AGE ENCRYPTED FILE-----"
    private const val FOOTER = "-----END AGE ENCRYPTED FILE-----"

    fun isArmoredAge(payload: String): Boolean {
        val trimmed = payload.trim()
        return trimmed.startsWith(HEADER) && trimmed.endsWith(FOOTER)
    }

    fun requireArmored(payload: String) {
        require(isArmoredAge(payload)) {
            "Only ASCII-armored AGE messages are allowed."
        }
    }
}

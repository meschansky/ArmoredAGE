package com.example.armoredage.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.armoredage.crypto.AgeArmor
import com.example.armoredage.crypto.KageBridge
import com.example.armoredage.data.KeyManager
import com.example.armoredage.data.KnownRecipientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AgeUiState(
    val plaintext: String = "",
    val ciphertext: String = "",
    val selectedRecipient: String = "",
    val selectedIdentity: String = "default",
    val recipientNameInput: String = "",
    val recipientPubkeyInput: String = "",
    val result: String = "",
    val error: String? = null,
    val identities: List<String> = emptyList(),
    val recipients: List<Pair<String, String>> = emptyList()
)

class AgeViewModel(
    private val keyManager: KeyManager,
    private val recipientManager: KnownRecipientManager,
    private val kageBridge: KageBridge = KageBridge()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AgeUiState(
            identities = keyManager.listIdentityLabels(),
            recipients = recipientManager.listRecipients()
        )
    )
    val uiState: StateFlow<AgeUiState> = _uiState

    fun updatePlaintext(value: String) = _uiState.update { it.copy(plaintext = value, error = null) }
    fun updateCiphertext(value: String) = _uiState.update { it.copy(ciphertext = value, error = null) }
    fun updateRecipientName(value: String) = _uiState.update { it.copy(recipientNameInput = value, error = null) }
    fun updateRecipientPubkey(value: String) = _uiState.update { it.copy(recipientPubkeyInput = value, error = null) }
    fun selectRecipient(value: String) = _uiState.update { it.copy(selectedRecipient = value, error = null) }
    fun selectIdentity(value: String) = _uiState.update { it.copy(selectedIdentity = value, error = null) }

    fun generateIdentity() {
        runCatching {
            val label = "id-${System.currentTimeMillis()}"
            keyManager.generateAndStoreIdentity(label)
            _uiState.update {
                it.copy(
                    identities = keyManager.listIdentityLabels(),
                    selectedIdentity = label,
                    result = "Generated identity '$label'.",
                    error = null
                )
            }
        }.onFailure { setError(it) }
    }

    fun saveRecipient() {
        val name = uiState.value.recipientNameInput.trim()
        val pub = uiState.value.recipientPubkeyInput.trim()
        runCatching {
            require(name.isNotEmpty()) { "Recipient name is required." }
            recipientManager.addRecipient(name, pub)
            _uiState.update {
                it.copy(
                    recipients = recipientManager.listRecipients(),
                    selectedRecipient = name,
                    recipientNameInput = "",
                    recipientPubkeyInput = "",
                    result = "Saved recipient '$name'.",
                    error = null
                )
            }
        }.onFailure { setError(it) }
    }

    fun encrypt() {
        val state = uiState.value
        runCatching {
            val recipient = recipientManager.getRecipient(state.selectedRecipient)
                ?: error("Select a known recipient for encryption.")
            val out = kageBridge.encryptArmored(state.plaintext, recipient)
            AgeArmor.requireArmored(out)
            _uiState.update { it.copy(result = out, error = null) }
        }.onFailure { setError(it) }
    }

    fun decrypt() {
        val state = uiState.value
        runCatching {
            AgeArmor.requireArmored(state.ciphertext)
            val privateKey = keyManager.getStoredPrivateKey(state.selectedIdentity)
                ?: error("Identity not found. Generate one first.")
            val out = kageBridge.decryptArmored(state.ciphertext, privateKey)
            _uiState.update { it.copy(result = out, error = null) }
        }.onFailure { setError(it) }
    }

    private fun setError(ex: Throwable) {
        _uiState.update {
            it.copy(error = ex.message ?: "Unknown error", result = "")
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AgeViewModel(
                        keyManager = KeyManager(context),
                        recipientManager = KnownRecipientManager(context)
                    ) as T
                }
            }
    }
}

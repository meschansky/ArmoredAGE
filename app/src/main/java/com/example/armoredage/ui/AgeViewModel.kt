package com.example.armoredage.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.armoredage.R
import com.example.armoredage.crypto.AgeArmor
import com.example.armoredage.crypto.KageBridge
import com.example.armoredage.data.KeyManager
import com.example.armoredage.data.KnownRecipientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class TopLevelSection {
    MAIN,
    RECIPIENTS,
    MY_KEYS;

    val labelRes: Int
        get() = when (this) {
            MAIN -> R.string.nav_main
            RECIPIENTS -> R.string.nav_recipients
            MY_KEYS -> R.string.nav_my_keys
        }
}

enum class MainMode {
    ENCRYPT,
    DECRYPT;

    val labelRes: Int
        get() = when (this) {
            ENCRYPT -> R.string.main_mode_encrypt
            DECRYPT -> R.string.main_mode_decrypt
        }
}

data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<Any> = emptyList()
)

data class AgeUiState(
    val plaintext: String = "",
    val ciphertext: String = "",
    val selectedRecipient: String = "",
    val selectedIdentity: String = "",
    val recipientNameInput: String = "",
    val recipientPubkeyInput: String = "",
    val result: String = "",
    val errorMessage: UiMessage? = null,
    val noticeMessage: UiMessage? = null,
    val activeSection: TopLevelSection = TopLevelSection.MAIN,
    val mainMode: MainMode = MainMode.ENCRYPT,
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

    fun updatePlaintext(value: String) = clearTransientAndUpdate { it.copy(plaintext = value) }
    fun updateCiphertext(value: String) = clearTransientAndUpdate { it.copy(ciphertext = value) }
    fun updateRecipientName(value: String) = clearTransientAndUpdate { it.copy(recipientNameInput = value) }
    fun updateRecipientPubkey(value: String) = clearTransientAndUpdate { it.copy(recipientPubkeyInput = value) }
    fun selectRecipient(value: String) = clearTransientAndUpdate { it.copy(selectedRecipient = value) }
    fun selectIdentity(value: String) = clearTransientAndUpdate { it.copy(selectedIdentity = value) }
    fun selectSection(value: TopLevelSection) = _uiState.update { it.copy(activeSection = value) }
    fun selectMainMode(value: MainMode) = clearTransientAndUpdate { it.copy(mainMode = value) }
    fun clearNotice() = _uiState.update { it.copy(noticeMessage = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearPlaintext() = _uiState.update { it.copy(plaintext = "", errorMessage = null) }
    fun clearCiphertext() = _uiState.update { it.copy(ciphertext = "", errorMessage = null) }
    fun clearResult() = _uiState.update { it.copy(result = "", errorMessage = null) }

    fun generateIdentity() {
        runCatching {
            val label = "id-${System.currentTimeMillis()}"
            keyManager.generateAndStoreIdentity(label)
            _uiState.update {
                it.copy(
                    identities = keyManager.listIdentityLabels(),
                    selectedIdentity = label,
                    activeSection = TopLevelSection.MY_KEYS,
                    noticeMessage = UiMessage(R.string.notice_identity_generated, listOf(label)),
                    errorMessage = null
                )
            }
        }.onFailure { setError(R.string.error_unknown) }
    }

    fun saveRecipient() {
        val name = uiState.value.recipientNameInput.trim()
        val pub = uiState.value.recipientPubkeyInput.trim()
        if (name.isEmpty()) {
            setError(R.string.error_recipient_name_required)
            return
        }
        runCatching {
            recipientManager.addRecipient(name, pub)
            _uiState.update {
                it.copy(
                    recipients = recipientManager.listRecipients(),
                    selectedRecipient = name,
                    recipientNameInput = "",
                    recipientPubkeyInput = "",
                    activeSection = TopLevelSection.RECIPIENTS,
                    noticeMessage = UiMessage(R.string.notice_recipient_saved, listOf(name)),
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "duplicate" -> R.string.error_recipient_duplicate
                    else -> R.string.error_invalid_public_key
                }
            )
        }
    }

    fun deleteRecipient(name: String) {
        runCatching {
            recipientManager.deleteRecipient(name)
            val recipients = recipientManager.listRecipients()
            _uiState.update {
                it.copy(
                    recipients = recipients,
                    selectedRecipient = if (it.selectedRecipient == name) recipients.firstOrNull()?.first.orEmpty() else it.selectedRecipient,
                    noticeMessage = UiMessage(R.string.notice_recipient_deleted, listOf(name)),
                    errorMessage = null
                )
            }
        }.onFailure { setError(R.string.error_unknown) }
    }

    fun renameRecipient(oldName: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            setError(R.string.error_recipient_name_required)
            return false
        }
        runCatching {
            recipientManager.renameRecipient(oldName, trimmed)
            val recipients = recipientManager.listRecipients()
            _uiState.update {
                it.copy(
                    recipients = recipients,
                    selectedRecipient = if (it.selectedRecipient == oldName) trimmed else it.selectedRecipient,
                    noticeMessage = UiMessage(R.string.notice_recipient_renamed, listOf(oldName, trimmed)),
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "duplicate" -> R.string.error_recipient_duplicate
                    else -> R.string.error_unknown
                }
            )
        }
        return uiState.value.errorMessage == null
    }

    fun deleteIdentity(label: String) {
        runCatching {
            keyManager.deleteIdentity(label)
            val identities = keyManager.listIdentityLabels()
            _uiState.update {
                it.copy(
                    identities = identities,
                    selectedIdentity = if (it.selectedIdentity == label) identities.firstOrNull().orEmpty() else it.selectedIdentity,
                    noticeMessage = UiMessage(R.string.notice_identity_deleted, listOf(label)),
                    errorMessage = null
                )
            }
        }.onFailure { setError(R.string.error_unknown) }
    }

    fun renameIdentity(oldLabel: String, newLabel: String): Boolean {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty()) {
            setError(R.string.error_identity_label_required)
            return false
        }
        runCatching {
            keyManager.renameIdentity(oldLabel, trimmed)
            val identities = keyManager.listIdentityLabels()
            _uiState.update {
                it.copy(
                    identities = identities,
                    selectedIdentity = if (it.selectedIdentity == oldLabel) trimmed else it.selectedIdentity,
                    noticeMessage = UiMessage(R.string.notice_identity_renamed, listOf(oldLabel, trimmed)),
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "duplicate" -> R.string.error_identity_duplicate
                    else -> R.string.error_unknown
                }
            )
        }
        return uiState.value.errorMessage == null
    }

    fun importIdentity(label: String, privateKey: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            setError(R.string.error_identity_label_required)
            return false
        }
        runCatching {
            keyManager.importIdentity(trimmed, privateKey.trim())
            _uiState.update {
                it.copy(
                    identities = keyManager.listIdentityLabels(),
                    selectedIdentity = trimmed,
                    activeSection = TopLevelSection.MY_KEYS,
                    noticeMessage = UiMessage(R.string.notice_identity_imported, listOf(trimmed)),
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "duplicate" -> R.string.error_identity_duplicate
                    else -> R.string.error_invalid_private_key
                }
            )
        }
        return uiState.value.errorMessage == null
    }

    fun encrypt() {
        val state = uiState.value
        runCatching {
            val recipient = recipientManager.getRecipient(state.selectedRecipient)
                ?: throw IllegalStateException("missing recipient")
            val out = kageBridge.encryptArmored(state.plaintext, recipient)
            AgeArmor.requireArmored(out)
            _uiState.update { it.copy(result = out, errorMessage = null) }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "missing recipient" -> R.string.error_select_recipient
                    else -> R.string.error_unknown
                }
            )
        }
    }

    fun decrypt() {
        val state = uiState.value
        runCatching {
            AgeArmor.requireArmored(state.ciphertext)
            val privateKey = keyManager.getStoredPrivateKey(state.selectedIdentity)
                ?: throw IllegalStateException("missing identity")
            val out = kageBridge.decryptArmored(state.ciphertext, privateKey)
            _uiState.update { it.copy(result = out, errorMessage = null) }
        }.onFailure { ex ->
            setError(
                when (ex.message) {
                    "missing identity" -> R.string.error_identity_missing
                    else -> R.string.error_invalid_armored_payload
                }
            )
        }
    }

    fun publicKeyFor(label: String): String? = keyManager.getStoredPublicKey(label)
    fun privateKeyFor(label: String): String? = keyManager.getStoredPrivateKey(label)

    private fun clearTransientAndUpdate(transform: (AgeUiState) -> AgeUiState) {
        _uiState.update { transform(it).copy(errorMessage = null) }
    }

    private fun setError(@StringRes messageRes: Int, args: List<Any> = emptyList()) {
        _uiState.update {
            it.copy(errorMessage = UiMessage(messageRes, args), result = "")
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AgeViewModel::class.java))
                    return modelClass.cast(
                        AgeViewModel(
                            keyManager = KeyManager(context),
                            recipientManager = KnownRecipientManager(context)
                        )
                    )!!
                }
            }
    }
}

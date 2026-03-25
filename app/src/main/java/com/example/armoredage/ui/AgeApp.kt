package com.example.armoredage.ui

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.armoredage.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeApp(context: Context) {
    val vm: AgeViewModel = viewModel(factory = AgeViewModel.factory(context))
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val resultCopiedNotice = stringResource(R.string.notice_result_copied)
    val publicKeyCopiedNotice = stringResource(R.string.notice_public_key_copied)
    val privateKeyCopiedNotice = stringResource(R.string.notice_private_key_copied)
    val clipboardEmptyNotice = stringResource(R.string.notice_clipboard_empty)
    val copyLabel = stringResource(R.string.action_copy)

    var deleteRecipientTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteIdentityTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var privateKeyCopyTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameRecipientTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameRecipientDraft by rememberSaveable { mutableStateOf("") }
    var renameIdentityTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameIdentityDraft by rememberSaveable { mutableStateOf("") }
    var importDialogOpen by rememberSaveable { mutableStateOf(false) }
    var importLabel by rememberSaveable { mutableStateOf("") }
    var importPrivateKey by rememberSaveable { mutableStateOf("") }

    fun copyWithFeedback(value: String, message: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("armored-age", value)))
            snackbarHostState.showSnackbar(message)
        }
    }

    fun pasteFromClipboard(onPasted: (String) -> Unit) {
        scope.launch {
            val text = clipboard
                .getClipEntry()
                ?.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.takeIf { it.isNotBlank() }

            if (text == null) {
                snackbarHostState.showSnackbar(clipboardEmptyNotice)
            } else {
                onPasted(text)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = state.activeSection == section,
                        onClick = { vm.selectSection(section) },
                        icon = { Icon(section.icon, contentDescription = stringResource(section.labelRes)) },
                        label = { Text(stringResource(section.labelRes)) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when (state.activeSection) {
            TopLevelSection.MAIN -> MainSection(
                state = state,
                onModeSelected = vm::selectMainMode,
                onPlaintextChange = vm::updatePlaintext,
                onCiphertextChange = vm::updateCiphertext,
                onRecipientSelected = vm::selectRecipient,
                onIdentitySelected = vm::selectIdentity,
                onEncrypt = vm::encrypt,
                onDecrypt = vm::decrypt,
                onClearPlaintext = vm::clearPlaintext,
                onClearCiphertext = vm::clearCiphertext,
                onClearResult = vm::clearResult,
                onPastePlaintext = { pasteFromClipboard(vm::updatePlaintext) },
                onPasteCiphertext = { pasteFromClipboard(vm::updateCiphertext) },
                onCopyResult = {
                    copyWithFeedback(state.result, resultCopiedNotice)
                },
                onOpenRecipients = { vm.selectSection(TopLevelSection.RECIPIENTS) },
                onOpenMyKeys = { vm.selectSection(TopLevelSection.MY_KEYS) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            TopLevelSection.RECIPIENTS -> RecipientsSection(
                state = state,
                onNameChange = vm::updateRecipientName,
                onPubkeyChange = vm::updateRecipientPubkey,
                onSave = vm::saveRecipient,
                onSelect = vm::selectRecipient,
                onRename = { label ->
                    renameRecipientTarget = label
                    renameRecipientDraft = label
                },
                onDelete = { deleteRecipientTarget = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            TopLevelSection.MY_KEYS -> KeysSection(
                state = state,
                onGenerate = vm::generateIdentity,
                onImport = {
                    importDialogOpen = true
                    importLabel = ""
                    importPrivateKey = ""
                },
                onSelect = vm::selectIdentity,
                onRename = { label ->
                    renameIdentityTarget = label
                    renameIdentityDraft = label
                },
                onCopyPublicKey = { label ->
                    vm.publicKeyFor(label)?.let {
                        copyWithFeedback(it, publicKeyCopiedNotice)
                    }
                },
                onCopyPrivateKey = { privateKeyCopyTarget = it },
                onDelete = { deleteIdentityTarget = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    state.errorMessage?.let { message ->
        LaunchedSnackBar(message, snackbarHostState, vm::clearError)
    }

    state.noticeMessage?.let { message ->
        LaunchedSnackBar(message, snackbarHostState, vm::clearNotice)
    }

    deleteRecipientTarget?.let { name ->
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_recipient_title),
            text = stringResource(R.string.dialog_delete_recipient_body, name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.deleteRecipient(name)
                deleteRecipientTarget = null
            },
            onDismiss = { deleteRecipientTarget = null }
        )
    }

    deleteIdentityTarget?.let { label ->
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_identity_title),
            text = stringResource(R.string.dialog_delete_identity_body, label),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.deleteIdentity(label)
                deleteIdentityTarget = null
            },
            onDismiss = { deleteIdentityTarget = null }
        )
    }

    privateKeyCopyTarget?.let { label ->
        ConfirmationDialog(
            title = stringResource(R.string.dialog_copy_private_key_title),
            text = stringResource(R.string.dialog_copy_private_key_body, label),
            confirmLabel = copyLabel,
            onConfirm = {
                vm.privateKeyFor(label)?.let {
                    copyWithFeedback(it, privateKeyCopiedNotice)
                }
                privateKeyCopyTarget = null
            },
            onDismiss = { privateKeyCopyTarget = null }
        )
    }

    renameRecipientTarget?.let { original ->
        RenameDialog(
            title = stringResource(R.string.dialog_rename_recipient_title),
            value = renameRecipientDraft,
            valueLabel = stringResource(R.string.label_recipient_alias),
            onValueChange = { renameRecipientDraft = it },
            onConfirm = {
                if (vm.renameRecipient(original, renameRecipientDraft)) {
                    renameRecipientTarget = null
                }
            },
            onDismiss = { renameRecipientTarget = null }
        )
    }

    renameIdentityTarget?.let { original ->
        RenameDialog(
            title = stringResource(R.string.dialog_rename_identity_title),
            value = renameIdentityDraft,
            valueLabel = stringResource(R.string.label_identity),
            onValueChange = { renameIdentityDraft = it },
            onConfirm = {
                if (vm.renameIdentity(original, renameIdentityDraft)) {
                    renameIdentityTarget = null
                }
            },
            onDismiss = { renameIdentityTarget = null }
        )
    }

    if (importDialogOpen) {
        ImportIdentityDialog(
            label = importLabel,
            privateKey = importPrivateKey,
            onLabelChange = { importLabel = it },
            onPrivateKeyChange = { importPrivateKey = it },
            onPastePrivateKey = { pasteFromClipboard { importPrivateKey = it } },
            onConfirm = {
                if (vm.importIdentity(importLabel, importPrivateKey)) {
                    importDialogOpen = false
                }
            },
            onDismiss = { importDialogOpen = false }
        )
    }
}

@Composable
private fun MainSection(
    state: AgeUiState,
    onModeSelected: (MainMode) -> Unit,
    onPlaintextChange: (String) -> Unit,
    onCiphertextChange: (String) -> Unit,
    onRecipientSelected: (String) -> Unit,
    onIdentitySelected: (String) -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onClearPlaintext: () -> Unit,
    onClearCiphertext: () -> Unit,
    onClearResult: () -> Unit,
    onPastePlaintext: () -> Unit,
    onPasteCiphertext: () -> Unit,
    onCopyResult: () -> Unit,
    onOpenRecipients: () -> Unit,
    onOpenMyKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(8.dp)) {
                    PrimaryTabRow(selectedTabIndex = state.mainMode.ordinal) {
                        MainMode.entries.forEach { mode ->
                            Tab(
                                selected = state.mainMode == mode,
                                onClick = { onModeSelected(mode) },
                                text = { Text(stringResource(mode.labelRes)) }
                            )
                        }
                    }
                }
            }
        }
        item {
            if (state.mainMode == MainMode.ENCRYPT) {
                WorkflowCard(
                    title = stringResource(R.string.main_encrypt_title),
                    body = stringResource(R.string.main_encrypt_body)
                ) {
                    DropdownSelector(
                        label = stringResource(R.string.label_recipient),
                        options = state.recipients.map { it.first },
                        selected = state.selectedRecipient,
                        placeholder = stringResource(R.string.placeholder_select_recipient),
                        enabled = state.recipients.isNotEmpty(),
                        onSelected = onRecipientSelected
                    )
                    if (state.recipients.isEmpty()) {
                        InlineActionCard(
                            title = stringResource(R.string.main_no_recipients_title),
                            body = stringResource(R.string.main_no_recipients_body),
                            actionLabel = stringResource(R.string.action_open_recipients),
                            onAction = onOpenRecipients
                        )
                    }
                    OutlinedTextField(
                        value = state.plaintext,
                        onValueChange = onPlaintextChange,
                        label = { Text(stringResource(R.string.label_plaintext)) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEncrypt,
                            enabled = state.selectedRecipient.isNotBlank() && state.plaintext.isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_encrypt))
                        }
                        OutlinedButton(onClick = onPastePlaintext) {
                            Text(stringResource(R.string.action_paste))
                        }
                        OutlinedButton(onClick = onClearPlaintext, enabled = state.plaintext.isNotBlank()) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            } else {
                WorkflowCard(
                    title = stringResource(R.string.main_decrypt_title),
                    body = stringResource(R.string.main_decrypt_body)
                ) {
                    DropdownSelector(
                        label = stringResource(R.string.label_identity),
                        options = state.identities,
                        selected = state.selectedIdentity,
                        placeholder = stringResource(R.string.placeholder_select_identity),
                        enabled = state.identities.isNotEmpty(),
                        onSelected = onIdentitySelected
                    )
                    if (state.identities.isEmpty()) {
                        InlineActionCard(
                            title = stringResource(R.string.main_no_identities_title),
                            body = stringResource(R.string.main_no_identities_body),
                            actionLabel = stringResource(R.string.action_open_my_keys),
                            onAction = onOpenMyKeys
                        )
                    }
                    OutlinedTextField(
                        value = state.ciphertext,
                        onValueChange = onCiphertextChange,
                        label = { Text(stringResource(R.string.label_armored_payload)) },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onDecrypt,
                            enabled = state.selectedIdentity.isNotBlank() && state.ciphertext.isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_decrypt))
                        }
                        OutlinedButton(onClick = onPasteCiphertext) {
                            Text(stringResource(R.string.action_paste))
                        }
                        OutlinedButton(onClick = onClearCiphertext, enabled = state.ciphertext.isNotBlank()) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }
        }
        item {
            ResultCard(
                result = state.result,
                onCopy = onCopyResult,
                onClear = onClearResult
            )
        }
    }
}

@Composable
private fun RecipientsSection(
    state: AgeUiState,
    onNameChange: (String) -> Unit,
    onPubkeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = stringResource(R.string.recipients_eyebrow),
                title = stringResource(R.string.recipients_title),
                body = stringResource(R.string.recipients_body)
            )
        }
        item {
            WorkflowCard(
                title = stringResource(R.string.recipients_add_title),
                body = stringResource(R.string.recipients_add_body)
            ) {
                OutlinedTextField(
                    value = state.recipientNameInput,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.label_recipient_alias)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.recipientPubkeyInput,
                    onValueChange = onPubkeyChange,
                    label = { Text(stringResource(R.string.label_age_public_key)) },
                    supportingText = { Text(stringResource(R.string.hint_age_public_key)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onSave,
                        enabled = state.recipientNameInput.isNotBlank() && state.recipientPubkeyInput.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save_recipient))
                    }
                }
            }
        }
        if (state.recipients.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.empty_recipients_title),
                    body = stringResource(R.string.empty_recipients_body)
                )
            }
        } else {
            items(state.recipients, key = { it.first }) { (name, publicKey) ->
                SelectionCard(
                    title = name,
                    subtitle = publicKey,
                    selected = state.selectedRecipient == name,
                    primaryAction = {
                        TextButton(onClick = { onSelect(name) }) {
                            Text(
                                if (state.selectedRecipient == name) {
                                    stringResource(R.string.action_selected)
                                } else {
                                    stringResource(R.string.action_select)
                                }
                            )
                        }
                    },
                    secondaryAction = {
                        TextButton(onClick = { onRename(name) }) {
                            Text(stringResource(R.string.action_rename))
                        }
                    },
                    tertiaryAction = {
                        TextButton(onClick = { onDelete(name) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KeysSection(
    state: AgeUiState,
    onGenerate: () -> Unit,
    onImport: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String) -> Unit,
    onCopyPublicKey: (String) -> Unit,
    onCopyPrivateKey: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = stringResource(R.string.my_keys_eyebrow),
                title = stringResource(R.string.my_keys_title),
                body = stringResource(R.string.my_keys_body)
            )
        }
        item {
            WorkflowCard(
                title = stringResource(R.string.my_keys_create_title),
                body = stringResource(R.string.my_keys_create_body)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onGenerate) {
                        Text(stringResource(R.string.action_generate_identity))
                    }
                    OutlinedButton(onClick = onImport) {
                        Text(stringResource(R.string.action_import_private_key))
                    }
                }
            }
        }
        if (state.identities.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.empty_identities_title),
                    body = stringResource(R.string.empty_identities_body)
                )
            }
        } else {
            items(state.identities, key = { it }) { label ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (state.selectedIdentity == label) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.selectedIdentity == label,
                                onClick = { onSelect(label) },
                                label = {
                                    Text(
                                        if (state.selectedIdentity == label) {
                                            stringResource(R.string.action_selected)
                                        } else {
                                            stringResource(R.string.action_select)
                                        }
                                    )
                                }
                            )
                            OutlinedButton(onClick = { onRename(label) }) {
                                Text(stringResource(R.string.action_rename))
                            }
                            OutlinedButton(onClick = { onCopyPublicKey(label) }) {
                                Text(stringResource(R.string.action_copy_public))
                            }
                        }
                        OutlinedButton(onClick = { onCopyPrivateKey(label) }) {
                            Text(stringResource(R.string.action_copy_private))
                        }
                        HorizontalDivider()
                        TextButton(onClick = { onDelete(label) }) {
                            Text(stringResource(R.string.action_delete_identity))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkflowCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
            }
        )
    }
}

@Composable
private fun ResultCard(
    result: String,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (result.isBlank()) {
                Text(
                    stringResource(R.string.result_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = result,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.result_output_label)) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCopy, enabled = result.isNotBlank()) {
                    Text(stringResource(R.string.action_copy_result))
                }
                OutlinedButton(onClick = onClear) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InlineActionCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    primaryAction: @Composable () -> Unit,
    secondaryAction: @Composable () -> Unit,
    tertiaryAction: @Composable () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                primaryAction()
                secondaryAction()
            }
            tertiaryAction()
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun LaunchedSnackBar(
    message: UiMessage,
    snackbarHostState: SnackbarHostState,
    onShown: (() -> Unit)? = null
) {
    val text = stringResource(message.resId, *message.args.toTypedArray())
    androidx.compose.runtime.LaunchedEffect(text) {
        snackbarHostState.showSnackbar(text)
        onShown?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    placeholder: String,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.ifBlank { placeholder },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = enabled && expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    title: String,
    value: String,
    valueLabel: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(valueLabel) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ImportIdentityDialog(
    label: String,
    privateKey: String,
    onLabelChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onPastePrivateKey: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_import_private_key_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = { Text(stringResource(R.string.label_identity)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = onPrivateKeyChange,
                    label = { Text(stringResource(R.string.label_private_key)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onPastePrivateKey) {
                        Text(stringResource(R.string.action_paste))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_import_private_key)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private val TopLevelSection.icon
    get() = when (this) {
        TopLevelSection.MAIN -> Icons.Outlined.Home
        TopLevelSection.RECIPIENTS -> Icons.Outlined.People
        TopLevelSection.MY_KEYS -> Icons.Outlined.Key
    }

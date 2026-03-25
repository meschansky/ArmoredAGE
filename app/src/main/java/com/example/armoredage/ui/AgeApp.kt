package com.example.armoredage.ui

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeApp(context: Context) {
    val vm: AgeViewModel = viewModel(factory = AgeViewModel.factory(context))
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var pendingDeleteRecipient by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteIdentity by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPrivateKeyCopy by rememberSaveable { mutableStateOf<String?>(null) }

    fun copyWithFeedback(value: String, message: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("armored-age", value)))
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ArmoredAge")
                        Text(
                            "Encrypt, decrypt, and manage AGE identities",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = state.activeSection == section,
                        onClick = { vm.selectSection(section) },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = { Text(section.label) }
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
                onCopyResult = { copyWithFeedback(state.result, "Result copied") },
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
                onDelete = { pendingDeleteRecipient = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            TopLevelSection.MY_KEYS -> KeysSection(
                state = state,
                onGenerate = vm::generateIdentity,
                onSelect = vm::selectIdentity,
                onCopyPublicKey = { label ->
                    vm.publicKeyFor(label)?.let { copyWithFeedback(it, "Public key copied") }
                },
                onCopyPrivateKey = { pendingPrivateKeyCopy = it },
                onDelete = { pendingDeleteIdentity = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    state.error?.let { error ->
        LaunchedSnackBar(error, snackbarHostState)
    }

    state.notice?.let { notice ->
        LaunchedSnackBar(
            message = notice,
            snackbarHostState = snackbarHostState,
            onShown = vm::clearNotice
        )
    }

    pendingDeleteRecipient?.let { name ->
        ConfirmationDialog(
            title = "Delete recipient?",
            text = "Remove '$name' from saved recipients?",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteRecipient(name)
                pendingDeleteRecipient = null
            },
            onDismiss = { pendingDeleteRecipient = null }
        )
    }

    pendingDeleteIdentity?.let { label ->
        ConfirmationDialog(
            title = "Delete identity?",
            text = "Delete '$label' and its stored keys from this device?",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteIdentity(label)
                pendingDeleteIdentity = null
            },
            onDismiss = { pendingDeleteIdentity = null }
        )
    }

    pendingPrivateKeyCopy?.let { label ->
        ConfirmationDialog(
            title = "Copy private key?",
            text = "Private keys grant decrypt access. Copy '$label' to the clipboard?",
            confirmLabel = "Copy",
            onConfirm = {
                vm.privateKeyFor(label)?.let { copyWithFeedback(it, "Private key copied") }
                pendingPrivateKeyCopy = null
            },
            onDismiss = { pendingPrivateKeyCopy = null }
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
    onCopyResult: () -> Unit,
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
            SectionHeader(
                eyebrow = "Main",
                title = "Encode and decode armored AGE payloads",
                body = "Focus on one workflow at a time and keep results easy to copy."
            )
        }
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
                                text = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }
        item {
            if (state.mainMode == MainMode.ENCRYPT) {
                WorkflowCard(
                    title = "Encrypt",
                    body = "Choose a saved recipient and convert plaintext into armored AGE output."
                ) {
                    DropdownSelector(
                        label = "Recipient",
                        options = state.recipients.map { it.first },
                        selected = state.selectedRecipient,
                        placeholder = "Select a recipient",
                        onSelected = onRecipientSelected
                    )
                    OutlinedTextField(
                        value = state.plaintext,
                        onValueChange = onPlaintextChange,
                        label = { Text("Plaintext") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEncrypt,
                            enabled = state.selectedRecipient.isNotBlank() && state.plaintext.isNotBlank()
                        ) {
                            Text("Encrypt")
                        }
                    }
                }
            } else {
                WorkflowCard(
                    title = "Decrypt",
                    body = "Use one of your saved identities to decode an armored AGE payload."
                ) {
                    DropdownSelector(
                        label = "Identity",
                        options = state.identities,
                        selected = state.selectedIdentity,
                        placeholder = "Select an identity",
                        onSelected = onIdentitySelected
                    )
                    OutlinedTextField(
                        value = state.ciphertext,
                        onValueChange = onCiphertextChange,
                        label = { Text("Armored AGE payload") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onDecrypt,
                        enabled = state.selectedIdentity.isNotBlank() && state.ciphertext.isNotBlank()
                    ) {
                        Text("Decrypt")
                    }
                }
            }
        }
        item {
            ResultCard(
                result = state.result,
                error = state.error,
                onCopy = onCopyResult
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
                eyebrow = "Recipients",
                title = "Manage who you encrypt for",
                body = "Save trusted recipient keys once, then pick them instantly from the main flow."
            )
        }
        item {
            WorkflowCard(
                title = "Add recipient",
                body = "Store a recipient alias and public AGE key for reuse."
            ) {
                OutlinedTextField(
                    value = state.recipientNameInput,
                    onValueChange = onNameChange,
                    label = { Text("Recipient alias") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.recipientPubkeyInput,
                    onValueChange = onPubkeyChange,
                    label = { Text("AGE public key") },
                    supportingText = { Text("Expected format: age1...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onSave,
                    enabled = state.recipientNameInput.isNotBlank() && state.recipientPubkeyInput.isNotBlank()
                ) {
                    Text("Save recipient")
                }
            }
        }
        if (state.recipients.isEmpty()) {
            item { EmptyStateCard("No recipients yet", "Add a recipient to enable encryption.") }
        } else {
            items(state.recipients, key = { it.first }) { (name, publicKey) ->
                SelectionCard(
                    title = name,
                    subtitle = publicKey,
                    selected = state.selectedRecipient == name,
                    primaryAction = {
                        TextButton(onClick = { onSelect(name) }) { Text(if (state.selectedRecipient == name) "Selected" else "Select") }
                    },
                    secondaryAction = {
                        TextButton(onClick = { onDelete(name) }) { Text("Delete") }
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
    onSelect: (String) -> Unit,
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
                eyebrow = "My Keys",
                title = "Manage local identities",
                body = "Generate device-stored AGE identities, choose the active one, and copy keys when needed."
            )
        }
        item {
            WorkflowCard(
                title = "Create identity",
                body = "Generate a fresh X25519 identity and store it securely on-device."
            ) {
                Button(onClick = onGenerate) { Text("Generate identity") }
            }
        }
        if (state.identities.isEmpty()) {
            item { EmptyStateCard("No identities yet", "Generate an identity to decrypt messages on this device.") }
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
                                label = { Text(if (state.selectedIdentity == label) "Selected" else "Select") }
                            )
                            OutlinedButton(onClick = { onCopyPublicKey(label) }) { Text("Copy public") }
                        }
                        OutlinedButton(onClick = { onCopyPrivateKey(label) }) { Text("Copy private") }
                        HorizontalDivider()
                        TextButton(onClick = { onDelete(label) }) { Text("Delete identity") }
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
private fun ResultCard(result: String, error: String?, onCopy: () -> Unit) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Result", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            if (result.isBlank()) {
                Text(
                    "Run an encrypt or decrypt action to see output here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = result,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Output") }
                )
                Button(onClick = onCopy) { Text("Copy result") }
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
private fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    primaryAction: @Composable () -> Unit,
    secondaryAction: @Composable () -> Unit
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LaunchedSnackBar(
    message: String,
    snackbarHostState: SnackbarHostState,
    onShown: (() -> Unit)? = null
) {
    androidx.compose.runtime.LaunchedEffect(message) {
        snackbarHostState.showSnackbar(message)
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
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.ifBlank { placeholder },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

private val TopLevelSection.label: String
    get() = when (this) {
        TopLevelSection.MAIN -> "Main"
        TopLevelSection.RECIPIENTS -> "Recipients"
        TopLevelSection.MY_KEYS -> "My Keys"
    }

private val TopLevelSection.icon
    get() = when (this) {
        TopLevelSection.MAIN -> Icons.Outlined.Home
        TopLevelSection.RECIPIENTS -> Icons.Outlined.People
        TopLevelSection.MY_KEYS -> Icons.Outlined.Key
    }

private val MainMode.label: String
    get() = when (this) {
        MainMode.ENCRYPT -> "Encrypt"
        MainMode.DECRYPT -> "Decrypt"
    }

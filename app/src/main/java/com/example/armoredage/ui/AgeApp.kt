package com.example.armoredage.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AgeApp(context: Context) {
    val vm: AgeViewModel = viewModel(factory = AgeViewModel.factory(context))
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Armored AGE Encrypt/Decrypt", style = MaterialTheme.typography.headlineSmall)
        Text("Only armored payloads are accepted for decrypt and generated for encrypt.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Identity management")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::generateIdentity) { Text("Generate identity") }
                }
                DropdownSelector(
                    label = "Identity",
                    options = state.identities,
                    selected = state.selectedIdentity,
                    onSelected = vm::selectIdentity
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Known recipient management")
                OutlinedTextField(
                    value = state.recipientNameInput,
                    onValueChange = vm::updateRecipientName,
                    label = { Text("Recipient alias") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.recipientPubkeyInput,
                    onValueChange = vm::updateRecipientPubkey,
                    label = { Text("AGE public key (age1...) ") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = vm::saveRecipient) { Text("Save recipient") }
                DropdownSelector(
                    label = "Encrypt to recipient",
                    options = state.recipients.map { it.first },
                    selected = state.selectedRecipient,
                    onSelected = vm::selectRecipient
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Encrypt")
                OutlinedTextField(
                    value = state.plaintext,
                    onValueChange = vm::updatePlaintext,
                    label = { Text("Plaintext") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Button(onClick = vm::encrypt) { Text("Encrypt -> armored AGE") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Decrypt")
                OutlinedTextField(
                    value = state.ciphertext,
                    onValueChange = vm::updateCiphertext,
                    label = { Text("Armored AGE payload") },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                Button(onClick = vm::decrypt) { Text("Decrypt") }
            }
        }

        state.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        if (state.result.isNotBlank()) {
            Text("Result")
            OutlinedTextField(
                value = state.result,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Output") }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
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

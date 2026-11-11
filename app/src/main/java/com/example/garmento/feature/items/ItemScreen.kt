package com.example.garmento.feature.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.feature.services.ServicesStore

private fun parseAndImportCsv(text: String) {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return
    // Skip header if present
    val startIndex = if (lines.first().lowercase().contains("item") || lines.first().lowercase().contains("price")) 1 else 0
    for (i in startIndex until lines.size) {
        val parts = lines[i].split(",").map { it.trim().trim('"') }
        if (parts.size < 3) continue
        val name = parts[0]
        val service = parts[1]
        val price = parts[2]
        if (name.isNotBlank()) {
            ItemStore.add(name, service, price)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(onNavigateBack: () -> Unit) {
    val items by ItemStore.items.collectAsStateWithLifecycle()
    val services = ServicesStore.services.collectAsStateWithLifecycle().value

    var itemName by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf(services.firstOrNull() ?: "") }
    var price by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val input = context.contentResolver.openInputStream(uri)
            val text = input?.bufferedReader()?.use { it.readText() } ?: ""
            parseAndImportCsv(text)
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create Item", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Service dropdown (fetched from Services page)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedService,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    services.forEach { svc ->
                        DropdownMenuItem(
                            text = { Text(svc) },
                            onClick = {
                                selectedService = svc
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (editingId == null) {
                        ItemStore.add(itemName, selectedService, price)
                    } else {
                        ItemStore.update(editingId!!, itemName, selectedService, price)
                    }
                    itemName = ""
                    price = ""
                    editingId = null
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (editingId == null) "Add Item" else "Update Item")
            }

            Button(
                onClick = { importLauncher.launch("text/*") },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Import Prices (CSV)")
            }

            Text(
                text = "Sample CSV:\nItem Name,Service,Price\nShirt,Wash Only,50\nPant,Wash & Iron,80\nJeans,Dry Clean,120",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            if (items.isEmpty()) {
                Text("No items yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Service: ${item.service}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Price: ${item.price}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    itemName = item.name
                                    selectedService = item.service
                                    price = item.price
                                    editingId = item.id
                                }) {
                                    Text("Edit")
                                }
                                Button(onClick = { ItemStore.remove(item.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
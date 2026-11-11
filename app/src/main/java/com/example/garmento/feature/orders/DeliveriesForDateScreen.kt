package com.example.garmento.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.core.domain.model.OrderItem
import kotlinx.serialization.json.Json

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DeliveriesForDateScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeliveriesForDateViewModel = hiltViewModel()
) {
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentOrderId by remember { mutableStateOf<String?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (deliveries.isEmpty()) {
                Text("No deliveries scheduled for this date.", style = MaterialTheme.typography.bodyMedium)
            } else {
                deliveries.forEach { d ->
                    val items = try {
                        if (d.itemsJson.isNullOrBlank()) emptyList() else Json.decodeFromString<List<OrderItem>>(d.itemsJson)
                    } catch (_: Exception) { emptyList() }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("#${d.orderNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Total items: ${d.totalItems}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Customer: ${d.customerName}", style = MaterialTheme.typography.bodyMedium)
                            Text("Mobile: ${d.customerPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Total: Rs.${String.format("%.2f", d.totalAmount)}  |  Paid: Rs.${String.format("%.2f", d.paidAmount)}  |  Due: Rs.${String.format("%.2f", d.totalAmount - d.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            if (items.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Items:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                items.forEach { it ->
                                    Text("  ${it.clothesType} x${it.quantity} @ Rs.${String.format("%.0f", it.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.markDelivered(d.orderId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as Delivered")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    paymentOrderId = d.orderId
                                    paymentAmount = String.format("%.2f", d.totalAmount - d.paidAmount)
                                    paymentMethod = "cash"
                                    showPaymentDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Collect Payment")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Collect Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val due = deliveries.find { it.orderId == paymentOrderId }?.let { it.totalAmount - it.paidAmount } ?: 0.0
                    Text("Amount Due: Rs.${String.format("%.2f", due)}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("cash", "upi").forEach { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(m.uppercase()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = paymentAmount.toDoubleOrNull() ?: 0.0
                    paymentOrderId?.let { viewModel.recordPayment(it, amt, paymentMethod) }
                    showPaymentDialog = false
                }) { Text("Collect") }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }
}
package com.example.garmento.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.garmento.core.domain.model.DeliveryStatus
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.dashboard.MainBottomBar
import com.example.garmento.feature.dashboard.MainTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacedOrdersScreen(
    onNavigateToTab: (MainTab) -> Unit = {},
    viewModel: PlacedOrdersViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var targetOrder by remember { mutableStateOf<PlacedOrderRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No orders placed yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(rows, key = { it.orderId }) { row ->
                    PlacedOrderCard(
                        row = row,
                        onMarkDelivery = { targetOrder = row }
                    )
                }
            }
        }
    }

    targetOrder?.let { order ->
        MarkDeliveryDialog(
            order = order,
            onDismiss = { targetOrder = null },
            onConfirm = { itemsLeft, note ->
                viewModel.markDelivered(order.orderId, itemsLeft, note.ifBlank { null })
                targetOrder = null
            }
        )
    }
}

@Composable
private fun MarkDeliveryDialog(
    order: PlacedOrderRow,
    onDismiss: () -> Unit,
    onConfirm: (itemsLeft: Int?, note: String) -> Unit
) {
    var itemsLeftText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${order.customerName} · due ${order.deliveryDate ?: "—"}"
                )
                OutlinedTextField(
                    value = itemsLeftText,
                    onValueChange = { itemsLeftText = it },
                    label = { Text("Items left for delivery (blank = fully delivered)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(itemsLeftText.toIntOrNull(), note) }) {
                Text(if (itemsLeftText.toIntOrNull()?.let { it > 0 } == true) "Mark Partial" else "Mark Delivered")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PlacedOrderCard(
    row: PlacedOrderRow,
    onMarkDelivery: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isVoided) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${row.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = row.orderDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${row.customerName} · ${row.customerPhone}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${row.clothesCount} clothes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.clothesOverage > 0) {
                    Text(
                        text = " · ${row.clothesOverage} over quota",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (row.isVoided) {
                    Text(
                        text = " · VOIDED",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            row.deliveryDate?.let { date ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (row.deliveryStatus) {
                        DeliveryStatus.DELIVERED -> "DELIVERED · $date"
                        DeliveryStatus.PARTIAL -> "PARTIAL DELIVERY · $date" +
                            (row.deliveryNotes?.let { " · $it" } ?: "")
                        DeliveryStatus.PENDING -> "Due $date"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (row.deliveryStatus) {
                        DeliveryStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                        DeliveryStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                        DeliveryStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (UserSession.isAdmin && !row.isVoided && row.deliveryStatus != DeliveryStatus.DELIVERED) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onMarkDelivery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark Delivered")
                }
            }
        }
    }
}
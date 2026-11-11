package com.example.garmento.feature.customers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.util.Constants
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.customer_portal.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditCustomer: (String) -> Unit,
    onNavigateToNewSubscription: (String) -> Unit,
    onNavigateToPlaceOrder: (String, String?) -> Unit,
    onNavigateToOrderEdit: (String) -> Unit,
    viewModel: CustomersViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    var subscriptionExpanded by remember { mutableStateOf(false) }
    val isAdmin = UserSession.isAdmin

    LaunchedEffect(Unit) {
        // Detail state is loaded in ViewModel init
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            detailState.customer?.let { onNavigateToEditCustomer(it.id.toString()) }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
                    }
                }
            )
        }
    ) { padding ->
        when {
            detailState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            detailState.customer == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Customer not found")
                }
            }
            else -> {
                val customer = detailState.customer!!
                val sub = detailState.selectedSubscription

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Contact", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Phone: ${customer.phone}")
                            if (customer.address.isNotBlank()) {
                                Text("Address: ${customer.address}")
                            }
                            if (customer.notes.isNotBlank()) {
                                Text("Notes: ${customer.notes}")
                            }
                            Text("Code: ${customer.publicCode}")
                        }
                    }

                    if (detailState.subscriptions.isNotEmpty()) {
                        Text("Subscription", style = MaterialTheme.typography.titleSmall)
                        ExposedDropdownMenuBox(
                            expanded = subscriptionExpanded,
                            onExpandedChange = { subscriptionExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = sub?.let { s ->
                                    "${s.startDate} — ${s.endDate} | ${s.remainingClothes}/${s.clothesQuota} clothes | ${s.status.name}"
                                } ?: "No subscription",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select subscription") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subscriptionExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = subscriptionExpanded,
                                onDismissRequest = { subscriptionExpanded = false }
                            ) {
                                detailState.subscriptions.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("${s.startDate} — ${s.endDate}", style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    "${s.remainingClothes}/${s.clothesQuota} clothes | ${s.status.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when {
                                                        s.status == SubscriptionStatus.ACTIVE && !s.isExpired ->
                                                            MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectDetailSubscription(s.id.toString())
                                            subscriptionExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (sub != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Selected Subscription",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Days left: ${sub.daysRemaining}")
                                        Text("${sub.remainingClothes} clothes left")
                                    }
                                    if (sub.clothesOverage > 0) {
                                        Text(
                                            "+${sub.clothesOverage} over quota",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("No active subscription")
                            }
                        }
                    }

                    // Self-Service Portal QR
                    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    val qrGenerator = remember { QrCodeGenerator() }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Self-Service Portal", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.height(200.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Code: ${customer.publicCode}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Button(
                                    onClick = {
                                        val url = "${Constants.PORTAL_BASE_URL}${customer.publicCode}"
                                        qrBitmap = qrGenerator.generateQrBitmap(url)
                                    }
                                ) {
                                    Text("Generate QR Code")
                                }
                            }
                        }
                    }

                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToNewSubscription(customer.id.toString()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (sub != null && !sub.isExpired) "Renew Plan" else "New Subscription"
                                )
                            }
                            OutlinedButton(
                                onClick = { onNavigateToPlaceOrder(customer.id.toString(), sub?.id.toString()) },
                                modifier = Modifier.weight(1f),
                                enabled = sub != null
                            ) {
                                Text("Place Order")
                            }
                        }
                    }

                    Text("Subscription History", style = MaterialTheme.typography.titleSmall)
                    detailState.subscriptions.forEach { subscription ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${subscription.startDate} — ${subscription.endDate}")
                                    Text(
                                        subscription.status.name,
                                        color = when (subscription.status) {
                                            SubscriptionStatus.ACTIVE ->
                                                MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Text("${subscription.clothesUsed}/${subscription.clothesQuota} clothes used")
                            }
                        }
                    }
                }
            }
        }
    }
}

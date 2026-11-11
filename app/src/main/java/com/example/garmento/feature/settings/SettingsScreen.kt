package com.example.garmento.feature.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.orders.OrderLookupViewModel
import com.example.garmento.ui.components.GlassBackground
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStaff: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    orderLookupVm: OrderLookupViewModel = hiltViewModel(),
    expiringVm: ExpiringSubscriptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lookupState by orderLookupVm.uiState.collectAsStateWithLifecycle()
    val expiring by expiringVm.items.collectAsStateWithLifecycle()
    val isAdmin = UserSession.isAdmin

    LaunchedEffect(Unit) { expiringVm.refresh() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        GlassBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Username
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Logged in as: ${uiState.username.ifBlank { "admin" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Company switcher
            if (uiState.companies.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Company", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.companies.size > 1) {
                            var expanded by remember { mutableStateOf(false) }
                            val selectedName = uiState.companies
                                .firstOrNull { it.id == uiState.currentCompanyId }?.name
                                ?: uiState.companies.firstOrNull()?.name
                                ?: ""
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    uiState.companies.forEach { company ->
                                        DropdownMenuItem(
                                            text = { Text(company.name) },
                                            onClick = {
                                                viewModel.switchCompany(company.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                uiState.companies.firstOrNull()?.name ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (isAdmin) {
                // Grace Period
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Grace Period", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Allow orders for ${uiState.gracePeriodDays} day(s) after subscription expires",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var sliderPosition by remember {
                            mutableFloatStateOf(uiState.gracePeriodDays.toFloat())
                        }

                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                viewModel.onGracePeriodChange(sliderPosition.roundToInt())
                            },
                            valueRange = 0f..7f,
                            steps = 6
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0 days", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${sliderPosition.roundToInt()} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("7 days", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Dark Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Use dark theme",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.isDarkMode,
                        onCheckedChange = viewModel::onDarkModeToggle
                    )
                }
            }

            if (isAdmin) {
                // Staff Management
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToStaff,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Staff Management", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Manage staff roles and access",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Order lookup & delete
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Order Lookup", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = lookupState.orderNumber,
                            onValueChange = orderLookupVm::onOrderNumberChange,
                            label = { Text("Order Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { orderLookupVm.fetch() }) { Text("Fetch") }
                            Button(onClick = { orderLookupVm.delete() }) { Text("Delete") }
                        }
                        lookupState.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        lookupState.foundOrder?.let { o ->
                            Text("Customer: ${o.customer_id}", style = MaterialTheme.typography.bodySmall)
                            Text("Date: ${o.order_date}", style = MaterialTheme.typography.bodySmall)
                            Text("Clothes: ${o.clothes_count}", style = MaterialTheme.typography.bodySmall)
                            Text("Total: Rs.${String.format("%.2f", o.total_amount)}", style = MaterialTheme.typography.bodySmall)
                            Text("Paid: Rs.${String.format("%.2f", o.paid_amount)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (isAdmin) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Expiring in 3 Days", style = MaterialTheme.typography.titleSmall)
                        if (expiring.isEmpty()) {
                            Text("No subscriptions expiring soon.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            expiring.forEach { sub ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${sub.customerName} — ${sub.planName}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Text("Ends: ${sub.endDate}", style = MaterialTheme.typography.bodySmall)
                                        Text("Mobile: ${sub.customerPhone}", style = MaterialTheme.typography.bodySmall)
                                        val ctx = LocalContext.current
                                        Button(onClick = {
                                            val msg = "Hi ${sub.customerName}, your ${sub.planName} subscription ends on ${sub.endDate}. Please renew to continue laundry service."
                                            val phone = sub.customerPhone.replace(Regex("[^0-9]"), "")
                                            val uri = android.net.Uri.parse("https://wa.me/$phone?text=${android.net.Uri.encode(msg)}")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                            try { ctx.startActivity(intent) } catch (_: Exception) { }
                                        }) {
                                            Text("Send WhatsApp")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout
            Button(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        }
        }
    }
}

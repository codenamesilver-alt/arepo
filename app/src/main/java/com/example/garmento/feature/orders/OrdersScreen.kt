package com.example.garmento.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.dashboard.MainBottomBar
import com.example.garmento.feature.dashboard.MainTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNewSubscription: (customerId: String) -> Unit = {},
    onNavigateToTab: (MainTab) -> Unit = {},
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.placeOrderState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var customerExpanded by remember { mutableStateOf(false) }
    var subscriptionExpanded by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }
    val itemEntries by com.example.garmento.feature.items.ItemStore.items.collectAsStateWithLifecycle()

    val selectedCustomer = remember(uiState.customerId, customers) {
        customers.find { it.id.toString() == uiState.customerId }
    }

    // Auto-fetch prices for items from the Item page (for non-subscription orders).
    LaunchedEffect(uiState.service, uiState.isSubscriptionOrder) {
        if (!uiState.isSubscriptionOrder) {
            uiState.items.forEachIndexed { index, item ->
                if (item.unitPrice.isBlank() || item.unitPrice == "0") {
                    val serviceForPrice = uiState.service.ifBlank { com.example.garmento.feature.services.ServicesStore.services.value.firstOrNull() ?: "" }
                    val priceForItem = com.example.garmento.feature.items.ItemStore.items.value
                        .firstOrNull { it.name.trim().equals(item.clothesType.trim(), ignoreCase = true) && it.service.trim().equals(serviceForPrice.trim(), ignoreCase = true) }
                        ?.price
                        ?: com.example.garmento.feature.items.ItemStore.items.value
                            .firstOrNull { it.name.trim().equals(item.clothesType.trim(), ignoreCase = true) }
                            ?.price
                    if (!priceForItem.isNullOrBlank()) {
                        viewModel.onItemChange(index, item.copy(unitPrice = priceForItem))
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.showLowQuotaWarning) {
        if (uiState.showLowQuotaWarning) {
            snackbarHostState.showSnackbar(
                "Low quota: only ${uiState.lowQuotaRemaining} clothes remaining on this subscription"
            )
            viewModel.consumeLowQuotaWarning()
        }
    }

    val canPlaceOrder = !uiState.isSaving &&
            !uiState.isCreatingSubscription &&
            !uiState.success &&
            uiState.customerId != null &&
            uiState.orderNumber.isNotBlank() &&
            uiState.clothesCountInt > 0 &&
            uiState.deliveryDate != null

    val canBookOrder = UserSession.isAdmin || UserSession.isStaff

    var showDeliveryPicker by rememberSaveable { mutableStateOf(false) }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!canBookOrder) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You have read-only access",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Only the admin can create subscriptions and place orders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Service", style = MaterialTheme.typography.titleSmall)
            var serviceExpanded by remember { mutableStateOf(false) }
            val services = com.example.garmento.feature.services.ServicesStore.services.collectAsStateWithLifecycle().value
            ExposedDropdownMenuBox(
                expanded = serviceExpanded,
                onExpandedChange = { serviceExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.service.ifBlank { services.firstOrNull() ?: "" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select service") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = serviceExpanded,
                    onDismissRequest = { serviceExpanded = false }
                ) {
                    services.forEach { svc ->
                        DropdownMenuItem(
                            text = { Text(svc) },
                            onClick = {
                                viewModel.onServiceChange(svc)
                                serviceExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Customer", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (customerExpanded) customerSearchQuery else selectedCustomer?.let {
                        "${it.name} (${it.phone})"
                    } ?: "",
                    onValueChange = {
                        customerSearchQuery = it
                        viewModel.onCustomerSearchQueryChange(it)
                        customerExpanded = true
                    },
                    label = { Text("Select customer") },
                    readOnly = !customerExpanded,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                )
                ExposedDropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = { customerExpanded = false }
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(customer.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        customer.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectOrderCustomer(customer)
                                customerSearchQuery = ""
                                customerExpanded = false
                            }
                        )
                    }
                    if (customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { customerExpanded = false }
                        )
                    }
                }
            }

            if (uiState.customerId != null) {
                Text("Order Type", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.isSubscriptionOrder,
                        onClick = { viewModel.onOrderTypeChange(true) },
                        label = { Text("Subscription") }
                    )
                    FilterChip(
                        selected = !uiState.isSubscriptionOrder,
                        onClick = { viewModel.onOrderTypeChange(false) },
                        label = { Text("Non-subscription") }
                    )
                }
            }

            if (uiState.isSubscriptionOrder && uiState.customerId != null && subscriptions.isEmpty()) {
                Text("No subscription yet — activate a plan", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Pick a plan below to start the subscription immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plans.isEmpty()) {
                    Text(
                        "No active plans found. Create one in the Plans tab first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    plans.forEach { plan ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(plan.name, style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${plan.durationDays} days")
                                    Text("Rs.${String.format("%.0f", plan.amount)}")
                                    Text("${plan.totalClothes} clothes")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.createSubscriptionForCustomer(plan) },
                                    enabled = !uiState.isCreatingSubscription,
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text(
                                        when {
                                            uiState.isCreatingSubscription -> "Starting..."
                                            else -> "Start subscription"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isSubscriptionOrder && uiState.customerId != null && subscriptions.isNotEmpty()) {
                Text("Subscription", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = subscriptionExpanded,
                    onExpandedChange = { subscriptionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.subscription?.let { sub ->
                            "${sub.startDate} — ${sub.endDate} | ${sub.remainingClothes}/${sub.clothesQuota} clothes"
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
                        subscriptions.forEach { sub ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "${sub.startDate} — ${sub.endDate}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            "${sub.remainingClothes}/${sub.clothesQuota} clothes | ${sub.status.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (sub.status.name == "ACTIVE" && !sub.isExpired) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectSubscription(sub)
                                    subscriptionExpanded = false
                                }
                            )
                        }
                        if (subscriptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No subscriptions found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { subscriptionExpanded = false }
                            )
                        }
                    }
                }
            }

            if (uiState.isSubscriptionOrder && uiState.subscription != null) {
                val sub = uiState.subscription!!

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Subscription Details", style = MaterialTheme.typography.titleSmall)
                            Row {
                                TextButton(onClick = { viewModel.onToggleSubscriptionEdit() }) {
                                    Text(
                                        if (uiState.isEditingSubscription) "Cancel" else "Edit",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = { onNavigateToNewSubscription(sub.customerId.toString()) }) {
                                    Text("Renew", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.isEditingSubscription) {
                            OutlinedTextField(
                                value = uiState.editClothesRemaining,
                                onValueChange = viewModel::onEditClothesRemainingChange,
                                label = { Text("Clothes remaining") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.editDaysRemaining,
                                onValueChange = viewModel::onEditDaysRemainingChange,
                                label = { Text("Days left") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Set the current run's remaining counts — useful for manually-added students who already started their subscription.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.saveSubscriptionEdits() },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("Save Subscription")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Start: ${sub.startDate}", style = MaterialTheme.typography.bodySmall)
                                Text("End: ${sub.endDate}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val daysText = "${sub.daysRemaining} days left"
                                Text(daysText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                val clothesText = "${sub.remainingClothes}/${sub.clothesQuota} clothes left"
                                Text(clothesText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            if (sub.isExpired) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Expired — renew to keep placing orders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                }
                Text("Payment Method", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash", "upi", "card").forEach { method ->
                        FilterChip(
                            selected = uiState.paymentMethod == method,
                            onClick = { viewModel.onPaymentMethodChange(method) },
                            label = { Text(method.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                if (uiState.isSubscriptionOrder) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Quota Preview",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Quota balance left: ${uiState.remainingBefore}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.orderNumber,
                    onValueChange = {},
                    label = { Text("Order Number (auto)") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Items", style = MaterialTheme.typography.titleSmall)
                uiState.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var typeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = item.clothesType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                val serviceForPrice = uiState.service.ifBlank { com.example.garmento.feature.services.ServicesStore.services.value.firstOrNull() ?: "" }.trim().lowercase()
                                val itemNames = itemEntries
                                    .filter { it.service.trim().lowercase() == serviceForPrice }
                                    .map { it.name }
                                    .distinct()
                                    .ifEmpty { itemEntries.map { it.name }.distinct() }
                                itemNames.forEach { t ->
                                    val priceForItem = itemEntries
                                        .firstOrNull { it.name.trim().equals(t.trim(), ignoreCase = true) && it.service.trim().equals(serviceForPrice, ignoreCase = true) }
                                        ?.price
                                        ?: itemEntries.firstOrNull { it.name.trim().equals(t.trim(), ignoreCase = true) }?.price
                                        ?: ""
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            viewModel.onItemChange(index, item.copy(clothesType = t, unitPrice = if (priceForItem.isNotBlank()) priceForItem else item.unitPrice))
                                            typeExpanded = false
                                        }
                                    )
                                }
                                if (itemNames.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No items — add in Item page") },
                                        onClick = { typeExpanded = false }
                                    )
                                }
                            }
                        }
                        var qtyExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = qtyExpanded,
                            onExpandedChange = { qtyExpanded = it },
                            modifier = Modifier.width(72.dp)
                        ) {
                            OutlinedTextField(
                                value = item.quantity,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Qty", fontSize = 11.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qtyExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = qtyExpanded,
                                onDismissRequest = { qtyExpanded = false }
                            ) {
                                (1..100).forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q.toString()) },
                                        onClick = {
                                            viewModel.onItemChange(index, item.copy(quantity = q.toString()))
                                            qtyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        if (!uiState.isSubscriptionOrder) {
                            OutlinedTextField(
                                value = item.unitPrice,
                                onValueChange = { viewModel.onItemChange(index, item.copy(unitPrice = it)) },
                                label = { Text("Price") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                        if (uiState.items.size > 1) {
                            TextButton(onClick = { viewModel.removeItem(index) }) { Text("✕") }
                        }
                    }
                }
                TextButton(onClick = { viewModel.addItem() }) { Text("+ Add item") }
                Text(
                    "Total items: ${uiState.totalItemsQuantity}" +
                        if (!uiState.isSubscriptionOrder) " · Total: Rs.${String.format("%.0f", uiState.totalAmount)}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!uiState.isSubscriptionOrder) {
                    OutlinedTextField(
                        value = uiState.discount,
                        onValueChange = viewModel::onDiscountChange,
                        label = { Text("Discount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Final Order Value: Rs.${String.format("%.2f", uiState.finalAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = { showDeliveryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.deliveryDate == null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Text(
                        text = uiState.deliveryDate?.let { "Delivery Date: $it" } ?: "Select Delivery Date",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
                if (uiState.deliveryDate == null) {
                    Text(
                        "Delivery date is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = uiState.deliveryNotes,
                    onValueChange = viewModel::onDeliveryNotesChange,
                    label = { Text("Delivery Notes / Comments") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.placeOrder(onNavigateBack) },
                    enabled = canPlaceOrder,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Place Order")
                    }
                }

                if (showDeliveryPicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = uiState.deliveryDate?.let {
                            it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDeliveryPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    viewModel.onDeliveryDateChange(
                                        java.time.Instant.ofEpochMilli(millis)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate()
                                    )
                                }
                                showDeliveryPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeliveryPicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState, showModeToggle = false)
                    }
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

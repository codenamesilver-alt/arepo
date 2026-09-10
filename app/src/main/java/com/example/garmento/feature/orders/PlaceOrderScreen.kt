package com.example.garmento.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceOrderScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.placeOrderState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showLowQuotaWarning) {
        if (uiState.showLowQuotaWarning) {
            snackbarHostState.showSnackbar(
                "Low quota: only ${uiState.lowQuotaRemaining} clothes remaining on this subscription"
            )
            viewModel.consumeLowQuotaWarning()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place Order") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.orderNumber,
                onValueChange = viewModel::onOrderNumberChange,
                label = { Text("Order Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.clothesCount,
                onValueChange = viewModel::onClothesCountChange,
                label = { Text("Clothes Count") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.subscription != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isOverQuota) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Quota Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (uiState.isOverQuota) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Before: ${uiState.remainingBefore} remaining")
                            Text("After: ${uiState.projectedRemaining}")
                        }
                        if (uiState.isOverQuota) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "+${uiState.overageAmount} over quota",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.placeOrder(onNavigateBack) },
                enabled = !uiState.isSaving && !uiState.success,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Order")
                }
            }
        }
    }
}

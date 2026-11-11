package com.example.garmento.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.util.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class OrderHistoryUiState(
    val query: String = "",
    val orders: List<OrderWithCustomerRow> = emptyList()
)

data class OrderWithCustomerRow(
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryDate: String,
    val clothesCount: Int,
    val totalAmount: Double
)

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderDao: OrderDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init { load() }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val cid = UserSession.currentCompanyId ?: return@launch
            val all = orderDao.getAllOrdersWithCustomers(cid)
            // Collect latest snapshot
            all.collect { rows ->
                val q = _uiState.value.query.trim().lowercase()
                val sevenDaysAgo = LocalDate.now().minusDays(7)
                val filtered = rows.filter { r ->
                    val matchesQuery = q.isBlank() ||
                        r.customer_name.lowercase().contains(q) ||
                        r.customer_phone.contains(q) ||
                        r.order_number.contains(q)
                    val within7 = try {
                        val d = LocalDate.parse((r.order_date ?: "").take(10))
                        !d.isBefore(sevenDaysAgo)
                    } catch (_: Exception) { true }
                    val dateOk = q.isNotBlank() || within7
                    matchesQuery && dateOk
                }.sortedByDescending { it.order_date }
                _uiState.update {
                    it.copy(orders = filtered.map { r ->
                        OrderWithCustomerRow(
                            orderNumber = r.order_number,
                            customerName = r.customer_name,
                            customerPhone = r.customer_phone,
                            deliveryDate = r.delivery_date ?: "",
                            clothesCount = r.clothes_count,
                            totalAmount = r.total_amount
                        )
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search by name, mobile or order number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.orders.isEmpty()) {
                Text("No orders found.", style = MaterialTheme.typography.bodyMedium)
            } else {
                uiState.orders.forEach { o ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Order #${o.orderNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Customer: ${o.customerName}", style = MaterialTheme.typography.bodySmall)
                            Text("Mobile: ${o.customerPhone}", style = MaterialTheme.typography.bodySmall)
                            Text("Delivery: ${o.deliveryDate}", style = MaterialTheme.typography.bodySmall)
                            Text("Items: ${o.clothesCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
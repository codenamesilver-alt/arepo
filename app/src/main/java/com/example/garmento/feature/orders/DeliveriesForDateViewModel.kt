package com.example.garmento.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.feature.dashboard.DashboardRepository
import com.example.garmento.feature.dashboard.DeliveryDetailRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveriesForDateViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val date: String = savedStateHandle.get<String>("date") ?: java.time.LocalDate.now().toString()

    val deliveries: StateFlow<List<DeliveryDetailRow>> =
        dashboardRepository.getDeliveriesForDateFull(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markDelivered(orderId: String) {
        viewModelScope.launch { orderRepository.markDeliveredById(orderId) }
    }

    fun recordPayment(orderId: String, amount: Double, method: String) {
        viewModelScope.launch { orderRepository.recordPayment(orderId, amount, method) }
    }
}
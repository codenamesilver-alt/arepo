package com.example.garmento.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.feature.orders.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderLookupUiState(
    val orderNumber: String = "",
    val foundOrder: OrderEntity? = null,
    val message: String? = null,
    val isDeleting: Boolean = false
)

@HiltViewModel
class OrderLookupViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderLookupUiState())
    val uiState: StateFlow<OrderLookupUiState> = _uiState.asStateFlow()

    fun onOrderNumberChange(value: String) {
        _uiState.update { it.copy(orderNumber = value, foundOrder = null, message = null) }
    }

    fun fetch() {
        val num = _uiState.value.orderNumber.trim()
        if (num.isBlank()) return
        viewModelScope.launch {
            try {
                val order = orderRepository.getOrderByNumber(num)
                if (order != null) {
                    _uiState.update { it.copy(foundOrder = order, message = null) }
                } else {
                    _uiState.update { it.copy(foundOrder = null, message = "Order not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(foundOrder = null, message = e.message ?: "Error") }
            }
        }
    }

    fun delete() {
        val order = _uiState.value.foundOrder ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                orderRepository.deleteOrderById(order.id)
                _uiState.update { it.copy(foundOrder = null, orderNumber = "", message = "Order deleted", isDeleting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, message = e.message ?: "Delete failed") }
            }
        }
    }
}
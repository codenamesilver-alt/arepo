package com.example.garmento.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.domain.model.DeliveryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlacedOrderRow(
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val clothesCount: Int,
    val clothesOverage: Int,
    val orderDate: String,
    val isVoided: Boolean,
    val voidReason: String?,
    val deliveryDate: String?,
    val deliveryStatus: DeliveryStatus,
    val deliveryNotes: String?
)

@HiltViewModel
class PlacedOrdersViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val orderRepository: OrderRepository
) : ViewModel() {

    val rows: StateFlow<List<PlacedOrderRow>> = orderDao.getAllOrdersWithCustomers()
        .map { list ->
            list.map {
                PlacedOrderRow(
                    orderId = it.id,
                    orderNumber = it.order_number,
                    customerName = it.customer_name,
                    customerPhone = it.customer_phone,
                    clothesCount = it.clothes_count,
                    clothesOverage = it.clothes_overage,
                    orderDate = it.order_date.take(10),
                    isVoided = it.is_voided,
                    voidReason = it.void_reason,
                    deliveryDate = it.delivery_date,
                    deliveryStatus = DeliveryStatus.fromValue(it.delivery_status),
                    deliveryNotes = it.delivery_notes
                )
            }.sortedWith { a, b ->
                val an = a.orderNumber.toLongOrNull()
                val bn = b.orderNumber.toLongOrNull()
                when {
                    an != null && bn != null -> bn.compareTo(an)
                    an != null -> -1
                    bn != null -> 1
                    else -> b.orderNumber.compareTo(a.orderNumber)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markDelivered(orderId: String, itemsLeft: Int?, note: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val order = orderRepository.getOrderById(orderId) ?: return@launch
            orderRepository.markDelivered(order, itemsLeft, note)
            onDone()
        }
    }
}
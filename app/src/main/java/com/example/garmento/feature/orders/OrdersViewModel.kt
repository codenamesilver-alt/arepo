package com.example.garmento.feature.orders

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import com.example.garmento.core.domain.model.Order
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.feature.customers.CustomerRepository
import com.example.garmento.feature.customers.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import javax.inject.Inject

data class PlaceOrderUiState(
    val customerId: String? = null,
    val subscription: Subscription? = null,
    val orderNumber: String = "",
    val clothesCount: String = "",
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val showLowQuotaWarning: Boolean = false,
    val lowQuotaRemaining: Int = 0
) {
    val clothesCountInt: Int get() = clothesCount.toIntOrNull() ?: 0
    val remainingBefore: Int get() = subscription?.remainingClothes ?: 0
    val projectedRemaining: Int get() = remainingBefore - clothesCountInt
    val isOverQuota: Boolean get() = projectedRemaining < 0
    val overageAmount: Int get() = if (isOverQuota) -projectedRemaining else 0
}

data class OrderEditUiState(
    val order: Order? = null,
    val newClothesCount: String = "",
    val voidReason: String = "",
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val customerRepository: CustomerRepository,
    private val pendingSyncDao: PendingSyncDao,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerIdArg: String? = savedStateHandle.get<String>("customerId")
    private val orderIdArg: String? = savedStateHandle.get<String>("orderId")

    private val _placeOrderState = MutableStateFlow(PlaceOrderUiState())
    val placeOrderState: StateFlow<PlaceOrderUiState> = _placeOrderState.asStateFlow()

    private val _editState = MutableStateFlow(OrderEditUiState())
    val editState: StateFlow<OrderEditUiState> = _editState.asStateFlow()

    init {
        customerIdArg?.let { loadSubscriptionForOrder(it) }
        orderIdArg?.let { loadOrderForEdit(it) }
    }

    private fun loadSubscriptionForOrder(customerId: String) {
        viewModelScope.launch {
            val sub = subscriptionRepository.getSubscriptionById(
                customerRepository.getActiveSubscription(customerId)?.id.toString()
            ) ?: customerRepository.getActiveSubscription(customerId)
            _placeOrderState.update {
                it.copy(
                    customerId = customerId,
                    subscription = sub
                )
            }
        }
    }

    fun onOrderNumberChange(value: String) {
        _placeOrderState.update { it.copy(orderNumber = value, error = null) }
    }

    fun onClothesCountChange(value: String) {
        _placeOrderState.update { it.copy(clothesCount = value, error = null) }
    }

    fun placeOrder(onSuccess: () -> Unit) {
        val state = _placeOrderState.value
        val sub = state.subscription
        val clothesCount = state.clothesCountInt

        if (sub == null) {
            _placeOrderState.update { it.copy(error = "No active subscription found") }
            return
        }
        if (state.orderNumber.isBlank()) {
            _placeOrderState.update { it.copy(error = "Order number is required") }
            return
        }
        if (clothesCount <= 0) {
            _placeOrderState.update { it.copy(error = "Clothes count must be positive") }
            return
        }

        viewModelScope.launch {
            _placeOrderState.update { it.copy(isSaving = true, error = null) }
            try {
                if (!isNetworkAvailable()) {
                    val orderPayload = buildJsonObject {
                        put("subscription_id", JsonPrimitive(sub.id.toString()))
                        put("order_number", JsonPrimitive(state.orderNumber))
                        put("clothes_count", JsonPrimitive(clothesCount))
                        put("clothes_from_quota", JsonPrimitive(minOf(clothesCount, sub.remainingClothes)))
                        put("clothes_overage", JsonPrimitive(maxOf(clothesCount - sub.remainingClothes, 0)))
                        put("is_voided", JsonPrimitive(false))
                        put("grace_period", JsonPrimitive(false))
                    }
                    pendingSyncDao.insertPendingItem(
                        PendingSyncEntity(
                            id = UUID.randomUUID().toString(),
                            table_name = "orders",
                            payload_json = Json.encodeToString(orderPayload),
                            created_at = java.time.LocalDateTime.now().toString(),
                            synced = false
                        )
                    )
                    _placeOrderState.update {
                        it.copy(
                            isSaving = false,
                            success = true,
                            error = "Saved offline — will sync when connected"
                        )
                    }
                    onSuccess()
                } else {
                    orderRepository.placeOrder(sub, state.orderNumber, clothesCount)
                    _placeOrderState.update { it.copy(isSaving = false, success = true) }
                    onSuccess()
                }
            } catch (e: Exception) {
                _placeOrderState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun consumeLowQuotaWarning() {
        _placeOrderState.update { it.copy(showLowQuotaWarning = false) }
    }

    private fun loadOrderForEdit(orderId: String) {
        viewModelScope.launch {
            val order = orderRepository.getOrderById(orderId)
            _editState.update {
                OrderEditUiState(
                    order = order,
                    newClothesCount = order?.clothesCount?.toString() ?: ""
                )
            }
        }
    }

    fun onEditClothesCountChange(value: String) {
        _editState.update { it.copy(newClothesCount = value, error = null) }
    }

    fun onVoidReasonChange(value: String) {
        _editState.update { it.copy(voidReason = value, error = null) }
    }

    fun saveOrderEdit(onSuccess: () -> Unit) {
        val state = _editState.value
        val order = state.order ?: return
        val newCount = state.newClothesCount.toIntOrNull()

        if (newCount == null || newCount <= 0) {
            _editState.update { it.copy(error = "Clothes count must be positive") }
            return
        }

        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true) }
            try {
                orderRepository.updateOrder(order, newCount, "Edited via admin")
                _editState.update { it.copy(isSaving = false, success = true) }
                onSuccess()
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun voidOrder(onSuccess: () -> Unit) {
        val state = _editState.value
        val order = state.order ?: return
        val reason = state.voidReason.ifBlank { "Voided by admin" }

        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true) }
            try {
                orderRepository.voidOrder(order, reason)
                _editState.update { it.copy(isSaving = false, success = true) }
                onSuccess()
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

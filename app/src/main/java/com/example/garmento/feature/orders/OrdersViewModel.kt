package com.example.garmento.feature.orders

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.util.UserSession
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Order
import com.example.garmento.core.domain.model.OrderItem
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import com.example.garmento.feature.customers.CustomerRepository
import com.example.garmento.feature.customers.SubscriptionRepository
import com.example.garmento.feature.plans.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class OrderLineItem(
    val clothesType: String = "Shirt",
    val quantity: String = "1",
    val unitPrice: String = "0"
) {
    val quantityInt: Int get() = quantity.toIntOrNull() ?: 0
    val unitPriceDouble: Double get() = unitPrice.toDoubleOrNull() ?: 0.0
    val lineTotal: Double get() = quantityInt * unitPriceDouble
}

data class PlaceOrderUiState(
    val service: String = "",
    val customerId: String? = null,
    val subscription: Subscription? = null,
    val isSubscriptionOrder: Boolean = true,
    val orderNumber: String = "",
    val clothesCount: String = "",
    val items: List<OrderLineItem> = listOf(OrderLineItem()),
    val paymentMethod: String = "cash",
    val deliveryDate: LocalDate? = LocalDate.now(),
    val deliveryNotes: String = "",
    val discount: String = "",
    val overageRatePerItem: Double = 0.0,
    val isSaving: Boolean = false,
    val isCreatingSubscription: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val showLowQuotaWarning: Boolean = false,
    val lowQuotaRemaining: Int = 0,
    val isEditingSubscription: Boolean = false,
    val editClothesRemaining: String = "",
    val editDaysRemaining: String = ""
) {
    val clothesCountInt: Int get() = if (isSubscriptionOrder) (clothesCount.toIntOrNull() ?: 0) else items.sumOf { it.quantityInt }
    val totalItemsQuantity: Int get() = items.sumOf { it.quantityInt }
    val totalAmount: Double get() = items.sumOf { it.lineTotal }
    val discountDouble: Double get() = discount.toDoubleOrNull() ?: 0.0
    val finalAmount: Double get() = (totalAmount - discountDouble).coerceAtLeast(0.0)
    val remainingBefore: Int get() = subscription?.remainingClothes ?: 0
    val projectedRemaining: Int get() = remainingBefore - clothesCountInt
    val isOverQuota: Boolean get() = projectedRemaining < 0
    val overageAmount: Int get() = if (isOverQuota) -projectedRemaining else 0
    val overageCharge: Double get() = overageAmount * overageRatePerItem
}

data class OrderEditUiState(
    val order: Order? = null,
    val newClothesCount: String = "",
    val voidReason: String = "",
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val pendingSyncDao: PendingSyncDao,
    private val transactionDao: TransactionDao,
    private val orderDao: OrderDao,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerIdArg: String? = savedStateHandle.get<String>("customerId")
    private val subscriptionIdArg: String? = savedStateHandle.get<String>("subscriptionId")
    private val orderIdArg: String? = savedStateHandle.get<String>("orderId")

    private val _placeOrderState = MutableStateFlow(PlaceOrderUiState())
    val placeOrderState: StateFlow<PlaceOrderUiState> = _placeOrderState.asStateFlow()

    private val _editState = MutableStateFlow(OrderEditUiState())
    val editState: StateFlow<OrderEditUiState> = _editState.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val plans: StateFlow<List<SubscriptionPlan>> = _plans.asStateFlow()

    private val _customersQuery = MutableStateFlow("")
    val customers: StateFlow<List<Customer>> = _customersQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.getAllCustomers()
            } else {
                customerRepository.searchCustomers(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        orderIdArg?.let { loadOrderForEdit(it) }
        customerIdArg?.let { loadSubscriptionForOrder(it) }
        loadPlans()
        generateOrderNumber()
    }

    private fun generateOrderNumber() {
        viewModelScope.launch {
            try {
                val count = orderDao.getAllOrdersWithCustomers(UserSession.currentCompanyId ?: "").first().size
                val next = count + 1
                _placeOrderState.update { it.copy(orderNumber = next.toString().padStart(5, '0')) }
            } catch (_: Exception) {
                _placeOrderState.update { it.copy(orderNumber = "00001") }
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _plans.value = planRepository.getActivePlans().first()
        }
    }

    private fun loadSubscriptionForOrder(customerId: String) {
        viewModelScope.launch {
            val subscriptions = customerRepository.getSubscriptions(customerId)
            _subscriptions.value = subscriptions

            val target = subscriptionIdArg?.let { arg ->
                subscriptions.firstOrNull { it.id.toString() == arg }
            } ?: subscriptions.firstOrNull { s ->
                s.status == SubscriptionStatus.ACTIVE && !s.isExpired
            } ?: subscriptions.firstOrNull { s ->
                s.status == SubscriptionStatus.ACTIVE
            } ?: subscriptions.maxByOrNull { it.endDate }

            val hasActive = target != null && target.status == SubscriptionStatus.ACTIVE && !target.isExpired
            val rate = target?.let { orderRepository.perItemRateFor(it) } ?: 0.0
            _placeOrderState.update {
                it.copy(
                    customerId = customerId,
                    subscription = if (hasActive) target else null,
                    isSubscriptionOrder = hasActive,
                    overageRatePerItem = rate
                )
            }

            if (subscriptions.isEmpty()) {
                _plans.value = planRepository.getActivePlans().first()
            }
        }
    }

    fun createSubscriptionForCustomer(plan: SubscriptionPlan) {
        val customerId = _placeOrderState.value.customerId ?: return
        viewModelScope.launch {
            _placeOrderState.update { it.copy(isCreatingSubscription = true, error = null) }
            try {
                val subscription = subscriptionRepository.createSubscription(
                    customerId = UUID.fromString(customerId),
                    plan = plan,
                    paymentMethod = "cash"
                )
                _subscriptions.value = customerRepository.getSubscriptions(customerId)
                val rate = orderRepository.perItemRateFor(subscription)
                _placeOrderState.update {
                    it.copy(
                        isCreatingSubscription = false,
                        subscription = subscription,
                        overageRatePerItem = rate,
                        clothesCount = ""
                    )
                }
            } catch (e: Exception) {
                _placeOrderState.update { it.copy(isCreatingSubscription = false, error = e.message) }
            }
        }
    }

    fun onCustomerSearchQueryChange(query: String) {
        _customersQuery.value = query
    }

    fun selectOrderCustomer(customer: Customer) {
        viewModelScope.launch {
            _placeOrderState.update {
                it.copy(customerId = customer.id.toString(), subscription = null, error = null, success = false)
            }
            loadSubscriptionForOrder(customer.id.toString())
        }
    }

    fun selectSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val rate = orderRepository.perItemRateFor(subscription)
            _placeOrderState.update {
                it.copy(
                    subscription = subscription,
                    clothesCount = "",
                    overageRatePerItem = rate,
                    error = null,
                    showLowQuotaWarning = false
                )
            }
        }
    }

    fun onPaymentMethodChange(method: String) {
        _placeOrderState.update { it.copy(paymentMethod = method) }
    }

    fun onOrderNumberChange(value: String) {
        _placeOrderState.update { it.copy(orderNumber = value, error = null, success = false) }
    }

    fun onServiceChange(value: String) {
        _placeOrderState.update {
            it.copy(
                service = value,
                isSubscriptionOrder = value != "Dry Clean" && it.isSubscriptionOrder,
                error = null,
                success = false
            )
        }
    }

    fun onClothesCountChange(value: String) {
        val count = value.toIntOrNull() ?: 0
        val remaining = (_placeOrderState.value.subscription?.remainingClothes ?: 0) - count
        _placeOrderState.update {
            it.copy(
                clothesCount = value,
                error = null,
                showLowQuotaWarning = remaining in 1..5,
                lowQuotaRemaining = maxOf(remaining, 0)
            )
        }
    }

    fun onDeliveryDateChange(date: LocalDate) {
        _placeOrderState.update { it.copy(deliveryDate = date, error = null) }
    }

    fun onDeliveryNotesChange(value: String) {
        _placeOrderState.update { it.copy(deliveryNotes = value, error = null) }
    }

    fun onDiscountChange(value: String) {
        _placeOrderState.update { it.copy(discount = value, error = null) }
    }

    fun onOrderTypeChange(isSubscription: Boolean) {
        _placeOrderState.update {
            it.copy(
                isSubscriptionOrder = isSubscription,
                subscription = if (isSubscription) it.subscription else null,
                clothesCount = if (isSubscription) it.clothesCount else "",
                error = null,
                success = false
            )
        }
    }

    fun onItemChange(index: Int, item: OrderLineItem) {
        _placeOrderState.update { state ->
            val newList = state.items.toMutableList()
            if (index in newList.indices) newList[index] = item
            state.copy(items = newList, error = null, success = false)
        }
    }

    fun addItem() {
        val service = _placeOrderState.value.service
        val serviceForPrice = service.ifBlank { com.example.garmento.feature.services.ServicesStore.services.value.firstOrNull() ?: "" }
        val priceForShirt = com.example.garmento.feature.items.ItemStore.items.value
            .firstOrNull { it.name.trim().equals("Shirt", ignoreCase = true) && it.service.trim().equals(serviceForPrice.trim(), ignoreCase = true) }
            ?.price
            ?: com.example.garmento.feature.items.ItemStore.items.value
                .firstOrNull { it.name.trim().equals("Shirt", ignoreCase = true) }
                ?.price
            ?: "0"
        val newItem = OrderLineItem(unitPrice = priceForShirt)
        _placeOrderState.update { it.copy(items = it.items + newItem, success = false) }
    }

    fun removeItem(index: Int) {
        _placeOrderState.update { state ->
            if (state.items.size <= 1) return@update state
            state.copy(items = state.items.toMutableList().also { it.removeAt(index) })
        }
    }

    fun onToggleSubscriptionEdit() {
        val sub = _placeOrderState.value.subscription ?: return
        if (_placeOrderState.value.isEditingSubscription) {
            _placeOrderState.update { it.copy(isEditingSubscription = false, error = null) }
        } else {
            _placeOrderState.update {
                it.copy(
                    isEditingSubscription = true,
                    editClothesRemaining = sub.remainingClothes.toString(),
                    editDaysRemaining = sub.daysRemaining.toString(),
                    error = null
                )
            }
        }
    }

    fun onEditClothesRemainingChange(value: String) {
        _placeOrderState.update { it.copy(editClothesRemaining = value, error = null) }
    }

    fun onEditDaysRemainingChange(value: String) {
        _placeOrderState.update { it.copy(editDaysRemaining = value, error = null) }
    }

    fun saveSubscriptionEdits() {
        val state = _placeOrderState.value
        val sub = state.subscription ?: return
        val remaining = state.editClothesRemaining.toIntOrNull()
        val daysLeft = state.editDaysRemaining.toIntOrNull()

        if (remaining == null || remaining < 0) {
            _placeOrderState.update { it.copy(error = "Clothes remaining must be 0 or more") }
            return
        }
        if (daysLeft == null || daysLeft < 0) {
            _placeOrderState.update { it.copy(error = "Days left must be 0 or more") }
            return
        }

        viewModelScope.launch {
            try {
                val clothesRemaining = minOf(remaining, sub.clothesQuota)
                val updated = sub.copy(
                    clothesUsed = sub.clothesQuota - clothesRemaining,
                    endDate = LocalDate.now().plusDays(daysLeft.toLong())
                )
                subscriptionRepository.updateSubscriptionFields(updated)
                _subscriptions.value = _subscriptions.value.map { s ->
                    if (s.id == updated.id) updated else s
                }
                val rate = orderRepository.perItemRateFor(updated)
                _placeOrderState.update {
                    it.copy(
                        subscription = updated,
                        overageRatePerItem = rate,
                        isEditingSubscription = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _placeOrderState.update { it.copy(error = e.message) }
            }
        }
    }

    fun placeOrder(onSuccess: () -> Unit) {
        val state = _placeOrderState.value
        val sub = state.subscription
        val clothesCount = state.clothesCountInt
        val isSub = state.isSubscriptionOrder

        if (state.customerId == null) {
            _placeOrderState.update { it.copy(error = "Customer is required") }
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
        if (isSub && sub == null) {
            _placeOrderState.update { it.copy(error = "No active subscription found") }
            return
        }
        if (!isSub && state.items.any { it.quantityInt <= 0 }) {
            _placeOrderState.update { it.copy(error = "Each item quantity must be positive") }
            return
        }
        if (state.deliveryDate == null) {
            _placeOrderState.update { it.copy(error = "Delivery date is required") }
            return
        }

        if (!isSub) {
            viewModelScope.launch {
                _placeOrderState.update { it.copy(isSaving = true, error = null) }
                try {
                    val customerId = UUID.fromString(state.customerId!!)
                    val items = state.items.map { OrderItem(it.clothesType, it.quantityInt, it.unitPriceDouble) }
                    orderRepository.placeNonSubscriptionOrder(
                        customerId = customerId,
                        orderNumber = state.orderNumber,
                        items = items,
                        paymentMethod = state.paymentMethod,
                        deliveryDate = state.deliveryDate,
                        deliveryNotes = state.deliveryNotes.ifBlank { null }
                    )
                    _placeOrderState.update { it.copy(isSaving = false, success = true) }
                    onSuccess()
                } catch (e: Exception) {
                    _placeOrderState.update { it.copy(isSaving = false, error = e.message) }
                }
            }
            return
        }

        viewModelScope.launch {
            _placeOrderState.update { it.copy(isSaving = true, error = null) }
            try {
                if (!isNetworkAvailable()) {
                    val fromQuota = minOf(clothesCount, sub!!.remainingClothes)
                    val overage = maxOf(clothesCount - sub!!.remainingClothes, 0)
                    val orderPayload = buildJsonObject {
                        put("subscription_id", JsonPrimitive(sub!!.id.toString()))
                        put("order_number", JsonPrimitive(state.orderNumber))
                        put("clothes_count", JsonPrimitive(clothesCount))
                        put("clothes_from_quota", JsonPrimitive(fromQuota))
                        put("clothes_overage", JsonPrimitive(overage))
                        put("is_voided", JsonPrimitive(false))
                        put("grace_period", JsonPrimitive(false))
                        put("delivery_date", JsonPrimitive(state.deliveryDate.toString()))
                        put("delivery_status", JsonPrimitive("pending"))
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

                    orderDao.insertOrder(
                        com.example.garmento.core.data.local.entity.OrderEntity(
                            id = UUID.randomUUID().toString(),
                            subscription_id = sub!!.id.toString(),
                            order_number = state.orderNumber,
                            clothes_count = clothesCount,
                            clothes_from_quota = fromQuota,
                            clothes_overage = overage,
                            order_date = java.time.LocalDateTime.now().toString(),
                            is_voided = false,
                            void_reason = null,
                            grace_period = false,
                            delivery_date = state.deliveryDate.toString(),
                            delivery_status = "pending",
                            delivery_notes = null
                        )
                    )

                    subscriptionRepository.updateSubscriptionQuota(
                        sub!!.copy(clothesUsed = sub!!.clothesUsed + clothesCount)
                    )

                    if (overage > 0 && state.overageRatePerItem > 0) {
                        val overageCharge = overage * state.overageRatePerItem
                        val transaction = Transaction(
                            customerId = sub.customerId,
                            subscriptionId = sub!!.id,
                            amount = overageCharge,
                            type = TransactionType.OVERAGE,
                            paymentMethod = state.paymentMethod
                        )
                        transactionDao.insertTransaction(
                            TransactionEntity(
                                id = transaction.id.toString(),
                                customer_id = sub.customerId.toString(),
                                subscription_id = sub!!.id.toString(),
                                amount = overageCharge,
                                type = TransactionType.OVERAGE.value,
                                payment_method = state.paymentMethod,
                                transaction_date = transaction.transactionDate.toString()
                            )
                        )
                        val txPayload = buildJsonObject {
                            put("id", JsonPrimitive(transaction.id.toString()))
                            put("customer_id", JsonPrimitive(sub.customerId.toString()))
                            put("subscription_id", JsonPrimitive(sub!!.id.toString()))
                            put("amount", JsonPrimitive(overageCharge))
                            put("type", JsonPrimitive(TransactionType.OVERAGE.value))
                            put("payment_method", JsonPrimitive(state.paymentMethod))
                            put("transaction_date", JsonPrimitive(transaction.transactionDate.toString()))
                        }
                        pendingSyncDao.insertPendingItem(
                            PendingSyncEntity(
                                id = UUID.randomUUID().toString(),
                                table_name = "transactions",
                                payload_json = Json.encodeToString(txPayload),
                                created_at = java.time.LocalDateTime.now().toString(),
                                synced = false
                            )
                        )
                    }

                    _placeOrderState.update {
                        it.copy(
                            isSaving = false,
                            success = true,
                            error = "Saved offline — will sync when connected"
                        )
                    }
                    onSuccess()
                } else {
                    orderRepository.placeOrder(
                        subscription = sub!!,
                        orderNumber = state.orderNumber,
                        clothesCount = clothesCount,
                        paymentMethod = state.paymentMethod,
                        deliveryDate = state.deliveryDate,
                        deliveryNotes = state.deliveryNotes.ifBlank { null },
                        items = state.items.map { OrderItem(it.clothesType, it.quantityInt, it.unitPriceDouble) }
                    )
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
